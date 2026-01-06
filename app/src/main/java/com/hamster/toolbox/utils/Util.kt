package com.hamster.toolbox.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.example.toolbox.R
import com.example.toolbox.ui.curriculum.Course
import com.hamster.toolbox.utils.prompt.PromptLoader
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.ZoneId
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

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

//通过现实时间获取时间戳
//@RequiresApi(Build.VERSION_CODES.O)注解标记的代码只能在Android 8+ 的系统运行
@RequiresApi(Build.VERSION_CODES.O)
fun timeToMillis(time: LocalDateTime, hour: Int? = null, minute: Int? = null, second: Int? = null, nano: Int? = null) : Long {
    hour?.let { time.withHour(it) }
    minute?.let { time.withMinute(it) }
    second?.let { time.withSecond(it) }
    nano?.let { time.withNano(it) }

    return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun Fragment.runWithPermission(
    permission: String,
    launcher: ActivityResultLauncher<String>,
    action: () -> Unit
) {
    if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
        action()
    } else {
        launcher.launch(permission)
    }
}

fun AppCompatActivity.runWithPermission(
    permission: String,
    launcher: ActivityResultLauncher<String>,
    action: () -> Unit
) {
    if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
        action()
    } else {
        launcher.launch(permission)
    }
}

// 获取课程表
fun getCurriculum(context: Context): List<Course> {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val jsonString = prefs.getString("curriculum_json", null)

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

// 将毫秒数转为"yyyy-MM-dd"
fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(Date(millis))
}

// 将"yyyy-MM-dd"转为毫秒数
@RequiresApi(Build.VERSION_CODES.O)
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

//复制课程表提示词
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
        //转化成数组时Gson会类型擦除，必须使用
        //json格式错误时会抛出异常
        val courseListType = object : TypeToken<List<Course>>() {}.type
        val gson = Gson()
        val courses: List<Course> = gson.fromJson(jsonString, courseListType)

        val validJsonString = gson.toJson(courses)

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs?.edit { putString("curriculum_json", validJsonString) }

//            (requireActivity() as? MainActivity)?.showAssistantBubble("课程表导入成功！")

        return true

    } catch (e: Exception) {
        e.printStackTrace()
//            (requireActivity() as? MainActivity)?.showAssistantBubble("导入失败")
        return false
    }
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