package top.yukonga.miuix.files.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.files.R
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable
import java.io.File

@Composable
fun EsPathBar(
    currentDir: File,
    onNavigate: (File) -> Unit,
    modifier: Modifier = Modifier,
    percentage: Int = 22
) {
    val scrollState = rememberScrollState()

    // Build path segments
    val segments = mutableListOf<File>()
    var curr: File? = currentDir
    while (curr != null) {
        segments.add(0, curr)
        curr = curr.parentFile
    }

    LaunchedEffect(segments.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(EsBgColor)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Path breadcrumbs (scrollable)
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            segments.forEachIndexed { index, file ->
                val isLast = index == segments.size - 1
                val displayName = when {
                    index == 0 -> "/"
                    file.name.isEmpty() -> "/"
                    else -> file.name
                }

                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .pressable(
                            interactionSource = interactionSource,
                            indication = SinkFeedback()
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = !isLast
                        ) { onNavigate(file) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = displayName,
                        fontSize = 12.5.sp,
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                        color = if (isLast) EsTextPrimary else EsTextSecondary
                    )
                }

                if (!isLast) {
                    Text(
                        text = "›",
                        fontSize = 14.sp,
                        color = EsTextSecondary.copy(alpha = 0.65f),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right Gauge Chip (e.g. 22%)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.toolbar_diskanalyzer_nomal),
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                colorFilter = ColorFilter.tint(EsTextSecondary)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$percentage%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = EsTextPrimary
            )
        }
    }
}
