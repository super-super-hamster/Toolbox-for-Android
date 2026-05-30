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
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.hamster.toolbox.R
import com.hamster.toolbox.compose.ItemGroup
import com.hamster.toolbox.compose.OptionDialog
import com.hamster.toolbox.compose.PageColumn
import com.hamster.toolbox.compose.SharedTiltState
import com.hamster.toolbox.compose.TextInputField
import com.hamster.toolbox.compose.rememberSharedTiltState
import com.hamster.toolbox.main.MainViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DiaryScreen(
    mainViewModel: MainViewModel,
    viewModel: DiaryViewModel,
) {
    val sharedTiltState = rememberSharedTiltState()
    val dateFormatter = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()) }

    val diary by viewModel.getDiary().collectAsState(initial = null)

    var newlyAddedImagePath by remember { mutableStateOf<String?>(null) }


    var paragraphs by remember { mutableStateOf(listOf("")) }
    var diaryImages by remember { mutableStateOf<List<DiaryImageEntity>>(emptyList()) }
    var titleText by remember { mutableStateOf("") }

    var isInitialLoaded by remember { mutableStateOf(false) }
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

                 newlyAddedImagePath = finalLocalPath
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

    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    var containerTop by remember { mutableFloatStateOf(0f) }
    var containerBottom by remember { mutableFloatStateOf(0f) }
    var draggedItemCenterY by remember { mutableFloatStateOf(0f) }
    var draggingImage by remember { mutableStateOf<DiaryImageEntity?>(null) }

    LaunchedEffect(draggingImage) {
        if (draggingImage != null) {
            val edgeThreshold = with(density) { 100.dp.toPx() }
            val maxScrollSpeed = with(density) { 25.dp.toPx() }

            while (true) {
                var scrollDelta = 0f
                if (draggedItemCenterY != 0f && containerBottom != 0f) {
                    val distanceToTop = draggedItemCenterY - containerTop
                    val distanceToBottom = containerBottom - draggedItemCenterY

                    if (distanceToTop < edgeThreshold) {
                        val factor = (1f - (distanceToTop / edgeThreshold)).coerceIn(0f, 1f)
                        scrollDelta = -(maxScrollSpeed * factor)
                    } else if (distanceToBottom < edgeThreshold) {
                        val factor = (1f - (distanceToBottom / edgeThreshold)).coerceIn(0f, 1f)
                        scrollDelta = maxScrollSpeed * factor
                    }
                }

                if (scrollDelta != 0f) {
                    scrollState.scrollTo(scrollState.value + scrollDelta.roundToInt())
                }
                delay(16L)
            }
        }
    }

    PageColumn(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInRoot()
                containerTop = bounds.top
                containerBottom = bounds.bottom
            }
            .verticalScroll(scrollState)
            .imePadding(),
        sharedTiltState = sharedTiltState
    ) {
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

        val imageHeights = remember { mutableStateMapOf<Long, Float>() }
        val textHeights = remember { mutableStateMapOf<Int, Float>() }

        val gapHeight = dimensionResource(id = R.dimen.item_group_gap)
        val spacerHeightPx = remember(density, gapHeight) {
            with(density) { gapHeight.toPx() }
        }

        var currentDragOffsetY by remember { mutableFloatStateOf(0f) }
        var initialScrollPos by remember { mutableIntStateOf(0) }

        val activeOriginPos = draggingImage?.position

        val crossThreshold = with(density) { 50.dp.toPx() }

        val activeTargetPos by remember(activeOriginPos, spacerHeightPx) {
            derivedStateOf {
                if (activeOriginPos == null) return@derivedStateOf null

                val totalY = currentDragOffsetY + (scrollState.value - initialScrollPos).toFloat()
                var target = activeOriginPos
                var currentY = 0f

                if (totalY > 0) {
                    while (target < paragraphs.size) {
                        val stepToCross = (textHeights[target] ?: 0f) + spacerHeightPx
                        val threshold = minOf(stepToCross / 2, crossThreshold)

                        if (totalY > currentY + threshold) {
                            currentY += stepToCross
                            target++
                        } else break
                    }
                } else if (totalY < 0) {
                    while (target > 0) {
                        val stepToCross = (textHeights[target - 1] ?: 0f) + spacerHeightPx
                        val threshold = minOf(stepToCross / 2, crossThreshold)

                        if (-totalY > currentY + threshold) {
                            currentY += stepToCross
                            target--
                        } else break
                    }
                }
                target
            }
        }

        val calculateShift = { itemPosition: Int ->
            var shift = 0f
            if (draggingImage != null && activeOriginPos != null && activeTargetPos != null) {
                val draggedImageHeight = imageHeights[draggingImage!!.imageId] ?: with(density) { 150.dp.toPx() }
                val exactGapPx = draggedImageHeight + spacerHeightPx

                if (activeOriginPos < activeTargetPos!!) {
                    if (itemPosition in activeOriginPos until activeTargetPos!!) {
                        shift = -exactGapPx
                    }
                } else if (activeOriginPos > activeTargetPos!!) {
                    if (itemPosition in activeTargetPos!! until activeOriginPos) {
                        shift = exactGapPx
                    }
                }
            }
            shift
        }

        paragraphs.forEachIndexed { index, text ->
            val shiftAmount = calculateShift(index)
            val imagesAtThisPosition = diaryImages.filter { it.position == index }

            imagesAtThisPosition.forEach { image ->
                key(image.imageId, image.localPath) {
                    DiaryImageItem(
                        imagePath = image.localPath,
                        dragScrollDeltaProvider = {
                            if (draggingImage == image) (scrollState.value - initialScrollPos).toFloat() else 0f
                        },
                        onDelete = { diaryImages = diaryImages.filter { it != image } },
                        titleState = sharedTiltState,
                        onDrag = { dragY -> currentDragOffsetY = dragY },
                        onDragStart = {
                            draggingImage = image
                            initialScrollPos = scrollState.value
                        },
                        onDragEnd = {
                            activeTargetPos?.let { finalTarget ->
                                if (finalTarget != image.position) {
                                    diaryImages = diaryImages.map { img ->
                                        if (img === image) {
                                            img.copy(position = finalTarget)
                                        } else {
                                            img
                                        }
                                    }
                                }
                            }
                            draggingImage = null
                            currentDragOffsetY = 0f
                        },
                        onHeightMeasured = { measuredHeight ->
                            imageHeights[image.imageId] = measuredHeight
                        },
                        visualShiftY = shiftAmount,
                        onPositionInRoot = { y -> draggedItemCenterY = y },
                        isNewlyAdded = (newlyAddedImagePath == image.localPath),
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))
                }
            }

            DiaryTextItem(
                text = text,
                visualShiftY = shiftAmount,
                isFocused = focusIndex == index,
                onFocusClear = { focusIndex = null },
                onTextChange = { newText ->
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
                onHeightMeasured = { measuredHeight ->
                    textHeights[index] = measuredHeight
                },
                titleState = sharedTiltState,
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))
        }

        val shiftAmountLast = calculateShift(paragraphs.size)
        val lastImages = diaryImages.filter { it.position == paragraphs.size }
        lastImages.forEach { image ->
            key(image.imageId, image.localPath) {
                DiaryImageItem(
                    imagePath = image.localPath,
                    dragScrollDeltaProvider = {
                        if (draggingImage == image) (scrollState.value - initialScrollPos).toFloat() else 0f
                    },
                    onDelete = { diaryImages = diaryImages.filter { it != image } },
                    titleState = sharedTiltState,
                    onDragStart = {
                        draggingImage = image
                        initialScrollPos = scrollState.value
                    },
                    onDragEnd = {
                        activeTargetPos?.let { finalTarget ->
                            if (finalTarget != image.position) {
                                diaryImages = diaryImages.map { img ->
                                    if (img === image) {
                                        img.copy(position = finalTarget)
                                    } else img
                                }
                            }
                        }
                        draggingImage = null
                        currentDragOffsetY = 0f
                    },
                    onDrag = { dragY -> currentDragOffsetY = dragY },
                    onHeightMeasured = { measuredHeight ->
                        imageHeights[image.imageId] = measuredHeight
                    },
                    visualShiftY = shiftAmountLast,
                    onPositionInRoot = { y -> draggedItemCenterY = y },
                    isNewlyAdded = (newlyAddedImagePath == image.localPath),
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_group_gap)))
            }
        }

        if (viewModel.showTitleSuggestionDialog) {
            OptionDialog(
                title = "标题建议",
                options = viewModel.titleSuggestion,
                singleSelect = true,
                onDismissRequest = { viewModel.showTitleSuggestionDialog = false },
                onConfirm = {
                    if (it.isNotEmpty()) {
                        titleText = viewModel.titleSuggestion[it.first()]
                    }
                }
            )
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
    visualShiftY: Float = 0f,
    onHeightMeasured: (Float) -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val density = LocalDensity.current
    val floatingBarHeightPx = remember(density) {
        with(density) { 80.dp.toPx() }
    }
    var componentSize by remember { mutableStateOf(IntSize.Zero) }

    var hasFocus by remember { mutableStateOf(false) }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusRequester.requestFocus()
            hasFocus = true
            onFocusClear()
        }
    }

    LaunchedEffect(text, hasFocus) {
        if (hasFocus && componentSize != IntSize.Zero) {
            delay(50)
            val requestRect = Rect(
                left = 0f,
                top = 0f,
                right = componentSize.width.toFloat(),
                bottom = componentSize.height.toFloat() + floatingBarHeightPx
            )
            bringIntoViewRequester.bringIntoView(requestRect)
        }
    }

    val animatedShiftY by animateFloatAsState(
        targetValue = visualShiftY,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "TextShiftAnimation"
    )

    ItemGroup(
        titleState = titleState,
        modifier = modifier
            .onSizeChanged { componentSize = it }
            .onGloballyPositioned { coordinates ->
                onHeightMeasured(coordinates.size.height.toFloat())
            }
            .offset { IntOffset(0, animatedShiftY.roundToInt()) }
            .bringIntoViewRequester(bringIntoViewRequester)
    ) {
        TextInputField(
            value = text,
            onValueChange = onTextChange,
            textStyle = TextStyle(
                fontSize = 16.sp,
                textIndent = TextIndent(firstLine = 2.em)
            ),
            modifier = Modifier
                .onFocusChanged { focusState ->
                    hasFocus = focusState.isFocused
                }
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
    visualShiftY: Float = 0f,
    dragScrollDeltaProvider: () -> Float = { 0f },
    onHeightMeasured: (Float) -> Unit = {},
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onPositionInRoot: (Float) -> Unit = {},
    isNewlyAdded: Boolean = false,
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnPositionInRoot by rememberUpdatedState(onPositionInRoot)
    val currentDragScrollDeltaProvider by rememberUpdatedState(dragScrollDeltaProvider)

    var isDeleteMode by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "ImageScaleAnimation"
    )

    val animatedVisualShiftY by animateFloatAsState(
        targetValue = if (isDragging) 0f else visualShiftY,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "ImageShiftAnimation"
    )

    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(isNewlyAdded) {
        if (isNewlyAdded) {
            delay(150)
            bringIntoViewRequester.bringIntoView()
        }
    }

    ItemGroup(
        titleState = titleState,
        modifier = modifier
            .offset {
                val scrollComp = if (isDragging) currentDragScrollDeltaProvider() else 0f
                val currentDrag = if (isDragging) dragOffsetY else 0f
                IntOffset(0, (animatedVisualShiftY + currentDrag + scrollComp).roundToInt())
            }
            .bringIntoViewRequester(bringIntoViewRequester)
            .onGloballyPositioned { coordinates ->
                onHeightMeasured(coordinates.size.height.toFloat())
                if (isDragging) {
                    currentOnPositionInRoot(coordinates.boundsInRoot().center.y)
                }
            }
            .zIndex(if (isDragging) 1f else 0f)
            .scale(scale)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            isDragging = true
                            isDeleteMode = false
                            totalDragDistance = 0f
                            currentOnDragStart()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetY += dragAmount.y
                            totalDragDistance += abs(dragAmount.y)
                            currentOnDrag(dragOffsetY)
                        },
                        onDragEnd = {
                            isDragging = false
                            currentOnDragEnd()
                            dragOffsetY = 0f

                            if (totalDragDistance < 20f) isDeleteMode = true
                        },
                        onDragCancel = {
                            isDragging = false
                            currentOnDragEnd()
                            dragOffsetY = 0f
                        }
                    )
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitFirstDown(requireUnconsumed = false)
                            isPressed = true
                            waitForUpOrCancellation()
                            isPressed = false
                        }
                    }
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
                            currentOnDelete()
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