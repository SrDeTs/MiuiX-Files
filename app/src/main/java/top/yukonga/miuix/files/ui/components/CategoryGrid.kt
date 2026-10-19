package top.yukonga.miuix.files.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.files.core.model.FileCategory
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class CategoryItemUi(
    val title: String,
    val iconEmoji: String,
    val iconBgColor: Color,
    val category: FileCategory?,
    val isCleaner: Boolean = false
)

@Composable
fun CategoryGrid(
    onCategoryClick: (FileCategory) -> Unit,
    onCleanerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        CategoryItemUi("Imagens", "🖼️", Color(0xFF00B4D8), FileCategory.IMAGES),
        CategoryItemUi("Vídeos", "🎬", Color(0xFFFF5964), FileCategory.VIDEOS),
        CategoryItemUi("Músicas", "🎵", Color(0xFF9D4EDD), FileCategory.AUDIO),
        CategoryItemUi("Documentos", "📄", Color(0xFFFF9E00), FileCategory.DOCUMENTS),
        CategoryItemUi("Apps", "📱", Color(0xFF2EC4B6), FileCategory.APPS),
        CategoryItemUi("Compactados", "🗜️", Color(0xFF3A86FF), FileCategory.ARCHIVES),
        CategoryItemUi("Downloads", "📥", Color(0xFF4361EE), FileCategory.DOWNLOADS),
        CategoryItemUi("Limpeza", "🧹", Color(0xFFFF0054), null, isCleaner = true)
    )

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val chunked = items.chunked(4)
            for (row in chunked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (item in row) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (item.isCleaner) onCleanerClick()
                                    else item.category?.let { onCategoryClick(it) }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(item.iconBgColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.iconEmoji,
                                    fontSize = 22.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
