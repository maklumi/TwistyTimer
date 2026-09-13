package com.aricneto.twistytimer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aricneto.twistytimer.utils.AlgUtils

@Composable
fun CubeComponent(
    state: String,
    modifier: Modifier = Modifier,
    cubeCornerRadius: Float = 8f,
    stickerCornerRadius: Float = 8f
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxSize()
    ) {
        val width = size.width
        val padding = width * 0.04f
        
        // 5 stickers per row (3 full, 2 outer half-stickers)
        val stickerSize = (width - (padding * 6)) / (3 + 0.75f)
        val sizeSubtract = stickerSize / 1.6f
        
        // Draw background
        val backgroundSize = (padding * 6) + (stickerSize * 5) - (sizeSubtract * 2)
        drawRoundRect(
            color = Color(0xFF2E2E2E),
            size = Size(backgroundSize, backgroundSize),
            cornerRadius = CornerRadius(cubeCornerRadius)
        )

        for (i in 0..4) {
            for (j in 0..4) {
                // Ignore the four corners
                if ((i == 0 || i == 4) && (j == 0 || j == 4)) continue

                val colorInt = AlgUtils.getColorFromStateIndex(state, (5 * i) + j)
                val color = Color(colorInt)

                val rectLeft: Float
                val rectTop: Float
                val rectWidth: Float
                val rectHeight: Float
                
                // Y Position
                if (i == 0) {
                    rectTop = padding
                    rectHeight = stickerSize - sizeSubtract
                } else if (i == 4) {
                    rectTop = padding + ((stickerSize + padding) * i) - sizeSubtract
                    rectHeight = stickerSize - sizeSubtract
                } else {
                    rectTop = padding + ((stickerSize + padding) * i) - sizeSubtract
                    rectHeight = stickerSize
                }
                
                // X Position
                if (j == 0) {
                    rectLeft = padding
                    rectWidth = stickerSize - sizeSubtract
                } else if (j == 4) {
                    rectLeft = padding + ((stickerSize + padding) * j) - sizeSubtract
                    rectWidth = stickerSize - sizeSubtract
                } else {
                    rectLeft = padding + ((stickerSize + padding) * j) - sizeSubtract
                    rectWidth = stickerSize
                }

                drawRoundRect(
                    color = color,
                    topLeft = Offset(rectLeft, rectTop),
                    size = Size(rectWidth, rectHeight),
                    cornerRadius = CornerRadius(stickerCornerRadius)
                )
            }
        }
    }
}
