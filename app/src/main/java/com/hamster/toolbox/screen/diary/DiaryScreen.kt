package com.hamster.toolbox.screen.diary

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hamster.toolbox.compose.ItemGroup
import com.hamster.toolbox.compose.PageColumn
import com.hamster.toolbox.compose.SharedTiltState
import com.hamster.toolbox.compose.TextInputField
import com.hamster.toolbox.compose.rememberSharedTiltState
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hamster.toolbox.R
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.hamster.toolbox.main.MainViewModel

@Composable
fun DiaryScreen(
    mainViewModel: MainViewModel,
    viewModel: DiaryViewModel,
) {
    val sharedTiltState = rememberSharedTiltState()
    val dateFormatter = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()) }

    val diary by viewModel.getDiary().collectAsState(initial = null)

    var paragraphs by remember { mutableStateOf(listOf("")) }
    var diaryImages by remember { mutableStateOf<List<DiaryImageEntity>>(emptyList()) }
    var titleText by remember { mutableStateOf("") }

    var isInitialLoaded by remember { mutableStateOf(false) }

    // 获取焦点的索引
    var focusIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(diary) {
        if (!isInitialLoaded && diary != null) {
            val content = diary?.diary?.content ?: ""
            titleText = diary?.diary?.title ?: ""
            paragraphs = if (content.isEmpty()) {
                listOf("")
            } else {
                content.split("\n")
            }
            diaryImages = diary?.images?.sortedBy { it.imageId } ?: emptyList()
            isInitialLoaded = true
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.saveImageToLocal(uri) { finalLocalPath ->
                val insertPosition = if (focusIndex != null) focusIndex!! + 1 else paragraphs.size

                val newImage = DiaryImageEntity(
                    diaryId = diary?.diary?.id ?: 0,
                    localPath = finalLocalPath,
                    position = insertPosition
                )

                diaryImages = diaryImages + newImage
            }
        }
    }

    val latestBaseDiary by rememberUpdatedState(newValue = diary)
    val latestParagraphs by rememberUpdatedState(newValue = paragraphs)
    val latestTitle by rememberUpdatedState(newValue = titleText)
    val latestImages by rememberUpdatedState(newValue = diaryImages)
    val lifecycleOwner = LocalLifecycleOwner.current

    val performSave = {
        latestBaseDiary?.let { baseDiary ->
            // 将拆分的段落重新拼成字符串
            val updatedContent = latestParagraphs.joinToString("\n")
            val diaryToSave = baseDiary.copy(
                diary = baseDiary.diary.copy(
                    content = updatedContent,
                    wordCount = updatedContent.length,
                    title = latestTitle.ifBlank { null },
                ),
                images = latestImages
            )
            viewModel.saveDiary(diaryToSave)
        }
    }

    // 自动保存
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                performSave()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            performSave()
        }
    }

    LaunchedEffect(mainViewModel.isAddDiaryImage) {
        if (mainViewModel.isAddDiaryImage) {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            mainViewModel.isAddDiaryImage = false
        }
    }

    PageColumn(modifier = Modifier.verticalScroll(rememberScrollState()), sharedTiltState = sharedTiltState) {
        DiaryTitleItem(
            title = titleText,
            dateString = dateFormatter.format(viewModel.selectedDiaryDate),
            isFocused = focusIndex == -1,
            onFocusClear = { focusIndex = null },
            onTitleChange = { titleText = it },
            onEnterPressed = { focusIndex = 0 },
            titleState = sharedTiltState
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))

        paragraphs.forEachIndexed { index, text ->
            val imagesAtThisPosition = diaryImages.filter { it.position == index }

            imagesAtThisPosition.forEach { image ->
                DiaryImageItem(
                    imagePath = image.localPath,
                    onDelete = {
                        diaryImages = diaryImages.filter { it != image }
                    },
                    titleState = sharedTiltState
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))
            }

            DiaryTextItem(
                text = text,
                isFocused = focusIndex == index,
                onFocusClear = { focusIndex = null },
                onTextChange = { newText ->
                    // 拦截换行符
                    if (newText.contains("\n")) {
                        val parts = newText.split("\n")
                        val newList = paragraphs.toMutableList()

                        newList.removeAt(index)

                        newList.addAll(index, parts)
                        paragraphs = newList

                        focusIndex = index + parts.size - 1
                    } else {
                        val newList = paragraphs.toMutableList()
                        newList[index] = newText
                        paragraphs = newList
                    }
                },
                onDeleteEmpty = {
                    if (index > 0) {
                        val newList = paragraphs.toMutableList()
                        newList.removeAt(index)
                        paragraphs = newList

                        focusIndex = index - 1
                    }
                },
                titleState = sharedTiltState
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))
        }

        val lastImages = diaryImages.filter { it.position == paragraphs.size }
        lastImages.forEach { image ->
            DiaryImageItem(
                imagePath = image.localPath,
                onDelete = { diaryImages = diaryImages.filter { it != image } },
                titleState = sharedTiltState
            )
             Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))
        }
    }
}

@Composable
fun DiaryTextItem(
    text: String,
    isFocused: Boolean,
    onFocusClear: () -> Unit,
    onTextChange: (String) -> Unit,
    onDeleteEmpty: () -> Unit,
    modifier: Modifier = Modifier,
    titleState: SharedTiltState,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusRequester.requestFocus()
            onFocusClear()
        }
    }

    ItemGroup(titleState = titleState, modifier = modifier) {
        TextInputField(
            value = text,
            onValueChange = onTextChange,
            textStyle = TextStyle(
                fontSize = 16.sp,
                textIndent = TextIndent(firstLine = 2.em)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Backspace && event.type == KeyEventType.KeyDown && text.isEmpty()) {
                        onDeleteEmpty()
                        true
                    } else {
                        false
                    }
                }
                .padding(16.dp),
        )
    }
}

@Composable
fun DiaryTitleItem(
    title: String,
    dateString: String,
    isFocused: Boolean,
    onFocusClear: () -> Unit,
    onTitleChange: (String) -> Unit,
    onEnterPressed: () -> Unit,
    modifier: Modifier = Modifier,
    titleState: SharedTiltState,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusRequester.requestFocus()
            onFocusClear()
        }
    }

    ItemGroup(titleState = titleState, modifier = modifier) {
        TextInputField(
            value = title,
            onValueChange = { newText ->
                if (newText.contains("\n")) {
                    onEnterPressed()
                } else {
                    onTitleChange(newText)
                }
            },
            textStyle = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .padding(16.dp),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (title.isBlank()) {
                        Text(
                            text = dateString,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray.copy(alpha = 0.5f)
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun DiaryImageItem(
    imagePath: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    titleState: SharedTiltState,
) {
    var isDeleteMode by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "ImageScaleAnimation"
    )

    ItemGroup(titleState = titleState, modifier = modifier.scale(scale)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
//                .scale(scale)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            try {
                                tryAwaitRelease()
                            } finally {
                                isPressed = false
                            }
                        },
                        onLongPress = {
                            isDeleteMode = true
                        }
                    )
                }
        ) {
            AsyncImage(
                model = imagePath,
                contentDescription = "图片",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .blur(radius = if (isDeleteMode) 16.dp else 0.dp)
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = isDeleteMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.matchParentSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Red.copy(alpha = 0.35f))
                        .clickable { isDeleteMode = false },
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            onDelete()
                            isDeleteMode = false
                        },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = "确认删除",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }
        }
    }
}