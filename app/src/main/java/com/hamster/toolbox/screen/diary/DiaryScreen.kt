package com.hamster.toolbox.screen.diary

import android.app.Activity
import android.view.WindowManager
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.IntOffset
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

data class SegmentUiModel(
    val entity: DiarySegmentEntity,
    val localId: String = java.util.UUID.randomUUID().toString()
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryScreen(
    mainViewModel: MainViewModel,
    viewModel: DiaryViewModel,
) {
    val sharedTiltState = rememberSharedTiltState()
    val dateFormatter = remember { SimpleDateFormat("yyyy年M月d日", Locale.getDefault()) }

    val isKeyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardVisible) {
        mainViewModel.showBottomMenu = !isKeyboardVisible
    }

    val diary by viewModel.getDiary().collectAsState(initial = null)

    val focusManager = LocalFocusManager.current

    var newlyAddedImagePath by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        mainViewModel.showBottomMenu = false

        onDispose {
            mainViewModel.showBottomMenu = true
        }
    }

    // 编辑文本时让软键盘以 insets(adjustResize)方式处理,避免系统整窗平移导致顶栏被顶走/底部出现空白
    val rootView = LocalView.current
    DisposableEffect(rootView) {
        val window = (rootView.context as? Activity)?.window
        val previousSoftInputMode = window?.attributes?.softInputMode
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        onDispose {
            if (window != null && previousSoftInputMode != null) {
                window.setSoftInputMode(previousSoftInputMode)
            }
        }
    }

    var segments by remember { mutableStateOf<List<SegmentUiModel>>(emptyList()) }
    var titleText by remember { mutableStateOf("") }

    var isInitialLoaded by remember { mutableStateOf(false) }
    var focusIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(diary) {
        if (!isInitialLoaded && diary != null) {
            titleText = diary?.diary?.title ?: ""
            val initialSegments = diary?.segments?.sortedBy { it.position } ?: emptyList()
            segments = if (initialSegments.isEmpty()) {
                listOf(SegmentUiModel(DiarySegmentEntity(diaryId = diary?.diary?.id ?: 0, type = SegmentType.TEXT, content = "", position = 0)))
            } else {
                initialSegments.map { SegmentUiModel(it) }
            }
            isInitialLoaded = true
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.saveImageToLocal(uri) { finalLocalPath ->
                val insertIndex = if (focusIndex != null) focusIndex!! + 1 else segments.size
                
                val newImage = DiarySegmentEntity(
                    diaryId = diary?.diary?.id ?: 0,
                    type = SegmentType.IMAGE,
                    content = finalLocalPath,
                    position = 0 
                )
                
                val newSegments = segments.toMutableList()
                newSegments.add(insertIndex, SegmentUiModel(newImage))
                segments = newSegments.mapIndexed { index, seg -> seg.copy(entity = seg.entity.copy(position = index)) }
                
                newlyAddedImagePath = finalLocalPath
            }
        }
    }

    val latestBaseDiary by rememberUpdatedState(newValue = diary)
    val latestSegments by rememberUpdatedState(newValue = segments)
    val latestTitle by rememberUpdatedState(newValue = titleText)
    val lifecycleOwner = LocalLifecycleOwner.current

    val performSave = {
        latestBaseDiary?.let { baseDiary ->
            val textContent = latestSegments.filter { it.entity.type == SegmentType.TEXT }.joinToString("\n") { it.entity.content }
            val diaryToSave = baseDiary.copy(
                diary = baseDiary.diary.copy(
                    wordCount = textContent.length,
                    title = latestTitle.ifBlank { null },
                ),
                segments = latestSegments.mapIndexed { index, seg -> seg.entity.copy(position = index) }
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
    var draggingSegment by remember { mutableStateOf<SegmentUiModel?>(null) }

    LaunchedEffect(draggingSegment) {
        if (draggingSegment != null) {
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
            .padding()
            .imePadding()
            .verticalScroll(scrollState),
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

        val itemHeights = remember { mutableStateMapOf<Int, Float>() }

        val gapHeight = dimensionResource(id = R.dimen.item_group_gap)
        val spacerHeightPx = remember(density, gapHeight) {
            with(density) { gapHeight.toPx() }
        }

        var currentDragOffsetY by remember { mutableFloatStateOf(0f) }
        var initialScrollPos by remember { mutableIntStateOf(0) }

        val activeOriginPos = draggingSegment?.let { segments.indexOf(it) }

        val crossThreshold = with(density) { 50.dp.toPx() }

        val activeTargetPos by remember(activeOriginPos, spacerHeightPx) {
            derivedStateOf {
                if (activeOriginPos == null || activeOriginPos < 0) return@derivedStateOf null

                val totalY = currentDragOffsetY + (scrollState.value - initialScrollPos).toFloat()
                var target = activeOriginPos
                var currentY = 0f

                if (totalY > 0) {
                    while (target < segments.size - 1) {
                        val stepToCross = (itemHeights[target + 1] ?: 0f) + spacerHeightPx
                        val threshold = minOf(stepToCross / 2, crossThreshold)

                        if (totalY > currentY + threshold) {
                            currentY += stepToCross
                            target++
                        } else break
                    }
                } else if (totalY < 0) {
                    while (target > 0) {
                        val stepToCross = (itemHeights[target - 1] ?: 0f) + spacerHeightPx
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

        val calculateShift = { index: Int ->
            var shift = 0f
            if (draggingSegment != null && activeOriginPos != null && activeTargetPos != null && activeOriginPos >= 0) {
                val draggedHeight = itemHeights[activeOriginPos] ?: with(density) { 150.dp.toPx() }
                val exactGapPx = draggedHeight + spacerHeightPx

                val targetDPos = activeTargetPos!!

                if (targetDPos > activeOriginPos) {
                    if (index in (activeOriginPos + 1)..targetDPos) {
                        shift = -exactGapPx
                    }
                } else if (targetDPos < activeOriginPos) {
                    if (index in targetDPos..<activeOriginPos) {
                        shift = exactGapPx
                    }
                }
            }
            shift
        }

        segments.forEachIndexed { index, segmentUi ->
            val segment = segmentUi.entity
            val shiftAmount = calculateShift(index)
            
            key(segmentUi.localId) {
                if (segment.type == SegmentType.IMAGE) {
                    DiaryImageItem(
                        imagePath = segment.content,
                        dragScrollDeltaProvider = {
                            if (draggingSegment == segmentUi) (scrollState.value - initialScrollPos).toFloat() else 0f
                        },
                        onDelete = { segments = segments.filter { it != segmentUi } },
                        titleState = sharedTiltState,
                        onDrag = { dragY -> currentDragOffsetY = dragY },
                        onDragStart = {
                            draggingSegment = segmentUi
                            initialScrollPos = scrollState.value
                        },
                        onDragEnd = {
                            activeTargetPos?.let { finalTarget ->
                                if (finalTarget != activeOriginPos && activeOriginPos != null && activeOriginPos >= 0) {
                                    val newList = segments.toMutableList()
                                    val item = newList.removeAt(activeOriginPos)
                                    newList.add(finalTarget, item)
                                    segments = newList.mapIndexed { i, seg -> seg.copy(entity = seg.entity.copy(position = i)) }
                                }
                            }
                            draggingSegment = null
                            currentDragOffsetY = 0f
                        },
                        onHeightMeasured = { measuredHeight ->
                            itemHeights[index] = measuredHeight
                        },
                        visualShiftY = shiftAmount,
                        onPositionInRoot = { y -> draggedItemCenterY = y },
                        isNewlyAdded = (newlyAddedImagePath == segment.content),
                    )
                } else {
                    DiaryTextItem(
                        text = segment.content,
                        visualShiftY = shiftAmount,
                        isFocused = focusIndex == index,
                        onFocusClear = { focusIndex = null },
                        onTextChange = { newText ->
                            if (newText.contains("\n")) {
                                val parts = newText.split("\n")
                                val newList = segments.toMutableList()
                                newList.removeAt(index)
                                val newSegments = parts.map { part ->
                                    SegmentUiModel(DiarySegmentEntity(diaryId = segment.diaryId, type = SegmentType.TEXT, content = part, position = 0))
                                }
                                newList.addAll(index, newSegments)
                                segments = newList.mapIndexed { i, seg -> seg.copy(entity = seg.entity.copy(position = i)) }
                                focusIndex = index + parts.size - 1
                            } else {
                                val newList = segments.toMutableList()
                                newList[index] = segmentUi.copy(entity = segment.copy(content = newText))
                                segments = newList
                            }
                        },
                        onDeleteEmpty = {
                            if (index > 0) {
                                val newList = segments.toMutableList()
                                newList.removeAt(index)
                                segments = newList.mapIndexed { i, seg -> seg.copy(entity = seg.entity.copy(position = i)) }
                                focusIndex = index - 1
                            }
                        },
                        onHeightMeasured = { measuredHeight ->
                            itemHeights[index] = measuredHeight
                        },
                        titleState = sharedTiltState,
                        mainViewModel = mainViewModel,
                    )
                }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryTextItem(
    text: String,
    isFocused: Boolean,
    onFocusClear: () -> Unit,
    mainViewModel: MainViewModel,
    onTextChange: (String) -> Unit,
    onDeleteEmpty: () -> Unit,
    modifier: Modifier = Modifier,
    titleState: SharedTiltState,
    visualShiftY: Float = 0f,
    onHeightMeasured: (Float) -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var isFieldFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusRequester.requestFocus()
            onFocusClear()
        }
    }

    // 键盘弹出时只滚动本页内容,把当前编辑块带到键盘上方可视区(避免整窗上移)
    val isKeyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(isFieldFocused, isKeyboardVisible) {
        if (isFieldFocused && isKeyboardVisible) {
            // 键盘/imePadding 动画期间持续对齐;无需滚动时 bringIntoView 为空操作
            repeat(4) {
                delay(100)
                bringIntoViewRequester.bringIntoView()
            }
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
            .onGloballyPositioned { coordinates ->
                onHeightMeasured(coordinates.size.height.toFloat())
            }
            .offset { IntOffset(0, animatedShiftY.roundToInt()) }
            .bringIntoViewRequester(bringIntoViewRequester)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // 点击文本块(含无文字的空白区域)时聚焦该块
                focusRequester.requestFocus()
            }
    ) {
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
                .onFocusChanged { isFieldFocused = it.isFocused }
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