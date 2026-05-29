package com.hamster.toolbox.screen.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.dimensionResource
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.PreferenceManager
import com.hamster.toolbox.AssistantSettings
import com.hamster.toolbox.ImportCurriculum
import com.hamster.toolbox.R
import com.hamster.toolbox.Route
import com.hamster.toolbox.ai.AI
import com.hamster.toolbox.ai.tools.ToolScope
import com.hamster.toolbox.compose.ClickItem
import com.hamster.toolbox.compose.DatePicker
import com.hamster.toolbox.compose.EditTextItem
import com.hamster.toolbox.compose.InquiryDialog
import com.hamster.toolbox.compose.ItemGroup
import com.hamster.toolbox.compose.SwitchItem
import com.hamster.toolbox.compose.VerticalScrollPageColumn
import com.hamster.toolbox.compose.rememberSharedTiltState
import com.hamster.toolbox.compose.rememberStringPreference
import com.hamster.toolbox.compose.scrollTargetId
import com.hamster.toolbox.main.MainViewModel
import com.hamster.toolbox.repository.SettingsRepository
import com.hamster.toolbox.repository.repositorySetBoolean
import com.hamster.toolbox.repository.repositorySetString
import com.hamster.toolbox.repository.settingsStore
import com.hamster.toolbox.system.Receiver
import com.hamster.toolbox.utils.authenticate
import com.hamster.toolbox.utils.convertDateToMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    mainViewModel: MainViewModel,
    onNavigate: (Route) -> Unit,
    setLoading: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    val settingsRepository = remember { SettingsRepository(context.settingsStore) }

    LaunchedEffect(Unit) {
        AI.setScope(ToolScope.SETTINGS)
    }

    val sharedTiltState = rememberSharedTiltState()

    val uriHandler = LocalUriHandler.current

    val avatarPickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                setLoading(true)
                val path = SettingsUtils.saveImageToInternalStorage(context, uri)
                if (path != null) {
                    scope.launch {
                        repositorySetString(context.settingsStore, path, SettingsRepository.USER_AVATAR_PATH)
                    }
                    Toast.makeText(context, "头像已更新", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "更新头像失败", Toast.LENGTH_SHORT).show()
                }
                setLoading(false)
            }
        }
    }
    
    val userName by settingsRepository.userNameFlow.collectAsStateWithLifecycle(initialValue = "无名氏")
    val semesterStartDate by settingsRepository.semesterStartDateFlow.collectAsStateWithLifecycle(initialValue = "")
    val scheduleJson by rememberStringPreference("schedule_json", "")
    val isClassRemindEnabled by settingsRepository.isClassRemindEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    val isDiaryUsingPassword by settingsRepository.isDiaryUsingPassword.collectAsStateWithLifecycle(initialValue = true)

    var showUserAvatarOptionsDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val classRemindRequestPostNotificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scope.launch {
                repositorySetBoolean(context.settingsStore, true, SettingsRepository.IS_CLASS_REMIND_ENABLED)
            }
        } else {
            scope.launch {
                repositorySetBoolean(context.settingsStore, false, SettingsRepository.IS_CLASS_REMIND_ENABLED)
            }
            Toast.makeText(context, "需要通知权限", Toast.LENGTH_SHORT).show()
        }
    }

    VerticalScrollPageColumn(
        sharedTiltState = sharedTiltState,
        scrollTarget = mainViewModel.settingsScrollTarget,
        scrollTrigger = mainViewModel.settingsScrollTrigger
    ) {
        ItemGroup(titleState = sharedTiltState) {
            ClickItem(title = "用户头像", icon = R.drawable.ic_user_avatar) {
                showUserAvatarOptionsDialog = true
            }
            EditTextItem(
                modifier = Modifier.scrollTargetId("nickname"),
                title = "昵称",
                dialogTitle = "修改昵称",
                initialValue = userName,
                hint = "昵称",
                singleLine = true,
                icon = R.drawable.ic_user_name,
                onCancel = { },
                onConfirm = { input ->
                    scope.launch {
                        repositorySetString(context.settingsStore, input, SettingsRepository.USER_NAME)
                    }
                    true
                }
            )
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            ClickItem(
                modifier = Modifier.scrollTargetId("semester_start_date"),
                title = "学期开始日期",
                summary = if (semesterStartDate == "") "未设置" else semesterStartDate,
                icon = R.drawable.ic_calendar
            ) {
                showDatePickerDialog = true
            }
            ClickItem(
                modifier = Modifier.scrollTargetId("import_curriculum_options"),
                title = "导入课程表",
                summary = if (scheduleJson.isBlank()) "未设置" else "已设置",
                icon = R.drawable.ic_curriculum
            ) {
                if (semesterStartDate.isEmpty()) {
                    mainViewModel.setSettingsScrollTarget("semester_start_date")
                    return@ClickItem
                }
                onNavigate(ImportCurriculum)
            }
            SwitchItem(
                modifier = Modifier.scrollTargetId("class_notification"),
                title = "上课提醒",
                summary = "上课前发送通知",
                checked = isClassRemindEnabled,
                icon = R.drawable.ic_message,
                onCheckedChange = { isChecked ->
                    if (isChecked) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                                classRemindRequestPostNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                return@SwitchItem
                            }
                        }
                    }

                    scope.launch {
                        repositorySetBoolean(context.settingsStore, isChecked, SettingsRepository.IS_CLASS_REMIND_ENABLED)
                    }

                    Receiver.dailyNotification(
                        context, 22, 0, Receiver.ACTION_CLASS_ALARM_CHECK, 101, isClassRemindEnabled, emptyArray()
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            SwitchItem(
                modifier = Modifier.scrollTargetId("is_diary_using_password"),
                title = "使用密码保护日记",
                checked = isDiaryUsingPassword,
                icon = R.drawable.ic_diary
            ) { checked ->
                if (!checked) {
                    authenticate(
                        context = context,
                        title = "取消密码确认",
                        onSuccess = {
                            scope.launch {
                                repositorySetBoolean(context.settingsStore, checked, SettingsRepository.IS_DIARY_USING_PASSWORD)
                            }
                        },
                        onNoPasswordSet = {
                            scope.launch {
                                repositorySetBoolean(context.settingsStore, checked, SettingsRepository.IS_DIARY_USING_PASSWORD)
                            }
                        }
                    )
                } else {
                    scope.launch {
                        repositorySetBoolean(context.settingsStore, true, SettingsRepository.IS_DIARY_USING_PASSWORD)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            ClickItem(
                modifier = Modifier.scrollTargetId("assistant_settings"),
                title = "助手设置",
                icon = R.drawable.ic_assistant
            ) {
                onNavigate(AssistantSettings)
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        ItemGroup(titleState = sharedTiltState) {
            ClickItem(
                modifier = Modifier.scrollTargetId("GitHub"),
                title = "Github",
                icon = R.drawable.ic_github
            ) {
                uriHandler.openUri("https://github.com/super-super-hamster/Toolbox-for-Android")
            }
        }
    }

    if (showUserAvatarOptionsDialog) {
        InquiryDialog(
            title = "修改头像",
            content = "",
            cancelText = "恢复默认",
            confirmText = "相册中选择",
            onCancel = {
                try {
                    val file = File(context.filesDir, "user_avatar.png")
                    if (file.exists()) {
                        file.delete()
                    }
                    scope.launch {
                        repositorySetString(context.settingsStore, "", SettingsRepository.USER_AVATAR_PATH)
                    }
                    Toast.makeText(context, "已恢复默认头像", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                } },
            onDismissRequest = { showUserAvatarOptionsDialog = false },
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
                if (semesterStartDate == "") null else semesterStartDate
            ),
            onDateSelected = { millis ->
                if (millis != null) {
                    val instant = Instant.ofEpochMilli(millis)
                    val selectedDateObj = instant.atZone(ZoneOffset.UTC).toLocalDate()
                    val dayOfWeek = selectedDateObj.dayOfWeek.value
                    val alignedDate = selectedDateObj.minusDays((dayOfWeek - 1).toLong())
                    val newDateString = alignedDate.toString()
                    scope.launch {
                        repositorySetString(context.settingsStore, newDateString, SettingsRepository.SEMESTER_START_DATE)
                    }
                    prefs.edit{ putString("semester_start_date", newDateString)}
                } },
            onDismiss = {
                showDatePickerDialog = false
            }
        )
    }
}

object SettingsUtils {
    fun cropBitmapToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val newWidth = if (height > width) width else height
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

    suspend fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                val squareBitmap = cropBitmapToSquare(bitmap)
                val resizedBitmap = resizeBitmap(squareBitmap, 256)

                val file = File(context.filesDir, "user_avatar.png")
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