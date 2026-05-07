package com.hamster.toolbox.screen.diary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hamster.toolbox.Diary
import com.hamster.toolbox.R
import com.hamster.toolbox.Route
import com.hamster.toolbox.ai.AI
import com.hamster.toolbox.ai.tools.ToolScope
import com.hamster.toolbox.compose.DatePicker
import com.hamster.toolbox.compose.InquiryDialog
import com.hamster.toolbox.compose.ItemGroup
import com.hamster.toolbox.compose.PageColumn
import com.hamster.toolbox.compose.StandardDialog
import com.hamster.toolbox.compose.TextInputField
import com.hamster.toolbox.compose.rememberBooleanPreference
import com.hamster.toolbox.compose.rememberSharedTiltState
import com.hamster.toolbox.compose.squircleShape
import com.hamster.toolbox.main.MainViewModel
import com.hamster.toolbox.utils.authenticate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DiaryPreviewScreen(
    mainViewModel: MainViewModel,
    viewModel: DiaryViewModel,
    onNavigate: (Route) -> Unit
) {
    val context = LocalContext.current
    val sharedTiltState = rememberSharedTiltState()

    LaunchedEffect(Unit) {
        AI.setScope(ToolScope.DIARY)
    }

    val diaries by viewModel.diaries.collectAsStateWithLifecycle(initialValue = emptyMap())

    // rememberSaveable 在页面重组或跳转返回后保留状态
    var selectedYear by rememberSaveable { mutableIntStateOf(-1) }
    var selectedMonth by rememberSaveable { mutableIntStateOf(-1) }

    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var diaryTitle by remember { mutableStateOf("") }

    var showDeleteDiaryDialog by remember { mutableStateOf(false) }
    var deleteDiaryId by remember { mutableLongStateOf(-1) }
    var deleteDate by remember { mutableStateOf("") }

    var isLocked by remember { mutableStateOf(true) }
    val isDiaryUsingPassword by rememberBooleanPreference("is_diary_using_password", true)

    LaunchedEffect(Unit) {
        if (isDiaryUsingPassword) {
            authenticate(
                context = context,
                title = "解锁日记",
                onSuccess = {
                    isLocked = false
                },
                onNoPasswordSet = {
                    isLocked = false
                }
            )
        } else {
            isLocked = false
        }
    }

    PageColumn(modifier = Modifier.verticalScroll(rememberScrollState()), sharedTiltState = sharedTiltState) {
        if (!isLocked) {
            diaries.forEach { (year, months) ->
                ItemGroup(titleState = sharedTiltState) {
                    DiaryItem(title = "$year 年", onClick = {
                        selectedYear = if (selectedYear == year) -1 else year
                        selectedMonth = -1
                    })

                    AnimatedVisibility(
                        visible = selectedYear == year,
                        enter = expandVertically(animationSpec = tween(durationMillis = 300)),
                        exit = shrinkVertically(animationSpec = tween(durationMillis = 300))
                    ) {
                        Column {
                            months.forEach { (month, diary) ->
                                DiaryItem(title = "    $month 月", onClick = {
                                    selectedMonth = if (selectedMonth == month) -1 else month
                                })

                                AnimatedVisibility(
                                    visible = selectedMonth == month,
                                    enter = expandVertically(animationSpec = tween(durationMillis = 300)),
                                    exit = shrinkVertically(animationSpec = tween(durationMillis = 300))
                                ) {
                                    Column {
                                        diary.forEach {
                                            val day = remember(it.date) {
                                                Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_MONTH)
                                            }
                                            val title = "        $day 日" + if (it.title.isNullOrBlank()) "" else " - ${it.title}"

                                            DiaryItem(title = title, wordCount = it.wordCount, onClick = {
                                                viewModel.selectedDiaryDate = it.date
                                                onNavigate(Diary)
                                            }) {
                                                deleteDiaryId = it.id
                                                deleteDate = dateFormatter.format(Date(it.date))
                                                showDeleteDiaryDialog = true
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))
            }

            if (mainViewModel.showAddDiaryDialog) {
                StandardDialog(onDismissRequest = { mainViewModel.showAddDiaryDialog = false }) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(0.85f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                        ) {
                            Text(
                                text = dateFormatter.format(Date(currentTime)),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )

                            Icon(
                                painter = painterResource(R.drawable.ic_edit_calendar),
                                contentDescription = null,
                                tint = colorResource(R.color.icon),
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopEnd)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        mainViewModel.showAddDiaryDialog = false
                                        showDatePicker = true
                                    }
                            )
                        }


                        Spacer(modifier = Modifier.height(16.dp))

                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (diaryTitle.isEmpty()) {
                                Text("标题", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
                            }
                            TextInputField(
                                value = diaryTitle,
                                onValueChange = { diaryTitle = it },
                                textStyle = LocalTextStyle.current.copy(
                                    textAlign = TextAlign.Center,
                                    color = colorResource(R.color.text),
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Button(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                border = BorderStroke(1.dp, Color.LightGray),
                                shape = squircleShape,
                                colors = ButtonDefaults.textButtonColors(Color.Transparent),
                                onClick = {
                                    mainViewModel.showAddDiaryDialog = false
                                    diaryTitle = ""
                                    currentTime = System.currentTimeMillis()
                                }
                            ) {
                                Text(text = "取消", color = Color.Gray)
                            }

                            Button(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                border = BorderStroke(1.dp, Color.LightGray),
                                shape = squircleShape,
                                colors = ButtonDefaults.textButtonColors(colorResource(R.color.btn_confirm)),
                                onClick = {
                                    viewModel.createDiary(
                                        title = diaryTitle,
                                        date = currentTime
                                    ) {
                                        onNavigate(Diary)
                                        diaryTitle = ""
                                        currentTime = System.currentTimeMillis()
                                        mainViewModel.showAddDiaryDialog = false
                                    }
                                }
                            ) {
                                Text(text = "确认", color = colorResource(R.color.text))
                            }
                        }
                    }
                }
            }

            if (showDatePicker) {
                DatePicker(
                    initialSelectedDateMillis = currentTime,
                    onDismiss = {
                        showDatePicker = false
                        mainViewModel.showAddDiaryDialog = true
                    },
                    onDateSelected = {
                        if (it != null) {
                            currentTime = it
                        }
                        showDatePicker = false
                        mainViewModel.showAddDiaryDialog = true
                    }
                )
            }

            if (showDeleteDiaryDialog) {
                InquiryDialog(
                    title = "删除确认",
                    content = "要删除$deleteDate 的日记吗？",
                    onCancel = { showDeleteDiaryDialog = false },
                    onDismissRequest = { showDeleteDiaryDialog = false }
                ) {
                    viewModel.deleteDiary(deleteDiaryId)
                    true
                }
            }
        }
    }
}

@Composable
fun DiaryItem(
    title: String,
    wordCount: Int = -1,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    var isRotated by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isRotated) 90f else 0f
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    isRotated = !isRotated
                    onClick()
                },
                onLongClick = {
                    onLongClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontSize = 16.sp, color = colorResource(id = R.color.text))
            if (wordCount >= 0) {
                Text(text = "$wordCount 字", fontSize = 12.sp, color = colorResource(id = R.color.text))
            }
        }

        Icon(
            painter = painterResource(id = R.drawable.arrow_right_bold),
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .rotate(rotation),
            tint = colorResource(R.color.icon)
        )
    }

    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}