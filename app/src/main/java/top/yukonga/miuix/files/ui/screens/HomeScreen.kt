package top.yukonga.miuix.files.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.yukonga.miuix.files.core.model.FileCategory
import top.yukonga.miuix.files.core.model.FileItem
import top.yukonga.miuix.files.core.model.StorageVolumeInfo
import top.yukonga.miuix.files.core.storage.StorageManager
import top.yukonga.miuix.files.ui.components.CategoryGrid
import top.yukonga.miuix.files.ui.components.FileListItem
import top.yukonga.miuix.files.ui.components.StorageCard
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

@Composable
fun HomeScreen(
    onNavigateDir: (File) -> Unit,
    onNavigateCategory: (FileCategory) -> Unit,
    onNavigateCleaner: () -> Unit,
    onNavigateAppManager: () -> Unit,
    onNavigateSettings: () -> Unit,
    onOpenFile: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var volumes by remember { mutableStateOf<List<StorageVolumeInfo>>(emptyList()) }
    var recentFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoadingRecent by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        volumes = StorageManager.getStorageVolumes(context)
        recentFiles = StorageManager.getRecentFiles()
        isLoadingRecent = false
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = "MiuiX Files",
            actions = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onNavigateSettings)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⚙️",
                        fontSize = 18.sp
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Cards (Internal & SD)
            items(volumes) { vol ->
                StorageCard(
                    volume = vol,
                    onClick = { onNavigateDir(vol.path) }
                )
            }

            // Category Grid (Iconic ES File Explorer 8 shortcuts)
            item {
                SmallTitle(text = "Categorias")
                Spacer(modifier = Modifier.height(6.dp))
                CategoryGrid(
                    onCategoryClick = onNavigateCategory,
                    onCleanerClick = onNavigateCleaner
                )
            }

            // Quick Tools (App Manager, Trash)
            item {
                SmallTitle(text = "Ferramentas")
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onNavigateAppManager)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📦", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Apps",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Gerenciar / Backup",
                                    fontSize = 11.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onNavigateCleaner)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🧹", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Limpeza",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Liberar espaço",
                                    fontSize = 11.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                    }
                }
            }

            // Recent Files
            item {
                SmallTitle(text = "Arquivos Recentes")
            }

            if (isLoadingRecent) {
                item {
                    Text(
                        text = "Carregando recentes...",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            } else if (recentFiles.isEmpty()) {
                item {
                    Text(
                        text = "Nenhum arquivo recente encontrado.",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            } else {
                items(recentFiles) { item ->
                    FileListItem(
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
}
