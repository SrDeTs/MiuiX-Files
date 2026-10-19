package top.yukonga.miuix.files.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.files.R
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

@Composable
fun EsBottomBar(
    onNewClick: () -> Unit,
    onSearchClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onWindowsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF292C2E))
            .navigationBarsPadding()
    ) {
        // Subtle 1px top border matching ES File Explorer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF383C42))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EsBottomBarItem(
                title = "Novo",
                iconRes = R.drawable.toolbar_new,
                onClick = onNewClick,
                modifier = Modifier.weight(1f)
            )
            EsBottomBarItem(
                title = "Procurar",
                iconRes = R.drawable.toolbar_search,
                onClick = onSearchClick,
                modifier = Modifier.weight(1f)
            )
            EsBottomBarItem(
                title = "Atualizar",
                iconRes = R.drawable.toolbar_refresh,
                onClick = onRefreshClick,
                modifier = Modifier.weight(1f)
            )
            EsBottomBarItem(
                title = "Janelas",
                iconRes = R.drawable.toolbar_windows,
                onClick = onWindowsClick,
                modifier = Modifier.weight(1f)
            )
            EsBottomBarItem(
                title = "Histórico",
                iconRes = R.drawable.toolbar_history,
                onClick = onHistoryClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EsBottomBarItem(
    title: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .pressable(
                interactionSource = interactionSource,
                indication = SinkFeedback()
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = title,
            modifier = Modifier.size(23.dp),
            colorFilter = ColorFilter.tint(Color(0xFFE0E0E0))
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 10.5.sp,
            color = Color(0xFFB0B3B8),
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}
