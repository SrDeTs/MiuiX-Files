package miuix.files.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import miuix.files.R
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable
import java.io.File

private val EsAnalyzerTagShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val r = h * 0.25f
    val tip = h * 0.34f
    moveTo(r, 0f)
    lineTo(w - tip, 0f)
    lineTo(w, h / 2f)
    lineTo(w - tip, h)
    lineTo(r, h)
    quadraticTo(0f, h, 0f, h - r)
    lineTo(0f, r)
    quadraticTo(0f, 0f, r, 0f)
    close()
}

@Composable
fun EsAnalyzerPercentageTag(
    percentage: Int,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .height(23.dp)
            .clip(EsAnalyzerTagShape)
            .background(top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceContainerHighest)
            .then(
                if (onClick != null) {
                    Modifier
                        .pressable(interactionSource = interactionSource, indication = SinkFeedback())
                        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                } else Modifier
            )
            .padding(start = 7.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.icon_home_tab_analyse),
            contentDescription = "Analisador de Espaço",
            modifier = Modifier.size(14.dp),
            colorFilter = ColorFilter.tint(EsTextSecondary)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "$percentage%",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = EsTextPrimary
        )
    }
}

@Composable
fun EsPathBar(
    currentDir: File,
    onNavigate: (File) -> Unit,
    modifier: Modifier = Modifier,
    percentage: Int = 22,
    onAnalyzerClick: (() -> Unit)? = null,
    hoveredDropPath: String? = null,
    onSegmentBoundsChanged: ((String, Rect) -> Unit)? = null
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
                val isHoveredDrop = !isLast && hoveredDropPath == file.absolutePath
                val displayName = when {
                    index == 0 -> "/"
                    file.name.isEmpty() -> "/"
                    else -> file.name
                }

                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            if (!isLast) {
                                onSegmentBoundsChanged?.invoke(file.absolutePath, coords.boundsInWindow())
                            }
                        }
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isHoveredDrop) Color(0xFF3FE0F0).copy(alpha = 0.34f) else Color.Transparent)
                        .then(
                            if (isHoveredDrop) {
                                Modifier.border(1.5.dp, Color(0xFF3FE0F0), RoundedCornerShape(6.dp))
                            } else Modifier
                        )
                        .pressable(
                            interactionSource = interactionSource,
                            indication = SinkFeedback()
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = !isLast
                        ) { onNavigate(file) }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = displayName,
                        fontSize = 12.5.sp,
                        fontWeight = if (isLast || isHoveredDrop) FontWeight.Bold else FontWeight.Normal,
                        color = if (isLast || isHoveredDrop) EsTextPrimary else EsTextSecondary
                    )
                }

                if (!isLast) {
                    Text(
                        text = "›",
                        fontSize = 16.sp,
                        color = EsTextSecondary.copy(alpha = 0.72f),
                        modifier = Modifier.padding(horizontal = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 1:1 Right Pointed Gauge Tag (e.g. 21% -> opens Space Analyzer)
        EsAnalyzerPercentageTag(
            percentage = percentage,
            onClick = onAnalyzerClick
        )
    }
}
