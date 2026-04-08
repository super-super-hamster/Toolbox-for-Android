package com.hamster.toolbox.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.animation.Animatable
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.palette.graphics.Palette
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hamster.toolbox.R
import com.hamster.toolbox.screen.schedule.Course
import com.hamster.toolbox.utils.prompt.PromptLoader
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

class ScrollTarget {
    val requester = BringIntoViewRequester()
    val colorAnimatable = Animatable(Color.Transparent)

    val modifier = Modifier
        .bringIntoViewRequester(requester)
        .drawWithContent {
            drawContent()
            drawRect(color = colorAnimatable.value)
        }

    suspend fun scrollTo(context: Context) {
        try {
            requester.bringIntoView()

            delay(100)

            val highlightColor = Color(ColorUtils.setAlphaComponent(ContextCompat.getColor(context, R.color.mikuGreen), 64))
            repeat(2) {
                colorAnimatable.animateTo(highlightColor, animationSpec = tween(300))
                colorAnimatable.animateTo(Color.Transparent, animationSpec = tween(300))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun getMainColor(bitmap: Bitmap): Color {
    return try {
        val pixels = IntArray(bitmap.width * bitmap.height)


        val colorInt = 0
        Color(colorInt)

    } catch (_: Exception) {
        Color.LightGray
    }
}

fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    }

    // 处理 AdaptiveIconDrawable (Android 8.0+) 或 VectorDrawable
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 100
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 100

    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
}

//通过现实时间获取时间戳
//@RequiresApi(Build.VERSION_CODES.O)注解标记的代码只能在Android 8+ 的系统运行
fun timeToMillis(time: LocalDateTime, hour: Int? = null, minute: Int? = null, second: Int? = null, nano: Int? = null) : Long {
    hour?.let { time.withHour(it) }
    minute?.let { time.withMinute(it) }
    second?.let { time.withSecond(it) }
    nano?.let { time.withNano(it) }

    return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

// 获取课程表
fun getSchedule(context: Context): List<Course> {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val jsonString = prefs.getString("schedule_json", null)

    if (jsonString == null) {
        Toast.makeText(context, "未导入课程表", Toast.LENGTH_LONG).show()
        return emptyList()
    }

    try {
        val courseListType = object : TypeToken<List<Course>>() {}.type
        val gson = Gson()
        return gson.fromJson(jsonString, courseListType)
    } catch (e: Exception) {
        e.printStackTrace()
        return emptyList()
    }
}

fun saveSchedule(context: Context, newCourseList: List<Course>) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val gson = Gson()
    val jsonString = gson.toJson(newCourseList)

    prefs.edit { putString("schedule_json", jsonString) }
}

// 将毫秒数转为"yyyy-MM-dd"
fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(Date(millis))
}

// 将"yyyy-MM-dd"转为毫秒数
fun convertDateToMillis(dateStr: String?): Long {
    if (dateStr.isNullOrEmpty()) return System.currentTimeMillis()
    return try {
        LocalDate.parse(dateStr)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

// 复制课程表提示词
fun copyCurriculumJSONPrompt(context: Context) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText("JSON Template", PromptLoader.getPromptById(context, "import_curriculum"))
    clipboardManager.setPrimaryClip(clipData)

    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
}

//判断json是否合法并保存
fun validateAndSaveJson(jsonString: String?, context: Context): Boolean {
    if (jsonString.isNullOrBlank()) {
        Toast.makeText(context, "输入内容无效", Toast.LENGTH_SHORT).show()
        return false
    }

    try {
        // 转化成数组时Gson会类型擦除，必须使用
        // json格式错误时会抛出异常
        val courseListType = object : TypeToken<List<Course>>() {}.type
        val gson = Gson()
        val courses: List<Course> = gson.fromJson(jsonString, courseListType)

        val validJsonString = gson.toJson(courses)

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs?.edit { putString("schedule_json", validJsonString) }

//            (requireActivity() as? MainActivity)?.showAssistantBubble("课程表导入成功！")
        Toast.makeText(context, "成功", Toast.LENGTH_SHORT).show()

        return true

    } catch (e: Exception) {
        e.printStackTrace()
//            (requireActivity() as? MainActivity)?.showAssistantBubble("导入失败")
        Toast.makeText(context, "失败", Toast.LENGTH_SHORT).show()
        return false
    }
}

// 进场动画
fun slideInWithScaleEnter(): EnterTransition {
    val duration = 200
    val delay = 75
    val offsetSpec = tween<IntOffset>(durationMillis = duration, delayMillis = delay)
    val floatSpec = tween<Float>(durationMillis = duration, delayMillis = delay)

    return slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = offsetSpec
    ) + scaleIn(
        initialScale = 0.9f,
        animationSpec = floatSpec
    )
}

// 出场动画
fun scaleOutExit(): ExitTransition {
    val duration = 200
    val floatSpec = tween<Float>(durationMillis = duration, delayMillis = 0)

    return scaleOut(
        targetScale = 0.85f,
        animationSpec = floatSpec
    )
}

/**
 * 返回-进场动画：旧页面从背景放大回来
 */
fun scaleInPopEnter(): EnterTransition {
    val duration = 200
    val floatSpec = tween<Float>(durationMillis = duration)

    return scaleIn(
        initialScale = 0.85f,
        animationSpec = floatSpec
    )
}

/**
 * 返回-出场动画：当前页面向右滑出 + 缩小
 */
fun slideOutWithScalePopExit(): ExitTransition {
    val duration = 200
    val offsetSpec = tween<IntOffset>(durationMillis = duration)
    val floatSpec = tween<Float>(durationMillis = duration)

    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = offsetSpec
    ) + scaleOut(
        targetScale = 0.9f,
        animationSpec = floatSpec
    )
}

fun cropBitmapToSquare(bitmap: Bitmap): Bitmap {
    val size = minOf(bitmap.width, bitmap.height)
    val x = (bitmap.width - size) / 2
    val y = (bitmap.height - size) / 2
    return Bitmap.createBitmap(bitmap, x, y, size, size)
}

fun resizeBitmap(bitmap: Bitmap, size: Int): Bitmap {
    if (bitmap.width == size && bitmap.height == size) return bitmap
    val matrix = Matrix()
    matrix.postScale(size.toFloat() / bitmap.width, size.toFloat() / bitmap.height)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}