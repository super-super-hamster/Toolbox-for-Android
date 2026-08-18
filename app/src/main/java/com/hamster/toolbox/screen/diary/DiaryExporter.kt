package com.hamster.toolbox.screen.diary

import java.io.File
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class DiaryExportFormat(
    val extension: String,
    val mimeType: String
) {
    TXT("txt", "text/plain"),
    EPUB("epub", "application/epub+zip"),
    MARKDOWN_ZIP("zip", "application/zip")
}

object DiaryExporter {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")
    private val modifiedFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun write(
        diaries: List<DiaryWithSegments>,
        format: DiaryExportFormat,
        output: OutputStream
    ) {
        val sortedDiaries = diaries.sortedBy { it.diary.date }
        when (format) {
            DiaryExportFormat.TXT -> writeTxt(sortedDiaries, output)
            DiaryExportFormat.EPUB -> writeEpub(sortedDiaries, output)
            DiaryExportFormat.MARKDOWN_ZIP -> writeMarkdownZip(sortedDiaries, output)
        }
    }

    fun createMarkdown(diaries: List<DiaryWithSegments>): MarkdownDocument {
        val images = mutableListOf<MarkdownImage>()
        val content = buildString {
            var currentYear: Int? = null
            var currentMonth: Int? = null

            diaries.sortedBy { it.diary.date }.forEachIndexed { index, diary ->
                val date = dateOf(diary.diary.date)
                if (currentYear != date.year) {
                    if (length > 0) append('\n')
                    append("# ").append(date.year).append("年\n\n")
                    currentYear = date.year
                    currentMonth = null
                }
                if (currentMonth != date.monthValue) {
                    append("## ").append(date.monthValue).append("月\n\n")
                    currentMonth = date.monthValue
                }

                append("### ").append(markdownHeading(diaryDayTitle(diary))).append("\n\n")
                diary.segments.sortedBy { it.position }.forEach { segment ->
                    when (segment.type) {
                        SegmentType.TEXT -> {
                            val text = segment.content.replace("\r\n", "\n").replace('\r', '\n')
                            if (text.isNotEmpty()) append(text).append("\n\n")
                        }

                        SegmentType.IMAGE -> {
                            val file = File(segment.content)
                            if (file.isFile) {
                                val extension = imageExtension(file)
                                val imageNumber = images.count { it.chapterIndex == index } + 1
                                val fileName = "chapter-${index + 1}-image-$imageNumber.$extension"
                                images += MarkdownImage(
                                    file = file,
                                    fileName = fileName,
                                    chapterIndex = index,
                                    mimeType = imageMediaType(extension)
                                )
                                append("![图片](日记.assets/").append(fileName).append(")\n\n")
                            } else {
                                append("[图片不可用]\n\n")
                            }
                        }
                    }
                }
            }
        }
        return MarkdownDocument(content = content, images = images)
    }

    private fun writeTxt(diaries: List<DiaryWithSegments>, output: OutputStream) {
        output.writer(StandardCharsets.UTF_8).use { writer ->
            diaries.forEachIndexed { index, diary ->
                val date = dateOf(diary.diary.date)
                writer.append(dateFormatter.format(date)).append('\n')
                diary.diary.title?.takeIf { it.isNotBlank() }?.let {
                    writer.append(it).append('\n')
                }
                writer.append('\n')

                diary.segments.sortedBy { it.position }.forEach { segment ->
                    when (segment.type) {
                        SegmentType.TEXT -> {
                            writer.append(segment.content.replace("\r\n", "\n").replace('\r', '\n'))
                            writer.append('\n')
                        }

                        SegmentType.IMAGE -> writer.append("[图片]\n")
                    }
                }

                if (index < diaries.lastIndex) {
                    writer.append('\n').append("----------------------------------------").append("\n\n")
                }
            }
        }
    }

    private fun writeEpub(diaries: List<DiaryWithSegments>, output: OutputStream) {
        val bookId = "urn:uuid:${UUID.randomUUID()}"
        val chapters = diaries.mapIndexed { index, diary ->
            createChapter(index, diary)
        }

        ZipOutputStream(output).use { zip ->
            writeStoredEntry(zip, "mimetype", "application/epub+zip")
            writeEntry(zip, "META-INF/container.xml", containerXml())
            writeEntry(zip, "OEBPS/content.opf", contentOpf(bookId, chapters))
            writeEntry(zip, "OEBPS/nav.xhtml", navXhtml(chapters))
            writeEntry(zip, "OEBPS/toc.ncx", tocNcx(bookId, chapters))

            chapters.forEach { chapter ->
                writeEntry(zip, chapter.href, chapter.xhtml)
            }

            chapters.flatMap { it.images }.forEach { image ->
                val entry = ZipEntry(image.href)
                zip.putNextEntry(entry)
                image.file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    private fun writeMarkdownZip(diaries: List<DiaryWithSegments>, output: OutputStream) {
        val document = createMarkdown(diaries)
        ZipOutputStream(output).use { zip ->
            writeEntry(zip, "日记.md", document.content)
            document.images.forEach { image ->
                zip.putNextEntry(ZipEntry("日记.assets/${image.fileName}"))
                image.file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    private fun createChapter(index: Int, diary: DiaryWithSegments): Chapter {
        val chapterId = "chapter-${index + 1}"
        val chapterHref = "OEBPS/text/$chapterId.xhtml"
        val images = mutableListOf<ImageResource>()
        val body = buildString {
            append("<h1>")
            append(escapeXml(diaryTitle(diary)))
            append("</h1>")
            append("<p class=\"date\">")
            append(escapeXml(dateFormatter.format(dateOf(diary.diary.date))))
            append("</p>")

            diary.segments.sortedBy { it.position }.forEach { segment ->
                when (segment.type) {
                    SegmentType.TEXT -> {
                        val content = segment.content.replace("\r\n", "\n").replace('\r', '\n')
                        if (content.isNotEmpty()) {
                            append("<p>")
                            append(escapeXml(content).replace("\n", "<br />"))
                            append("</p>")
                        }
                    }

                    SegmentType.IMAGE -> {
                        val file = File(segment.content)
                        if (file.isFile) {
                            val extension = imageExtension(file)
                            val imageName = "$chapterId-image-${images.size + 1}.$extension"
                            val imageHref = "OEBPS/images/$imageName"
                            images += ImageResource(
                                file = file,
                                href = imageHref,
                                mediaType = imageMediaType(extension)
                            )
                            append("<img src=\"../images/$imageName\" alt=\"图片\" />")
                        } else {
                            append("<p>[图片不可用]</p>")
                        }
                    }
                }
            }
        }

        return Chapter(
            id = chapterId,
            href = chapterHref,
            label = diaryTitle(diary),
            xhtml = chapterXhtml(diaryTitle(diary), body),
            images = images
        )
    }

    private fun contentOpf(bookId: String, chapters: List<Chapter>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        append("<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\" unique-identifier=\"book-id\">")
        append("<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\">")
        append("<dc:identifier id=\"book-id\">").append(escapeXml(bookId)).append("</dc:identifier>")
        append("<dc:title>日记</dc:title><dc:language>zh-CN</dc:language>")
        append("<meta property=\"dcterms:modified\">")
        append(escapeXml(modifiedFormatter.format(java.time.OffsetDateTime.now()))).append("</meta>")
        append("</metadata><manifest>")
        append("<item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>")
        append("<item id=\"ncx\" href=\"toc.ncx\" media-type=\"application/x-dtbncx+xml\"/>")
        chapters.forEach { chapter ->
            append("<item id=\"").append(chapter.id).append("\" href=\"")
                .append(chapter.href.removePrefix("OEBPS/")).append("\" media-type=\"application/xhtml+xml\"/>")
            chapter.images.forEachIndexed { index, image ->
                append("<item id=\"").append(chapter.id).append("-image-").append(index + 1)
                    .append("\" href=\"").append(image.href.removePrefix("OEBPS/"))
                    .append("\" media-type=\"").append(image.mediaType).append("\"/>")
            }
        }
        append("</manifest><spine toc=\"ncx\">")
        chapters.forEach { append("<itemref idref=\"").append(it.id).append("\"/>") }
        append("</spine></package>")
    }

    private fun navXhtml(chapters: List<Chapter>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:epub=\"http://www.idpf.org/2007/ops\">")
        append("<head><title>目录</title></head><body><nav epub:type=\"toc\" id=\"toc\">")
        append("<h1>目录</h1><ol>")
        chapters.forEach { chapter ->
            append("<li><a href=\"").append(chapter.href.removePrefix("OEBPS/")).append("\">")
                .append(escapeXml(chapter.label)).append("</a></li>")
        }
        append("</ol></nav></body></html>")
    }

    private fun tocNcx(bookId: String, chapters: List<Chapter>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        append("<ncx xmlns=\"http://www.daisy.org/z3986/2005/ncx/\" version=\"2005-1\">")
        append("<head><meta name=\"dtb:uid\" content=\"").append(escapeXml(bookId)).append("\"/></head>")
        append("<docTitle><text>日记</text></docTitle><navMap>")
        chapters.forEachIndexed { index, chapter ->
            append("<navPoint id=\"").append(chapter.id).append("-toc\" playOrder=\"").append(index + 1).append("\">")
                .append("<navLabel><text>").append(escapeXml(chapter.label)).append("</text></navLabel><content src=\"")
                .append(chapter.href.removePrefix("OEBPS/")).append("\"/></navPoint>")
        }
        append("</navMap></ncx>")
    }

    private fun chapterXhtml(title: String, body: String): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>")
            .append(escapeXml(title)).append("</title></head><body>").append(body).append("</body></html>")
    }

    private fun containerXml() = """
        <?xml version="1.0" encoding="UTF-8"?>
        <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
          <rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
        </container>
    """.trimIndent()

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    private fun writeStoredEntry(zip: ZipOutputStream, name: String, content: String) {
        val bytes = content.toByteArray(StandardCharsets.US_ASCII)
        val checksum = CRC32().apply { update(bytes) }
        zip.putNextEntry(ZipEntry(name).apply {
            method = ZipEntry.STORED
            size = bytes.size.toLong()
            this.crc = checksum.value
        })
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun dateOf(timestamp: Long) = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun diaryTitle(diary: DiaryWithSegments): String {
        val date = dateFormatter.format(dateOf(diary.diary.date))
        return diary.diary.title?.takeIf { it.isNotBlank() }?.let { "$date-$it" } ?: date
    }

    private fun diaryDayTitle(diary: DiaryWithSegments): String {
        val date = dateOf(diary.diary.date)
        val day = date.dayOfMonth
        return diary.diary.title?.takeIf { it.isNotBlank() }?.let { title ->
            day.toString() + "日-" + title
        } ?: day.toString() + "日"
    }

    private fun markdownHeading(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\n", " ")
        .replace("\r", " ")
        .replace("#", "\\#")

    private fun imageExtension(file: File): String = when (file.extension.lowercase()) {
        "jpeg" -> "jpg"
        "jpg", "png", "webp" -> file.extension.lowercase()
        else -> "jpg"
    }

    private fun imageMediaType(extension: String): String = when (extension) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }

    private fun escapeXml(value: String): String = buildString(value.length) {
        value.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(char)
            }
        }
    }

    private data class Chapter(
        val id: String,
        val href: String,
        val label: String,
        val xhtml: String,
        val images: List<ImageResource>
    )

    private data class ImageResource(
        val file: File,
        val href: String,
        val mediaType: String
    )

    data class MarkdownDocument(
        val content: String,
        val images: List<MarkdownImage>
    )

    data class MarkdownImage(
        val file: File,
        val fileName: String,
        val chapterIndex: Int,
        val mimeType: String
    )
}
