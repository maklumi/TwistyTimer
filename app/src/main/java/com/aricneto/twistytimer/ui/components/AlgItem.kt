package com.aricneto.twistytimer.ui.components

import android.widget.ImageView
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aricneto.twistytimer.items.Algorithm
import com.aricneto.twistytimer.utils.AlgUtils

@Composable
fun AlgItem(
    algorithm: Algorithm,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pState = AlgUtils.getCaseState(context, algorithm.subset, algorithm.name)

    ElevatedCard(
        modifier = modifier
            .padding(6.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = algorithm.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            LinearProgressIndicator(
                progress = { algorithm.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .height(3.dp),
                strokeCap = StrokeCap.Round
            )

            Box(
                modifier = Modifier
                    .size(88.dp) // Total size including margins (72dp + 8dp*2)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                CubeComponent(
                    state = pState,
                    modifier = Modifier.size(72.dp),
                    cubeCornerRadius = 9f,
                    stickerCornerRadius = 2f
                )

                if (algorithm.subset == "PLL") {
                    val arrowDrawable = AlgUtils.getPllArrow(context, algorithm.name)
                    if (arrowDrawable != null) {
                        // Using AndroidView for legacy Drawable support if needed, 
                        // but here we can just show an Image if we convert it.
                        // For simplicity during migration, we can use painterResource if it's a resource.
                        // AlgUtils.getPllArrow returns a Drawable.
                        AndroidView(
                            factory = { context ->
                                ImageView(context).apply {
                                    setImageDrawable(arrowDrawable)
                                }
                            },
                            modifier = Modifier.fillMaxSize().padding(7.dp)
                        )
                    }
                }
            }
        }
    }
}
