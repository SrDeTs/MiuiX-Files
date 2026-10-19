package top.yukonga.miuix.files.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.files.R
import top.yukonga.miuix.files.core.model.FileCategory
import top.yukonga.miuix.kmp.basic.Text

data class TopCircleCategory(
    val title: String,
    val iconRes: Int,
    val bgColor: Color,
    val category: FileCategory
)

data class ToolItem(
    val title: String,
    val iconRes: Int,
    val bgColor: Color,
    val onClick: () -> Unit
)

@Composable
fun EsTopCategoryRow(
    onCategoryClick: (FileCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        TopCircleCategory("Fotografias", R.drawable.icon_home_tab_image, Color(0xFFFBBF24), FileCategory.IMAGES),
        TopCircleCategory("Filmes", R.drawable.icon_home_tab_video, Color(0xFF06B6D4), FileCategory.VIDEOS),
        TopCircleCategory("Livros", R.drawable.icon_home_tab_document, Color(0xFF3B82F6), FileCategory.DOCUMENTS),
        TopCircleCategory("Música", R.drawable.icon_home_tab_music, Color(0xFFEC4899), FileCategory.AUDIO),
        TopCircleCategory("APP", R.drawable.icon_home_tab_app, Color(0xFFA855F7), FileCategory.APPS)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (item in items) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onCategoryClick(item.category) }
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(item.bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(item.iconRes),
                        contentDescription = item.title,
                        modifier = Modifier.size(28.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.title,
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun EsToolGrid(
    onCleanerClick: () -> Unit,
    onAppManagerClick: () -> Unit,
    onRecycleBinClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val toolsRow1 = listOf(
        ToolItem("Cleaner", R.drawable.icon_home_tab_clean, Color(0xFFBCAAA4), onCleanerClick),
        ToolItem("Unid. de nu...", R.drawable.icon_home_tab_cloud, Color(0xFF26C6DA), {}),
        ToolItem("Arquivado...", R.drawable.icon_home_tab_logger, Color(0xFF42A5F5), {}),
        ToolItem("Remetente", R.drawable.icon_home_tab_sender, Color(0xFF66BB6A), {}),
        ToolItem("Editor de t...", R.drawable.format_text, Color(0xFF00BCD4), {})
    )

    val toolsRow2 = listOf(
        ToolItem("Rede", R.drawable.icon_home_tab_device, Color(0xFF43A047), {}),
        ToolItem("Reciclagem", R.drawable.icon_home_tab_recyclebin, Color(0xFF7E57C2), onRecycleBinClick),
        ToolItem("Downloader", R.drawable.icon_home_tab_download, Color(0xFF3F51B5), onDownloadClick),
        ToolItem("Ver no PC", R.drawable.icon_home_tab_device, Color(0xFF009688), {}),
        ToolItem("Todas as F...", R.drawable.icon_home_tab_toolbox, Color(0xFF455A64), onAppManagerClick)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF25282F))
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (tool in toolsRow1) {
                    ToolItemCell(tool = tool, modifier = Modifier.weight(1f))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (tool in toolsRow2) {
                    ToolItemCell(tool = tool, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ToolItemCell(
    tool: ToolItem,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = tool.onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tool.bgColor),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(tool.iconRes),
                contentDescription = tool.title,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = tool.title,
            fontSize = 11.sp,
            color = Color(0xFFD0D2D8),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
