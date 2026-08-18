package com.hamster.toolbox.screen.diary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.LocalDate
import java.time.ZoneId
import java.util.zip.ZipFile

class DiaryExporterTest {
    private val testDate = LocalDate.of(2024, 1, 2)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    @Test
    fun txtContainsOrderedContentAndImagePlaceholder() {
        val diary = diary(
            title = "第一篇",
            segments = listOf(
                DiarySegmentEntity(1, 1, SegmentType.IMAGE, "missing.jpg", 1),
                DiarySegmentEntity(2, 1, SegmentType.TEXT, "第一段\r\n第二行", 0)
            )
        )
        val output = ByteArrayOutputStream()

        DiaryExporter.write(listOf(diary), DiaryExportFormat.TXT, output)

        val text = output.toString(StandardCharsets.UTF_8.name())
        assertTrue(text.startsWith("2024年1月2日\n第一篇\n"))
        assertTrue(text.contains("第一段\n第二行\n[图片]"))
    }

    @Test
    fun epubContainsNavigationPackageAndEmbeddedImage() {
        val image = Files.createTempFile("diary-export-test", ".jpg").toFile()
        image.writeBytes(byteArrayOf(1, 2, 3))
        try {
            val diary = diary(
                title = "<标题 & 测试>",
                segments = listOf(
                    DiarySegmentEntity(3, 1, SegmentType.TEXT, "正文 <内容>", 0),
                    DiarySegmentEntity(4, 1, SegmentType.IMAGE, image.absolutePath, 1)
                )
            )
            val output = ByteArrayOutputStream()

            DiaryExporter.write(listOf(diary), DiaryExportFormat.EPUB, output)

            val epub = Files.createTempFile("diary-export", ".epub").toFile()
            epub.writeBytes(output.toByteArray())
            try {
                ZipFile(epub).use { zip ->
                    val entries = zip.entries().asSequence().toList()
                    assertEquals("mimetype", entries.first().name)
                    assertEquals(0, entries.first().method)
                    assertEquals("application/epub+zip", zip.getInputStream(entries.first()).reader().readText())
                    assertTrue(entries.any { it.name == "META-INF/container.xml" })
                    assertTrue(entries.any { it.name == "OEBPS/content.opf" })
                    assertTrue(entries.any { it.name == "OEBPS/nav.xhtml" })
                    assertTrue(entries.any { it.name == "OEBPS/toc.ncx" })
                    assertTrue(entries.any { it.name == "OEBPS/images/chapter-1-image-1.jpg" })

                    val nav = zip.getInputStream(zip.getEntry("OEBPS/nav.xhtml")).reader(StandardCharsets.UTF_8).readText()
                    assertTrue(nav.contains("2024年"))
                    assertTrue(nav.contains("1月"))
                    assertTrue(nav.contains("2024年1月2日-&lt;标题 &amp; 测试&gt;"))
                    assertTrue(nav.contains("href=\"text/chapter-1.xhtml\""))
                    assertTrue(!nav.contains("year-2024"))

                    val ncx = zip.getInputStream(zip.getEntry("OEBPS/toc.ncx")).reader(StandardCharsets.UTF_8).readText()
                    assertTrue(ncx.contains("content src=\"text/chapter-1.xhtml\""))
                    assertTrue(!ncx.contains("nav.xhtml#"))

                    val chapter = zip.getInputStream(zip.getEntry("OEBPS/text/chapter-1.xhtml"))
                        .reader(StandardCharsets.UTF_8).readText()
                    assertTrue(chapter.contains("<h1>2024年1月2日-&lt;标题 &amp; 测试&gt;</h1>"))
                    assertTrue(chapter.contains("正文 &lt;内容&gt;"))
                    assertTrue(chapter.contains("../images/chapter-1-image-1.jpg"))
                }
            } finally {
                epub.delete()
            }
        } finally {
            image.delete()
        }
    }

    @Test
    fun markdownUsesYearMonthDayHeadingsAndRelativeImages() {
        val image = Files.createTempFile("diary-markdown-test", ".jpg").toFile()
        image.writeBytes(byteArrayOf(1, 2, 3))
        try {
            val diary = diary(
                title = "标题",
                segments = listOf(
                    DiarySegmentEntity(5, 1, SegmentType.TEXT, "正文", 0),
                    DiarySegmentEntity(6, 1, SegmentType.IMAGE, image.absolutePath, 1)
                )
            )

            val markdown = DiaryExporter.createMarkdown(listOf(diary))

            assertTrue(markdown.content.contains("# 2024年"))
            assertTrue(markdown.content.contains("## 1月"))
            assertTrue(markdown.content.contains("### 2日-标题"))
            assertTrue(markdown.content.contains("正文"))
            assertTrue(markdown.content.contains("![图片](日记.assets/chapter-1-image-1.jpg)"))
            assertEquals(1, markdown.images.size)
            assertEquals("chapter-1-image-1.jpg", markdown.images.single().fileName)

            val untitled = diary(
                title = null,
                date = LocalDate.of(2024, 1, 3)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli(),
                segments = emptyList()
            )
            assertTrue(DiaryExporter.createMarkdown(listOf(untitled)).content.contains("### 3日\n"))
        } finally {
            image.delete()
        }
    }

    @Test
    fun markdownZipContainsMarkdownAndAssets() {
        val image = Files.createTempFile("diary-markdown-zip-test", ".jpg").toFile()
        image.writeBytes(byteArrayOf(4, 5, 6))
        try {
            val diary = diary(
                title = "标题",
                segments = listOf(DiarySegmentEntity(7, 1, SegmentType.IMAGE, image.absolutePath, 0))
            )
            val output = ByteArrayOutputStream()

            DiaryExporter.write(listOf(diary), DiaryExportFormat.MARKDOWN_ZIP, output)

            val archive = Files.createTempFile("diary-markdown", ".zip").toFile()
            archive.writeBytes(output.toByteArray())
            try {
                ZipFile(archive).use { zip ->
                    assertTrue(zip.getEntry("日记.md") != null)
                    assertTrue(zip.getEntry("日记.assets/chapter-1-image-1.jpg") != null)
                    val markdown = zip.getInputStream(zip.getEntry("日记.md"))
                        .reader(StandardCharsets.UTF_8).readText()
                    assertTrue(markdown.contains("### 2日-标题"))
                    assertTrue(markdown.contains("日记.assets/chapter-1-image-1.jpg"))
                }
            } finally {
                archive.delete()
            }
        } finally {
            image.delete()
        }
    }

    private fun diary(
        title: String?,
        segments: List<DiarySegmentEntity>,
        date: Long = testDate
    ) = DiaryWithSegments(
        diary = DiaryEntity(id = 1, title = title, date = date, wordCount = 0),
        segments = segments
    )
}
