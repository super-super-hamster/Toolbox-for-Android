package com.hamster.toolbox.screen.settings

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.hamster.toolbox.ImportCurriculum
import com.hamster.toolbox.R
import com.hamster.toolbox.Route
import com.hamster.toolbox.SetKeywords
import com.hamster.toolbox.system.Receiver
import com.hamster.toolbox.utils.ClickItem
import com.hamster.toolbox.utils.DatePicker
import com.hamster.toolbox.utils.EditTextItem
import com.hamster.toolbox.utils.InquiryDialog
import com.hamster.toolbox.utils.ItemGroup
import com.hamster.toolbox.utils.ScrollTarget
import com.hamster.toolbox.utils.SwitchItem
import com.hamster.toolbox.utils.convertDateToMillis
import com.hamster.toolbox.utils.rememberBooleanPreference
import com.hamster.toolbox.utils.rememberSharedTiltState
import com.hamster.toolbox.utils.rememberStringPreference
import com.hamster.toolbox.utils.tiltGestureContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneOffset

// TODO:高光

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    triggerTime: Long? = null,
    jumpTargetId: String? = null,
    onNavigate: (Route) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    val sharedTiltState = rememberSharedTiltState()
    val targets = remember { mutableMapOf<String, ScrollTarget>() }
    var currentAvatarType by remember { mutableStateOf("user") }

    val avatarPickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val path = SettingsUtils.saveImageToInternalStorage(context, uri, currentAvatarType)
                if (path != null) {
                    prefs.edit { putString(currentAvatarType + "_avatar_path", path) }
                    Toast.makeText(context, "头像已更新", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "更新头像失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun getModifier(id: String): Modifier {
        return targets.getOrPut(id) { ScrollTarget() }.modifier
    }

    fun scrollTo(id: String?) {
        if (!id.isNullOrEmpty()) {
            scope.launch {
                delay(400)
                targets[id]?.scrollTo(context)
            }
        }
    }

    LaunchedEffect(triggerTime) {
        scrollTo(jumpTargetId)
    }

    var nickname by rememberStringPreference("nickname", stringResource(R.string.anonymous))
    var signature by rememberStringPreference("signature", stringResource(R.string.user_signature))
    var assistantNickname by rememberStringPreference("assistant_nickname", stringResource(R.string.assistant))
    var semesterStartDate by rememberStringPreference("semester_start_date", "未设置")
    var curriculumImportState by rememberStringPreference("curriculum_json", "未设置")
    var apiKey by rememberStringPreference("api_key", "")
    var isClassRemindEnabled by rememberBooleanPreference("class_notification")
    var isAlarmRemindEnabled by rememberBooleanPreference("alarm_notification")

    var showUserAvatarOptionsDialog by remember { mutableStateOf(false) }
    var showAssistantAvatarOptionsDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    if (curriculumImportState != "未设置") {
        curriculumImportState = "已设置"
    }

    val classRemindRequestPostNotificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isClassRemindEnabled = true
        } else {
            isClassRemindEnabled = false
            Toast.makeText(context, "需要通知权限", Toast.LENGTH_SHORT).show()
        }
    }

    val alarmRemindRequestPostNotificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isAlarmRemindEnabled = true
        } else {
            isAlarmRemindEnabled = false
            Toast.makeText(context, "需要通知权限", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize().tiltGestureContainer(sharedTiltState)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
//            .tiltGestureContainer(sharedTiltState)
                .background(colorResource(id = R.color.background))
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.top_padding)))

            ItemGroup(titleState = sharedTiltState) {
                ClickItem(title = "用户头像", icon = R.drawable.ic_user_avatar) {
                    showUserAvatarOptionsDialog = true
                    currentAvatarType = "user"
                }
                EditTextItem(
                    modifier = getModifier("nickname"),
                    title = "昵称",
                    dialogTitle = "修改昵称",
                    initialValue = nickname,
                    hint = "昵称",
                    singleLine = true,
                    icon = R.drawable.ic_user_name,
                    onCancel = { nickname = prefs.getString("nickname", "") ?: "" },
                    onConfirm = { input ->
                        nickname = input
                        prefs.edit { putString("nickname", nickname) }
                        true
                    }
                )
                EditTextItem(
                    modifier = getModifier("signature"),
                    title = "个性签名",
                    dialogTitle = "修改签名",
                    initialValue = signature,
                    hint = "个性签名",
                    singleLine = true,
                    icon = R.drawable.ic_user_signature,
                    onCancel = { signature = prefs.getString("signature", "") ?: "" },
                    onConfirm = { input ->
                        signature = input
                        prefs.edit { putString("signature", signature) }
                        true
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ItemGroup(titleState = sharedTiltState) {
                ClickItem(
                    modifier = getModifier("semester_start_date"),
                    title = "学期开始日期",
                    summary = semesterStartDate,
                    icon = R.drawable.ic_calendar
                ) {
                    showDatePickerDialog = true
                }
                ClickItem(
                    modifier = getModifier("import_curriculum_options"),
                    title = "导入课程表",
                    summary = curriculumImportState,
                    icon = R.drawable.ic_curriculum
                ) {
                    if (prefs.getString("semester_start_date", null)?.isEmpty() == true) {
                        scrollTo("semester_start_date")
                        return@ClickItem
                    }
                    onNavigate(ImportCurriculum)
                }
                SwitchItem(
                    modifier = getModifier("class_notification"),
                    title = "上课提醒",
                    summary = "开启后将在上课前发送通知",
                    checked = isClassRemindEnabled,
                    icon = R.drawable.ic_message,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val permissionStatus = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                if (permissionStatus != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    classRemindRequestPostNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    return@SwitchItem
                                }
                            }
                        }

                        isClassRemindEnabled = isChecked
                        prefs.edit { putBoolean("class_notification", isChecked) }

                        Receiver.dailyNotification(
                            context, 22, 0, Receiver.ACTION_CLASS_ALARM_CHECK, 101, isClassRemindEnabled || isAlarmRemindEnabled, emptyArray()
                        )
                    }
                )

                SwitchItem(
                    modifier = getModifier("alarm_notification"),
                    title = "上课闹钟设置提醒",
                    summary = "开启后将自动发送设置闹钟的通知",
                    checked = isAlarmRemindEnabled,
                    icon = R.drawable.ic_alarm,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val permissionStatus = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                if (permissionStatus != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    alarmRemindRequestPostNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    return@SwitchItem
                                }
                            }
                        }

                        isAlarmRemindEnabled = isChecked
                        prefs.edit { putBoolean("alarm_notification", isChecked) }

                        Receiver.dailyNotification(
                            context, 22, 0, Receiver.ACTION_CLASS_ALARM_CHECK, 101, isClassRemindEnabled || isAlarmRemindEnabled, emptyArray()
                        )
                    }
                )

            }

            Spacer(modifier = Modifier.height(8.dp))

            ItemGroup(titleState = sharedTiltState) {
                ClickItem(
                    modifier = getModifier("assistant_avatar"),
                    title = "助手头像",
                    icon = R.drawable.ic_assistant
                ) {
                    showAssistantAvatarOptionsDialog = true
                    currentAvatarType = "assistant"
                }
                EditTextItem(
                    modifier = getModifier("assistant_nickname"),
                    title = "助手昵称",
                    dialogTitle = "修改助手昵称",
                    initialValue = assistantNickname,
                    hint = "助手昵称",
                    singleLine = true,
                    icon = R.drawable.ic_user_name,
                    onCancel = { assistantNickname = prefs.getString("assistant_nickname", "") ?: "" },
                    onConfirm = { input ->
                        assistantNickname = input
                        prefs.edit { putString("assistant_nickname", assistantNickname) }
                        true
                    }
                )
                ClickItem(
                    modifier = getModifier("keywords"),
                    title = "热词",
                    summary = "热词更容易被语音识别",
                    icon = R.drawable.ic_characters
                ) {
                    onNavigate(SetKeywords)
                }
                EditTextItem(
                    modifier = getModifier("api_key"),
                    title = "API",
                    summary = if (apiKey.isEmpty()) "未设置" else "******",
                    dialogTitle = "输入 API Key",
                    initialValue = apiKey,
                    hint = "API KEY",
                    singleLine = true,
                    icon = R.drawable.ic_a,
                    onCancel = { apiKey = prefs.getString("api_key", "") ?: "" },
                    onConfirm = { input ->
                        apiKey = input
                        prefs.edit { putString("api_key", apiKey) }
                        true
                    }
                )
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_padding)))
        }
    }

    if (showUserAvatarOptionsDialog || showAssistantAvatarOptionsDialog) {
        InquiryDialog(
            title = "修改头像",
            content = "",
            cancelText = "恢复默认",
            confirmText = "相册中选择",
            onCancel = {
                try {
                    val file = File(context.filesDir, "$currentAvatarType.png")
                    if (file.exists()) {
                        file.delete()
                    }
                    prefs.edit { remove(currentAvatarType + "_avatar_path") }
                    Toast.makeText(context, "已恢复默认头像", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                } },
            onDismissRequest = {
                showUserAvatarOptionsDialog = false
                showAssistantAvatarOptionsDialog = false },
            onConfirm = {
                avatarPickMedia.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
                true
            }
        )
    }
    if (showDatePickerDialog) {
        DatePicker(
            title = "选择开学日期",
            initialSelectedDateMillis = convertDateToMillis(
                if (semesterStartDate == "未设置") null else semesterStartDate
            ),
            onDateSelected = { millis ->
                if (millis != null) {
                    val instant = Instant.ofEpochMilli(millis)
                    val selectedDateObj = instant.atZone(ZoneOffset.UTC).toLocalDate()
                    val dayOfWeek = selectedDateObj.dayOfWeek.value
                    val alignedDate = selectedDateObj.minusDays((dayOfWeek - 1).toLong())
                    val newDateString = alignedDate.toString()
                    semesterStartDate = newDateString
                    prefs.edit { putString("semester_start_date", newDateString) }
                } },
            onDismiss = {
                showDatePickerDialog = false
            }
        )
    }
}

// TODO:改
object SettingsUtils {
    fun cropBitmapToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val newWidth = if (height > width) width else height
        val newHeight = if (height > width) height - (height - width) / 2 else height
        var cropW = (width - height) / 2
        cropW = if (cropW < 0) 0 else cropW
        var cropH = (height - width) / 2
        cropH = if (cropH < 0) 0 else cropH
        return Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newWidth)
    }

    fun resizeBitmap(bitmap: Bitmap, newWidth: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newWidth.toFloat() / height
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    // 挂起函数：保存图片 (移除了 UI 线程阻塞)
    suspend fun saveImageToInternalStorage(context: Context, uri: Uri, type: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                val squareBitmap = cropBitmapToSquare(bitmap)
                val resizedBitmap = resizeBitmap(squareBitmap, 256) // 默认 256

                val file = File(context.filesDir, "$type.png")
                val fos = FileOutputStream(file)
                resizedBitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
                fos.close()
                file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}