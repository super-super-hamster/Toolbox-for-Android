package com.hamster.toolbox.screen.colorPicker

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hamster.toolbox.compose.ItemGroup
import com.hamster.toolbox.compose.PageColumn
import com.hamster.toolbox.compose.rememberSharedTiltState
import com.hamster.toolbox.R
import com.hamster.toolbox.ai.AI
import com.hamster.toolbox.ai.tools.ToolScope
import com.hamster.toolbox.utils.color.getColors

@Composable
fun ColorPickerScreen(
    setLoading: (Boolean) -> Unit
) {
    val sharedTiltState = rememberSharedTiltState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        AI.setScope(ToolScope.COLOR_PICKER)
    }

    var extractedColors by remember { mutableStateOf<List<Color>>(emptyList()) }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                 ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
        }
    }

    LaunchedEffect(bitmap) {
        setLoading(true)
        bitmap?.let { currentBitmap ->
            val colors = getColors(
                bitmap = currentBitmap,
                radius = 20f,
                mergeRadius = 10f,
                ignoreBackground = true,
                count = 5
            )

            // 将获取到的 Int 颜色列表映射为 Compose 的 Color 列表
            extractedColors = colors.map { Color(it) }
        }
        setLoading(false)
    }

    PageColumn(sharedTiltState = sharedTiltState) {
        ItemGroup(titleState = sharedTiltState, modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = {
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(painter = painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(64.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (extractedColors.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp), // 卡片之间的间距
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        extractedColors.forEach { color ->
                            Card(
                                modifier = Modifier.size(48.dp),
                                colors = CardDefaults.cardColors(containerColor = color)
                            ) {
                                // 颜色卡片内容
                            }
                        }
                    }
                }
            }
        }
    }
}