package top.yukonga.miuix.files.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.yukonga.miuix.files.R
import top.yukonga.miuix.files.core.model.FileCategory
import top.yukonga.miuix.files.core.model.FileItem
import top.yukonga.miuix.files.core.storage.StorageManager
import top.yukonga.miuix.files.ui.components.FileGridItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable
import java.io.File

@Composable
fun CategoryScreen(
    category: FileCategory,
    onNavigateBack: () -> Unit,
    onOpenFile: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(category) {
        isLoading = true
        files = StorageManager.getCategoryFiles(category)
        isLoading = false
    }

    val catIcon = when (category) {
        FileCategory.IMAGES -> R.drawable.es_cat_photo
        FileCategory.VIDEOS -> R.drawable.es_cat_movie
        FileCategory.DOCUMENTS -> R.drawable.es_cat_doc
        FileCategory.AUDIO -> R.drawable.es_cat_music
        FileCategory.APPS -> R.drawable.es_cat_app
        FileCategory.ARCHIVES -> R.drawable.format_zip
        FileCategory.DOWNLOADS -> R.drawable.es_tool_downloader
        FileCategory.RECYCLE_BIN -> R.drawable.es_tool_recycle
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF292C2E))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1:1 Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF31353D))
                    .statusBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Menu (☰)
                val menuInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .pressable(
                            interactionSource = menuInteraction,
                            indication = SinkFeedback()
                        )
                        .clickable(
                            interactionSource = menuInteraction,
                            indication = null,
                            onClick = onNavigateBack
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.toolbar_menu),
                        contentDescription = "Menu",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Small Home Icon
                val homeInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .pressable(
                            interactionSource = homeInteraction,
                            indication = SinkFeedback()
                        )
                        .clickable(
                            interactionSource = homeInteraction,
                            indication = null,
                            onClick = onNavigateBack
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.icon_home_tab_home),
                        contentDescription = "Início",
                        modifier = Modifier.size(16.dp),
                        colorFilter = ColorFilter.tint(Color(0xFFB0B3B8))
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Tab pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF464A51))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(catIcon),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = category.label,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✕", fontSize = 11.sp, color = Color(0xFFB0B0B0))
                    }
                }
            }

            // Path Header Bar with Gauge Chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF292C2E))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${category.label} (${files.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Right Gauge Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF383D48))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.toolbar_diskanalyzer_nomal),
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        colorFilter = ColorFilter.tint(Color(0xFFB0B3B8))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "22%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Main Content Grid
            Box(modifier = Modifier.weight(1f)) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Buscando arquivos...",
                            fontSize = 13.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                } else if (files.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Pasta Vazia",
                            fontSize = 14.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(files, key = { it.path }) { item ->
                            FileGridItem(
                                item = item,
                                isSelected = false,
                                isSelectionMode = false,
                                onClick = { onOpenFile(item.file) },
                                onLongClick = {}
                            )
                        }
                    }
                }
            }

            // Bottom Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF292C2E))
                    .navigationBarsPadding()
            ) {
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
                    CategoryBottomItem("Novo", R.drawable.toolbar_new, modifier = Modifier.weight(1f)) {}
                    CategoryBottomItem("Procurar", R.drawable.toolbar_search, modifier = Modifier.weight(1f)) {}
                    CategoryBottomItem("Atualizar", R.drawable.toolbar_refresh, modifier = Modifier.weight(1f)) {
                        scope.launch {
                            isLoading = true
                            files = StorageManager.getCategoryFiles(category)
                            isLoading = false
                        }
                    }
                    CategoryBottomItem("Visualizar", R.drawable.toolbar_view, modifier = Modifier.weight(1f)) {}
                    CategoryBottomItem("Mais", R.drawable.toolbar_more, modifier = Modifier.weight(1f)) {}
                }
            }
        }
    }
}

@Composable
private fun CategoryBottomItem(
    title: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
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
            fontWeight = FontWeight.Normal
        )
    }
}
