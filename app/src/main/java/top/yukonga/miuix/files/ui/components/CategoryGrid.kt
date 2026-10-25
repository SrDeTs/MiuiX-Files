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
            .padding(horizontal = 5.dp),
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
                    .padding(top = 6.dp, bottom = 10.dp)
            ) {
                Image(
                    painter = painterResource(item.iconRes),
                    contentDescription = item.title,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    color = EsTextPrimary.copy(alpha = 0.88f),
                    fontWeight = FontWeight.Bold,
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
    onCloudClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onSenderClick: () -> Unit = {},
    onTextEditorClick: () -> Unit = {},
    onNetworkClick: () -> Unit = {},
    onRecycleBinClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onViewPcClick: () -> Unit = {},
    onAppManagerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val toolsRow1 = listOf(
        ToolItem("Cleaner", R.drawable.es_tool_cleaner, onCleanerClick),
        ToolItem("Unid. de nu...", R.drawable.es_tool_cloud, onCloudClick),
        ToolItem("Arquivado...", R.drawable.es_tool_archive, onArchiveClick),
        ToolItem("Remetente", R.drawable.es_tool_sender, onSenderClick),
        ToolItem("Editor de t...", R.drawable.es_tool_texteditor, onTextEditorClick)
    )

    val toolsRow2 = listOf(
        ToolItem("Rede", R.drawable.es_tool_network, onNetworkClick),
        ToolItem("Reciclagem", R.drawable.es_tool_recycle, onRecycleBinClick),
        ToolItem("Downloader", R.drawable.es_tool_downloader, onDownloadClick),
        ToolItem("Ver no PC", R.drawable.es_tool_viewpc, onViewPcClick),
        ToolItem("Todas as F...", R.drawable.es_tool_alltools, onAppManagerClick)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 10.dp,
        insideMargin = PaddingValues(vertical = 12.dp, horizontal = 2.dp),
        colors = CardDefaults.defaultColors(
            color = EsCardColor,
            contentColor = EsTextPrimary
        )
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
        Box(
            modifier = Modifier.size(34.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(tool.iconRes),
                contentDescription = tool.title,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = tool.title,
            fontSize = 12.sp,
            color = EsTextPrimary.copy(alpha = 0.85f),
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
