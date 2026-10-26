package top.yukonga.miuix.files.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.yukonga.miuix.files.R
import top.yukonga.miuix.files.core.model.FileCategory
import top.yukonga.miuix.files.core.model.FileItem
import top.yukonga.miuix.files.core.model.SortOption
import top.yukonga.miuix.files.core.storage.FileOperations
import top.yukonga.miuix.files.core.storage.StorageManager
import top.yukonga.miuix.files.ui.components.EsAnalyzerPercentageTag
import top.yukonga.miuix.files.ui.components.EsApkPropertiesDialog
import top.yukonga.miuix.files.ui.components.EsBgColor
import top.yukonga.miuix.files.ui.components.EsClipboardSideTab
import top.yukonga.miuix.files.ui.components.EsDividerColor
import top.yukonga.miuix.files.ui.components.EsTextPrimary
import top.yukonga.miuix.files.ui.components.EsTextSecondary
import top.yukonga.miuix.files.ui.components.EsTopHeader
import top.yukonga.miuix.files.ui.components.EsWindowTab
import top.yukonga.miuix.files.ui.components.FileGridItem
import top.yukonga.miuix.files.ui.components.FileListItem
import top.yukonga.miuix.files.ui.components.InlineTransferProgressStrip
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.pressable
import java.io.File

@Composable
fun CategoryScreen(
    category: FileCategory,
    onNavigateBack: () -> Unit,
    onOpenFile: (File) -> Unit,
    onMenuClick: () -> Unit = {},
    tabs: List<EsWindowTab> = emptyList(),
    activeTabId: String = "",
    onTabSelect: (EsWindowTab) -> Unit = {},
    onTabClose: (EsWindowTab) -> Unit = {},
    onAddTab: (() -> Unit)? = null,
    onAnalyzerClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val cached = remember(category) { StorageManager.getCachedCategory(category) }
    var files by remember(category) { mutableStateOf(cached ?: emptyList()) }
    var isLoading by remember(category) { mutableStateOf(cached == null) }
    var isGridView by remember { mutableStateOf(true) }
    var viewGridColumns by remember { mutableIntStateOf(4) }
    var showViewMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var currentSort by remember { mutableStateOf(SortOption.DATE_DESC) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val displayFiles = remember(files, searchQuery, currentSort) {
        val filtered = if (searchQuery.isBlank()) files else files.filter { it.name.contains(searchQuery, ignoreCase = true) }
        when (currentSort) {
            SortOption.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            SortOption.SIZE_DESC -> filtered.sortedByDescending { it.size }
            else -> filtered.sortedByDescending { it.lastModified }
        }
    }
    var selectedPaths by remember(category) { mutableStateOf<Set<String>>(emptySet()) }
    val isSelectionMode = selectedPaths.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var tappedApkFile by remember { mutableStateOf<File?>(null) }

    androidx.activity.compose.BackHandler(enabled = isSelectionMode) {
        selectedPaths = emptySet()
    }

    LaunchedEffect(category) {
        if (cached == null) isLoading = true
        files = StorageManager.getCategoryFiles(category, context = context)
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
            .background(EsBgColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1:1 Top Bar with multi-tab switching and global drawer menu (☰)
            val displayTabs = remember(tabs, activeTabId, isSelectionMode, selectedPaths.size) {
                if (tabs.isEmpty()) emptyList()
                else tabs.map { t ->
                    if (t.id == activeTabId && isSelectionMode) {
                        t.copy(title = "${selectedPaths.size} selecionados")
                    } else t
                }
            }
            EsTopHeader(
                tabTitle = if (isSelectionMode) "${selectedPaths.size} selecionados" else category.label,
                onMenuClick = {
                    if (isSelectionMode) selectedPaths = emptySet()
                    else onMenuClick()
                },
                onTabCloseClick = onNavigateBack,
                menuIconRes = if (isSelectionMode) R.drawable.toolbar_cancel else R.drawable.toolbar_menu,
                tabs = displayTabs,
                activeTabId = activeTabId,
                onTabSelect = onTabSelect,
                onTabClose = onTabClose,
                onAddTab = onAddTab,
                isSelectionMode = isSelectionMode,
                selectedCount = selectedPaths.size,
                totalCount = displayFiles.size,
                onSelectAll = {
                    selectedPaths = if (selectedPaths.size == displayFiles.size && displayFiles.isNotEmpty()) {
                        emptySet()
                    } else {
                        displayFiles.map { it.path }.toSet()
                    }
                }
            )

            // Path Header Bar with Pointed Gauge Tag
            val extRoot = remember { android.os.Environment.getExternalStorageDirectory() }
            val usedPct = remember(extRoot) {
                val total = extRoot.totalSpace
                if (total > 0) (((total - extRoot.freeSpace) * 100L) / total).toInt().coerceIn(0, 100) else 22
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EsBgColor)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${category.label} (${files.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EsTextPrimary
                )

                EsAnalyzerPercentageTag(
                    percentage = usedPct,
                    onClick = onAnalyzerClick
                )
            }

            // Inline Copy / Cut Progress Strip
            InlineTransferProgressStrip(state = FileOperations.transferState)

            AnimatedVisibility(
                visible = showSearchBar,
                enter = expandVertically(animationSpec = folmeSpring(damping = 0.85f, response = 0.28f)) + fadeIn(),
                exit = shrinkVertically(animationSpec = folmeSpring(damping = 0.9f, response = 0.22f)) + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = "Filtrar em ${category.label}...",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(text = "✕", onClick = { searchQuery = ""; showSearchBar = false })
                }
            }

            // Main Content Grid or List with MIUIX Folme spring transitions
            Box(modifier = Modifier.weight(1f)) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Buscando arquivos...",
                            fontSize = 13.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                } else if (displayFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Pasta Vazia",
                            fontSize = 14.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                } else {
                    AnimatedContent(
                        targetState = isGridView,
                        transitionSpec = {
                            val toGrid = targetState
                            val inScale = if (toGrid) 0.90f else 1.08f
                            val outScale = if (toGrid) 1.06f else 0.92f
                            (scaleIn(
                                initialScale = inScale,
                                animationSpec = folmeSpring(damping = 0.82f, response = 0.32f)
                            ) + fadeIn(
                                animationSpec = folmeSpring(damping = 0.9f, response = 0.24f)
                            )) togetherWith (scaleOut(
                                targetScale = outScale,
                                animationSpec = folmeSpring(damping = 0.9f, response = 0.22f)
                            ) + fadeOut(
                                animationSpec = folmeSpring(damping = 0.9f, response = 0.18f)
                            ))
                        },
                        label = "category_view_mode_transition"
                    ) { gridMode ->
                        if (gridMode) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(viewGridColumns),
                                modifier = Modifier.fillMaxSize().overScrollVertical(),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(displayFiles, key = { it.path }) { item ->
                                    val isSelected = selectedPaths.contains(item.path)
                                    FileGridItem(
                                        item = item,
                                        isSelected = isSelected,
                                        isSelectionMode = isSelectionMode,
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = folmeSpring(damping = 0.9f, response = 0.25f),
                                            placementSpec = folmeSpring(damping = 0.84f, response = 0.30f, visibilityThreshold = IntOffset(1, 1)),
                                            fadeOutSpec = folmeSpring(damping = 0.9f, response = 0.20f)
                                        ),
                                        onClick = {
                                            if (isSelectionMode) {
                                                selectedPaths = if (isSelected) selectedPaths - item.path else selectedPaths + item.path
                                            } else if (item.extension == "apk") {
                                                tappedApkFile = item.file
                                            } else {
                                                onOpenFile(item.file)
                                            }
                                        },
                                        onLongClick = {
                                            if (!isSelectionMode) selectedPaths = setOf(item.path)
                                        }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().overScrollVertical(),
                                contentPadding = PaddingValues(bottom = 12.dp)
                            ) {
                                items(displayFiles, key = { it.path }) { item ->
                                    val isSelected = selectedPaths.contains(item.path)
                                    FileListItem(
                                        item = item,
                                        isSelected = isSelected,
                                        isSelectionMode = isSelectionMode,
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = folmeSpring(damping = 0.9f, response = 0.25f),
                                            placementSpec = folmeSpring(damping = 0.84f, response = 0.30f, visibilityThreshold = IntOffset(1, 1)),
                                            fadeOutSpec = folmeSpring(damping = 0.9f, response = 0.20f)
                                        ),
                                        onClick = {
                                            if (isSelectionMode) {
                                                selectedPaths = if (isSelected) selectedPaths - item.path else selectedPaths + item.path
                                            } else if (item.extension == "apk") {
                                                tappedApkFile = item.file
                                            } else {
                                                onOpenFile(item.file)
                                            }
                                        },
                                        onLongClick = {
                                            if (!isSelectionMode) selectedPaths = setOf(item.path)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                EsClipboardSideTab(
                    clipboard = if (!isSelectionMode) FileOperations.currentClipboard else null,
                    onPaste = {},
                    onClear = { FileOperations.currentClipboard = null },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(top = 60.dp)
                )
            }

            // Bottom Bar
            if (isSelectionMode) {
                top.yukonga.miuix.files.ui.components.FileActionBottomBar(
                    selectedCount = selectedPaths.size,
                    restoreMode = category == FileCategory.RECYCLE_BIN,
                    onCopy = {
                        val sel = files.filter { selectedPaths.contains(it.path) }.map { it.file }
                        top.yukonga.miuix.files.core.storage.FileOperations.currentClipboard =
                            top.yukonga.miuix.files.core.storage.FileOperations.ClipboardItem(sel, top.yukonga.miuix.files.core.storage.FileOperations.ClipboardMode.COPY)
                        selectedPaths = emptySet()
                    },
                    onCut = {
                        val sel = files.filter { selectedPaths.contains(it.path) }.map { it.file }
                        top.yukonga.miuix.files.core.storage.FileOperations.currentClipboard =
                            top.yukonga.miuix.files.core.storage.FileOperations.ClipboardItem(sel, top.yukonga.miuix.files.core.storage.FileOperations.ClipboardMode.CUT)
                        selectedPaths = emptySet()
                    },
                    onDelete = { showDeleteDialog = true },
                    onRename = {
                        if (category == FileCategory.RECYCLE_BIN) {
                            val sel = files.filter { selectedPaths.contains(it.path) }.map { it.file }
                            scope.launch {
                                top.yukonga.miuix.files.core.storage.FileOperations.restoreFromRecycle(sel)
                                selectedPaths = emptySet()
                                files = StorageManager.getCategoryFiles(category, context = context, forceRefresh = true)
                            }
                        }
                    },
                    onMore = {
                        val first = files.firstOrNull { selectedPaths.contains(it.path) }?.file
                        if (first != null) {
                            runCatching {
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", first)
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "*/*"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Compartilhar ${first.name}"))
                            }
                        }
                        selectedPaths = emptySet()
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EsBgColor)
                        .navigationBarsPadding()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(EsDividerColor)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryBottomItem(if (category == FileCategory.RECYCLE_BIN) "Esvaziar" else "Novo", if (category == FileCategory.RECYCLE_BIN) R.drawable.toolbar_delete else R.drawable.toolbar_new, modifier = Modifier.weight(1f)) {
                            if (category == FileCategory.RECYCLE_BIN) {
                                scope.launch {
                                    top.yukonga.miuix.files.core.storage.FileOperations.emptyRecycle()
                                    files = StorageManager.getCategoryFiles(category, context = context, forceRefresh = true)
                                }
                            } else showMoreMenu = true
                        }
                        CategoryBottomItem("Procurar", R.drawable.toolbar_search, modifier = Modifier.weight(1f)) {
                            showSearchBar = !showSearchBar
                            if (!showSearchBar) searchQuery = ""
                        }
                        CategoryBottomItem("Atualizar", R.drawable.toolbar_refresh, modifier = Modifier.weight(1f)) {
                            scope.launch {
                                isLoading = true
                                files = StorageManager.getCategoryFiles(category, context = context, forceRefresh = true)
                                isLoading = false
                            }
                        }
                        CategoryBottomItem("Visualizar", R.drawable.toolbar_view, modifier = Modifier.weight(1f)) {
                            showViewMenu = true
                        }
                        CategoryBottomItem("Mais", R.drawable.toolbar_more, modifier = Modifier.weight(1f)) {
                            showMoreMenu = true
                        }
                    }
                }
            }
        }
    }

    if (showViewMenu) {
        OverlayDialog(
            show = showViewMenu,
            onDismissRequest = { showViewMenu = false },
            title = "Visualizar",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Modo de Exibição", fontSize = 12.sp, color = EsTextSecondary, fontWeight = FontWeight.Bold)
                    TextButton(
                        text = if (isGridView && viewGridColumns == 4) "✓ Ícone Grande (4 colunas)" else "Ícone Grande (4 colunas)",
                        onClick = { isGridView = true; viewGridColumns = 4; showViewMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        text = if (isGridView && viewGridColumns == 5) "✓ Ícone Médio (5 colunas)" else "Ícone Médio (5 colunas)",
                        onClick = { isGridView = true; viewGridColumns = 5; showViewMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        text = if (!isGridView) "✓ Lista Detalhada" else "Lista Detalhada",
                        onClick = { isGridView = false; showViewMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        text = "Fechar",
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = { showViewMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    if (showDeleteDialog) {
        val isRecycleCategory = category == FileCategory.RECYCLE_BIN
        var moveToRecycleBin by remember(isRecycleCategory) { mutableStateOf(!isRecycleCategory) }
        OverlayDialog(
            show = showDeleteDialog,
            onDismissRequest = { showDeleteDialog = false },
            title = "Excluir (${selectedPaths.size})",
            summary = if (moveToRecycleBin && !isRecycleCategory) {
                "Mover ${selectedPaths.size} arquivo(s) para a Reciclagem?"
            } else {
                "Tem certeza de que deseja excluir permanentemente ${selectedPaths.size} arquivo(s)?"
            },
            content = {
                if (!isRecycleCategory) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { moveToRecycleBin = !moveToRecycleBin }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            state = if (moveToRecycleBin) androidx.compose.ui.state.ToggleableState.On else androidx.compose.ui.state.ToggleableState.Off,
                            onClick = { moveToRecycleBin = !moveToRecycleBin }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mover para a Reciclagem",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = EsTextPrimary
                            )
                            Text(
                                text = if (moveToRecycleBin) "Pode ser restaurado depois" else "Desmarcado: exclusão direta e permanente",
                                fontSize = 11.5.sp,
                                color = if (moveToRecycleBin) EsTextSecondary else Color(0xFFFF6B6B)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        text = "Cancelar",
                        onClick = { showDeleteDialog = false },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        text = if (moveToRecycleBin && !isRecycleCategory) "Mover" else "Excluir",
                        colors = ButtonDefaults.textButtonColorsPrimary(
                            color = if (moveToRecycleBin && !isRecycleCategory) Color(0xFF2F7DF6) else Color(0xFFE53935),
                            textColor = Color.White
                        ),
                        onClick = {
                            val toDel = files.filter { selectedPaths.contains(it.path) }.map { it.file }
                            val useRecycle = moveToRecycleBin && !isRecycleCategory
                            showDeleteDialog = false
                            scope.launch {
                                if (useRecycle) {
                                    top.yukonga.miuix.files.core.storage.FileOperations.moveToRecycle(toDel)
                                } else {
                                    top.yukonga.miuix.files.core.storage.FileOperations.deleteFiles(toDel)
                                }
                                selectedPaths = emptySet()
                                files = StorageManager.getCategoryFiles(category, context = context, forceRefresh = true)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        )
    }

    if (tappedApkFile != null) {
        EsApkPropertiesDialog(
            apkFile = tappedApkFile!!,
            onDismiss = { tappedApkFile = null },
            onViewContent = { file ->
                tappedApkFile = null
                onOpenFile(file)
            }
        )
    }

    if (showMoreMenu) {
        OverlayDialog(
            show = showMoreMenu,
            onDismissRequest = { showMoreMenu = false },
            title = "Ordenar ${category.label}",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        text = "Ordenar por Data (Mais recente)",
                        onClick = { currentSort = SortOption.DATE_DESC; showMoreMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        text = "Ordenar por Nome (A-Z)",
                        onClick = { currentSort = SortOption.NAME_ASC; showMoreMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        text = "Ordenar por Nome (Z-A)",
                        onClick = { currentSort = SortOption.NAME_DESC; showMoreMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        text = "Ordenar por Tamanho (Maior)",
                        onClick = { currentSort = SortOption.SIZE_DESC; showMoreMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        text = "Fechar",
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = { showMoreMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
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
            colorFilter = ColorFilter.tint(EsTextPrimary.copy(alpha = 0.88f))
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 10.5.sp,
            color = EsTextSecondary,
            fontWeight = FontWeight.Normal
        )
    }
}
