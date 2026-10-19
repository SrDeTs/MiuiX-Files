package top.yukonga.miuix.files.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.files.R
import top.yukonga.miuix.files.core.model.FileCategory
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

data class TopCircleCategory(
    val title: String,
    val iconRes: Int,
    val category: FileCategory
)

data class ToolItem(
    val title: String,
    val iconRes: Int,
    val onClick: () -> Unit
)

@Composable
fun EsTopCategoryRow(
    onCategoryClick: (FileCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        TopCircleCategory("Fotografias", R.drawable.es_cat_photo, FileCategory.IMAGES),
        TopCircleCategory("Filmes", R.drawable.es_cat_movie, FileCategory.VIDEOS),
        TopCircleCategory("Livros", R.drawable.es_cat_doc, FileCategory.DOCUMENTS),
        TopCircleCategory("Música", R.drawable.es_cat_music, FileCategory.AUDIO),
        TopCircleCategory("APP", R.drawable.es_cat_app, FileCategory.APPS)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (item in items) {
            val interactionSource = remember { MutableInteractionSource() }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .pressable(
                        interactionSource = interactionSource,
                        indication = SinkFeedback()
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onCategoryClick(item.category) }
                    .padding(vertical = 2.dp)
            ) {
                Image(
                    painter = painterResource(item.iconRes),
                    contentDescription = item.title,
                    modifier = Modifier.size(52.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.title,
                    fontSize = 11.5.sp,
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
        ToolItem("Cleaner", R.drawable.es_tool_cleaner, onCleanerClick),
        ToolItem("Unid. de nu...", R.drawable.es_tool_cloud, {}),
        ToolItem("Arquivado...", R.drawable.es_tool_archive, {}),
        ToolItem("Remetente", R.drawable.es_tool_sender, {}),
        ToolItem("Editor de t...", R.drawable.es_tool_texteditor, {})
    )

    val toolsRow2 = listOf(
        ToolItem("Rede", R.drawable.es_tool_network, {}),
        ToolItem("Reciclagem", R.drawable.es_tool_recycle, onRecycleBinClick),
        ToolItem("Downloader", R.drawable.es_tool_downloader, onDownloadClick),
        ToolItem("Ver no PC", R.drawable.es_tool_viewpc, {}),
        ToolItem("Todas as F...", R.drawable.es_tool_alltools, onAppManagerClick)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        insideMargin = PaddingValues(vertical = 12.dp, horizontal = 4.dp),
        colors = CardDefaults.defaultColors(
            color = Color(0xFF23262B),
            contentColor = Color.White
        )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .pressable(
                interactionSource = interactionSource,
                indication = SinkFeedback()
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = tool.onClick
            )
            .padding(vertical = 2.dp, horizontal = 1.dp)
    ) {
        Image(
            painter = painterResource(tool.iconRes),
            contentDescription = tool.title,
            modifier = Modifier.size(42.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = tool.title,
            fontSize = 10.5.sp,
            color = Color(0xFFDDDDDD),
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}
