package com.hamster.toolbox.screen.gameConsole

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamster.toolbox.R
import com.hamster.toolbox.utils.squircleShape

@Composable
fun Handle (onInput: (ConsoleInput) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(256.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 十字键
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(45.dp,130.dp).background(color = colorResource(R.color.silver), shape = squircleShape))
            Box(modifier = Modifier.size(130.dp,45.dp).background(color = colorResource(R.color.silver), shape = squircleShape))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DPadButton(Icons.Default.KeyboardArrowUp) { onInput(ConsoleInput.UP) }
                Spacer(modifier = Modifier.height(45.dp))
                DPadButton(Icons.Default.KeyboardArrowDown) { onInput(ConsoleInput.DOWN) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                DPadButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft) { onInput(ConsoleInput.LEFT) }
                Spacer(modifier = Modifier.width(45.dp))
                DPadButton(Icons.AutoMirrored.Filled.KeyboardArrowRight) { onInput(ConsoleInput.RIGHT) }
            }
        }

        // AB XY
        Box(modifier = Modifier.size(160.dp)) {
            ActionButton("A", colorResource(R.color.silver), Modifier.align(Alignment.CenterEnd).offset(x = (-10).dp)) { onInput(ConsoleInput.A) }
            ActionButton("B", colorResource(R.color.silver), Modifier.align(Alignment.BottomCenter).offset(y = (-10).dp)) { onInput(ConsoleInput.B) }
            ActionButton("X", colorResource(R.color.silver), Modifier.align(Alignment.TopCenter).offset(y = 10.dp)) { onInput(ConsoleInput.X) }
            ActionButton("Y", colorResource(R.color.silver), Modifier.align(Alignment.CenterStart).offset(x = 10.dp)) { onInput(ConsoleInput.Y) }
        }
    }
}

@Composable
fun DPadButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(45.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
        , contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(30.dp))
    }
}

@Composable
fun ActionButton(text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(45.dp)
            .shadow(4.dp, CircleShape)
            .background(color, CircleShape)
            .clickable{ onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}