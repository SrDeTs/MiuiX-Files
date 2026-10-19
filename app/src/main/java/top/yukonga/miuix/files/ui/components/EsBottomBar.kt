package top.yukonga.miuix.files.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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

@Composable
fun EsBottomBar(
    onNewClick: () -> Unit,
    onSearchClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onWindowsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF191B20))
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EsBottomBarItem(
                title = "Novo",
                iconRes = R.drawable.toolbar_new,
                onClick = onNewClick
            )
            EsBottomBarItem(
                title = "Procurar",
                iconRes = R.drawable.toolbar_search,
                onClick = onSearchClick
            )
            EsBottomBarItem(
                title = "Atualizar",
                iconRes = R.drawable.toolbar_refresh,
                onClick = onRefreshClick
            )
            EsBottomBarItem(
                title = "Janelas",
                iconRes = R.drawable.toolbar_windows,
                onClick = onWindowsClick
            )
            EsBottomBarItem(
                title = "Histórico",
                iconRes = R.drawable.toolbar_history,
                onClick = onHistoryClick
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 12.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(Color(0xFFE0E0E0))
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            color = Color(0xFFB0B3B8),
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}
