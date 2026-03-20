package com.hamster.toolbox.utils.compose

import androidx.annotation.Keep
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.annotations.SerializedName
import com.hamster.toolbox.R

@Keep
data class TabItem(
    @SerializedName("title") val title: String,
    @SerializedName("screen") val screen: @Composable () -> Unit
)

@Composable
fun Tabs(
    tabs: List<TabItem>,
    selectedIndex: Int,
    setSelectedIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SegmentedControl(
            items = tabs.map { it.title },
            selectedIndex = selectedIndex,
            onItemSelected = { setSelectedIndex(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Crossfade(
            targetState = selectedIndex,
            animationSpec = tween(durationMillis = 300),
            label = "PageTransition"
        ) { pageIndex ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                tabs[pageIndex].screen()
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun SegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val tabWidths = remember { mutableStateMapOf<Int, Dp>() }
    val tabOffsets = remember { mutableStateMapOf<Int, Dp>() }

    val targetWidth = tabWidths[selectedIndex] ?: 0.dp
    val targetOffset = tabOffsets[selectedIndex] ?: 0.dp

    val indicatorWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = tween(durationMillis = 300),
        label = "indicatorWidth"
    )
    val indicatorOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = tween(durationMillis = 300),
        label = "indicatorOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = colorResource(R.color.tabs_bg), shape = squircleShape)
            .padding(6.dp)
            .height(IntrinsicSize.Min)
    ) {
        if (indicatorWidth > 0.dp) {
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(indicatorWidth)
                    .fillMaxHeight()
                    .shadow(
                        elevation = 2.dp,
                        shape = squircleShape,
                        clip = false,
                        spotColor = colorResource(id = R.color.item_group_card_shadow),  // 主阴影
                        ambientColor = colorResource(id = R.color.item_group_card_shadow)// 柔和阴影
                    )
                    .clip(squircleShape)
                    .background(colorResource(R.color.tabs_selected_bg))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            items.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) colorResource(R.color.tabs_selected_text) else colorResource(R.color.tabs_text),
                    label = "TabText"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned { coordinates ->
                            tabWidths[index] = with(density) { coordinates.size.width.toDp() }
                            tabOffsets[index] = with(density) { coordinates.positionInParent().x.toDp() }
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onItemSelected(index) }
                        )
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}