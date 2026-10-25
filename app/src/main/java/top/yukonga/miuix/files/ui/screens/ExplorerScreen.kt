package top.yukonga.miuix.files.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.yukonga.miuix.files.R
import top.yukonga.miuix.files.core.model.FileItem
import top.yukonga.miuix.files.core.model.SortOption
import top.yukonga.miuix.files.core.storage.FileOperations
import top.yukonga.miuix.files.core.storage.StorageManager
import top.yukonga.miuix.files.ui.components.*
import top.yukonga.miuix.files.ui.theme.MiuiXFilesPreferences
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.pressable
import java.io.File

@Composable
fun ExplorerScreen(
    initialDir: File,
    onNavigateBack: () -> Unit,
    onOpenFile: (File) -> Unit,
    onMenuClick: () -> Unit = {},
    tabs: List<EsWindowTab> = emptyList(),
    activeTabId: String = "",
    onTabSelect: (EsWindowTab) -> Unit = {},
    onTabClose: (EsWindowTab) -> Unit = {},
    onAddTab: (() -> Unit)? = null,
    onCurrentDirChange: (File) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { context.getSharedPreferences(MiuiXFilesPreferences.PREFS_NAME, Context.MODE_PRIVATE) }

    var currentDir by remember(initialDir) { mutableStateOf(initialDir) }
    var files by remember(initialDir) { mutableStateOf(StorageManager.getCachedDir(initialDir) ?: emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showHidden by remember { mutableStateOf(prefs.getBoolean(MiuiXFilesPreferences.KEY_SHOW_HIDDEN, false)) }
    val savedSortIndex = remember(prefs) { prefs.getInt(MiuiXFilesPreferences.KEY_SORT_OPTION, 0) }
    val initialSort = remember(savedSortIndex) {
        when (savedSortIndex) {
            1 -> SortOption.NAME_DESC
            2 -> SortOption.DATE_DESC
            3 -> SortOption.SIZE_DESC
            4 -> SortOption.TYPE_ASC
            else -> SortOption.NAME_ASC
        }
    }
    var currentSort by remember { mutableStateOf(initialSort) }
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    val defaultViewMode = remember(prefs) { prefs.getInt(MiuiXFilesPreferences.KEY_DEFAULT_VIEW_MODE, 0) }
    var isGridView by remember { mutableStateOf(defaultViewMode != 2) } // 0: 4-col Grid, 1: 5-col Compact Grid, 2: Detailed List
    val gridColumnCount = if (defaultViewMode == 1) 5 else 4
    val foldersFirst = remember(prefs) { prefs.getBoolean(MiuiXFilesPreferences.KEY_FOLDERS_FIRST, true) }
    val pinMediaFolders = remember(prefs) { prefs.getBoolean(MiuiXFilesPreferences.KEY_PIN_MEDIA_FOLDERS, true) }
    val isSelectionMode = selectedPaths.isNotEmpty()

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameFileName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showSelectionMoreDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    var tappedApkFile by remember { mutableStateOf<File?>(null) }

    val clipboardState = FileOperations.currentClipboard
    val transferState = FileOperations.transferState
    var navForward by remember { mutableStateOf(true) }

    fun navigateToDir(target: File) {
        navForward = target.absolutePath.length >= currentDir.absolutePath.length
        currentDir = target
        onCurrentDirChange(target)
    }

    fun refreshFiles() {
        StorageManager.getCachedDir(currentDir)?.let {
            if (!showHidden && currentSort == SortOption.NAME_ASC) files = it
        }
        scope.launch {
            files = StorageManager.listFiles(currentDir, showHidden = showHidden, sortOption = currentSort)
            selectedPaths = emptySet()
        }
    }

    LaunchedEffect(currentDir, showHidden, currentSort) {
        refreshFiles()
    }

    BackHandler {
        if (isSelectionMode) {
            selectedPaths = emptySet()
        } else {
            val parent = currentDir.parentFile
            if (parent != null && parent.canRead()) {
                navigateToDir(parent)
            } else {
                onNavigateBack()
            }
        }
    }

    val displayFiles = remember(files, searchQuery, currentDir, currentSort, foldersFirst, pinMediaFolders) {
        val base = if (searchQuery.isBlank()) files
        else files.filter { it.name.contains(searchQuery, ignoreCase = true) }

        if (pinMediaFolders && currentSort == SortOption.NAME_ASC && (currentDir.name == "0" || currentDir.absolutePath == "/sdcard")) {
            val pinned = listOf("dcim", "download")
            val dirs = base.filter { it.isDirectory }
            val nonDirs = base.filter { !it.isDirectory }
            val pinnedDirs = dirs.filter { it.name.lowercase() in pinned }
                .sortedBy { pinned.indexOf(it.name.lowercase()) }
            val otherDirs = dirs.filter { it.name.lowercase() !in pinned }
            pinnedDirs + otherDirs + nonDirs
        } else if (!foldersFirst) {
            base.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        } else {
            base
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EsBgColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1:1 Top Bar for Explorer with multi-tab switching and global drawer menu (☰)
            val displayTabs = remember(tabs, activeTabId, isSelectionMode, selectedPaths.size, currentDir) {
                if (tabs.isEmpty()) emptyList()
                else tabs.map { t ->
                    if (t.id == activeTabId && isSelectionMode) {
                        t.copy(title = "${selectedPaths.size} selecionados")
                    } else t
                }
            }
            EsTopHeader(
                tabTitle = if (isSelectionMode) "${selectedPaths.size} selecionados" else "Local",
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
                onAddTab = onAddTab
            )

            // Path Breadcrumb Bar with Right Gauge Chip
            val totalSpace = currentDir.totalSpace
            val usedPct = if (totalSpace > 0) (((totalSpace - currentDir.freeSpace) * 100L) / totalSpace).toInt().coerceIn(0, 100) else 22
            EsPathBar(
                currentDir = currentDir,
                onNavigate = { target -> navigateToDir(target) },
                percentage = usedPct
            )

            // Inline Copy / Cut Progress Strip (never blocks the full screen)
            InlineTransferProgressStrip(state = transferState)

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
                        label = "Filtrar nesta pasta...",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        text = "✕",
                        onClick = {
                            searchQuery = ""
                            showSearchBar = false
                        }
                    )
                }
            }

            // Content Area (Grid or List) with MIUIX Folme spring transitions and overscroll
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = Pair(currentDir.absolutePath, isGridView),
                    transitionSpec = {
                        val dirChanged = initialState.first != targetState.first
                        if (dirChanged) {
                            val sign = if (navForward) 1 else -1
                            (slideInHorizontally(animationSpec = folmeSpring(damping = 0.88f, response = 0.30f)) { (it * 0.22f * sign).toInt() } +
                                fadeIn(animationSpec = folmeSpring(damping = 0.9f, response = 0.25f))) togetherWith
                                (slideOutHorizontally(animationSpec = folmeSpring(damping = 0.9f, response = 0.24f)) { (-it * 0.14f * sign).toInt() } +
                                    fadeOut(animationSpec = folmeSpring(damping = 0.9f, response = 0.2f)))
                        } else {
                            // Grid <-> List view switch: authentic MIUIX Folme spring zoom + fade transition
                            val toGrid = targetState.second
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
                        }
                    },
                    label = "explorer_dir_transition"
                ) { (_, gridMode) ->
                    if (displayFiles.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Pasta Vazia",
                                color = Color(0xFF9E9E9E),
                                fontSize = 14.sp
                            )
                        }
                    } else if (gridMode) {
                        // 1:1 4-Column (or 5-Column Compact) Grid View with MIUIX overScrollVertical
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumnCount),
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
                                        } else {
                                            if (item.isDirectory) navigateToDir(item.file)
                                            else if (item.extension == "apk") tappedApkFile = item.file
                                            else onOpenFile(item.file)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) selectedPaths = setOf(item.path)
                                    }
                                )
                            }
                        }
                    } else {
                        // List View with MIUIX overScrollVertical
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
                                        } else {
                                            if (item.isDirectory) navigateToDir(item.file)
                                            else if (item.extension == "apk") tappedApkFile = item.file
                                            else onOpenFile(item.file)
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

                // Right-edge semi-circular Clipboard Tab (1:1 ES File Explorer)
                EsClipboardSideTab(
                    clipboard = if (!isSelectionMode) clipboardState else null,
                    onPaste = {
                        scope.launch {
                            FileOperations.paste(currentDir)
                            refreshFiles()
                        }
                    },
                    onClear = {
                        FileOperations.currentClipboard = null
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(top = 60.dp)
                )
            }

            // 1:1 Bottom Bar with AnimatedContent transition between Normal / Selection / Clipboard Paste modes
            val bottomMode = when {
                isSelectionMode -> 1
                clipboardState != null -> 2
                else -> 0
            }
            AnimatedContent(
                targetState = bottomMode,
                transitionSpec = {
                    (slideInVertically(animationSpec = folmeSpring(damping = 0.85f, response = 0.28f)) { it / 2 } + fadeIn()) togetherWith
                        (slideOutVertically(animationSpec = folmeSpring(damping = 0.9f, response = 0.22f)) { it / 2 } + fadeOut())
                },
                label = "explorer_bottom_bar"
            ) { mode ->
                when (mode) {
                    1 -> FileActionBottomBar(
                        selectedCount = selectedPaths.size,
                        onCopy = {
                            val selectedFiles = files.filter { selectedPaths.contains(it.path) }.map { it.file }
                            FileOperations.currentClipboard = FileOperations.ClipboardItem(selectedFiles, FileOperations.ClipboardMode.COPY)
                            selectedPaths = emptySet()
                        },
                        onCut = {
                            val selectedFiles = files.filter { selectedPaths.contains(it.path) }.map { it.file }
                            FileOperations.currentClipboard = FileOperations.ClipboardItem(selectedFiles, FileOperations.ClipboardMode.CUT)
                            selectedPaths = emptySet()
                        },
                        onDelete = { showDeleteDialog = true },
                        onRename = {
                            val selected = files.firstOrNull { selectedPaths.contains(it.path) }
                            if (selected != null) {
                                renameFileName = selected.name
                                showRenameDialog = true
                            }
                        },
                        onMore = {
                            showSelectionMoreDialog = true
                        }
                    )
                    2 -> EsClipboardBottomBar(
                        onPaste = {
                            scope.launch {
                                FileOperations.paste(currentDir)
                                refreshFiles()
                            }
                        },
                        onNewFolder = {
                            newFolderName = ""
                            showNewFolderDialog = true
                        },
                        onCancel = {
                            FileOperations.currentClipboard = null
                        }
                    )
                    else -> Column(
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
                            ExplorerBottomActionItem("Novo", R.drawable.toolbar_new, modifier = Modifier.weight(1f)) {
                                newFolderName = ""
                                showNewFolderDialog = true
                            }
                            ExplorerBottomActionItem("Procurar", R.drawable.toolbar_search, modifier = Modifier.weight(1f)) {
                                showSearchBar = !showSearchBar
                                if (!showSearchBar) searchQuery = ""
                            }
                            ExplorerBottomActionItem("Atualizar", R.drawable.toolbar_refresh, modifier = Modifier.weight(1f)) { refreshFiles() }
                            ExplorerBottomActionItem("Visualizar", R.drawable.toolbar_view, modifier = Modifier.weight(1f)) {
                                isGridView = !isGridView
                            }
                            ExplorerBottomActionItem("Mais", R.drawable.toolbar_more, modifier = Modifier.weight(1f)) {
                                showMoreMenu = true
                            }
                        }
                    }
                }
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
        }
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        OverlayDialog(
            show = showNewFolderDialog,
            onDismissRequest = { showNewFolderDialog = false },
            title = "Nova Pasta",
            content = {
                TextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = "Digite o nome da pasta",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        text = "Cancelar",
                        onClick = { showNewFolderDialog = false },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        text = "Criar",
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = {
                            if (newFolderName.isNotBlank()) {
                                scope.launch {
                                    FileOperations.createDirectory(currentDir, newFolderName.trim())
                                    showNewFolderDialog = false
                                    refreshFiles()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        )
    }

    // Rename Dialog
    if (showRenameDialog) {
        OverlayDialog(
            show = showRenameDialog,
            onDismissRequest = { showRenameDialog = false },
            title = "Renomear",
            content = {
                TextField(
                    value = renameFileName,
                    onValueChange = { renameFileName = it },
                    label = "Novo nome",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        text = "Cancelar",
                        onClick = { showRenameDialog = false },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        text = "Salvar",
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = {
                            val selected = files.firstOrNull { selectedPaths.contains(it.path) }
                            if (selected != null && renameFileName.isNotBlank()) {
                                scope.launch {
                                    FileOperations.rename(selected.file, renameFileName.trim())
                                    showRenameDialog = false
                                    refreshFiles()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        )
    }

    // Delete Confirmation Dialog (1:1 ES File Explorer with "Mover para a Reciclagem" checkbox vs direct permanent deletion)
    if (showDeleteDialog) {
        val defaultRecycle = remember(prefs) { prefs.getBoolean(MiuiXFilesPreferences.KEY_USE_RECYCLE_BIN, true) }
        var moveToRecycleBin by remember(defaultRecycle) { mutableStateOf(defaultRecycle) }
        OverlayDialog(
            show = showDeleteDialog,
            onDismissRequest = { showDeleteDialog = false },
            title = "Excluir",
            summary = if (moveToRecycleBin) {
                "Mover ${selectedPaths.size} item(ns) selecionado(s) para a Reciclagem?"
            } else {
                "Tem certeza de que deseja excluir permanentemente ${selectedPaths.size} item(ns)?"
            },
            content = {
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
                        text = if (moveToRecycleBin) "Mover" else "Excluir",
                        colors = ButtonDefaults.textButtonColorsPrimary(
                            color = if (moveToRecycleBin) Color(0xFF2F7DF6) else Color(0xFFE53935),
                            textColor = Color.White
                        ),
                        onClick = {
                            val toDelete = moveToRecycleBin
                            scope.launch {
                                val filesToDelete = files.filter { selectedPaths.contains(it.path) }.map { it.file }
                                if (toDelete) {
                                    FileOperations.moveToRecycle(filesToDelete)
                                } else {
                                    FileOperations.deleteFiles(filesToDelete)
                                }
                                showDeleteDialog = false
                                refreshFiles()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        )
    }

    if (showMoreMenu) {
        OverlayDialog(
            show = showMoreMenu,
            onDismissRequest = { showMoreMenu = false },
            title = "Opções e Ordenação",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        text = "Ordenar por Data (Mais recente)",
                        onClick = { currentSort = SortOption.DATE_DESC; showMoreMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        text = "Ordenar por Tamanho (Maior)",
                        onClick = { currentSort = SortOption.SIZE_DESC; showMoreMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        text = if (selectedPaths.size == displayFiles.size && displayFiles.isNotEmpty()) "Desmarcar Tudo" else "Selecionar Tudo (${displayFiles.size})",
                        onClick = {
                            selectedPaths = if (selectedPaths.size == displayFiles.size) emptySet()
                            else displayFiles.map { it.path }.toSet()
                            showMoreMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        text = if (showHidden) "Ocultar Arquivos Ocultos" else "Mostrar Arquivos Ocultos",
                        onClick = {
                            showHidden = !showHidden
                            prefs.edit().putBoolean(MiuiXFilesPreferences.KEY_SHOW_HIDDEN, showHidden).apply()
                            showMoreMenu = false
                        },
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

    if (showSelectionMoreDialog) {
        val selectedFiles = remember(files, selectedPaths) {
            files.filter { selectedPaths.contains(it.path) }.map { it.file }
        }
        OverlayDialog(
            show = showSelectionMoreDialog,
            onDismissRequest = { showSelectionMoreDialog = false },
            title = "Ações (${selectedFiles.size})",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        text = "Compactar em .ZIP",
                        onClick = {
                            showSelectionMoreDialog = false
                            scope.launch {
                                val zipName = if (selectedFiles.size == 1) "${selectedFiles.first().nameWithoutExtension}.zip" else "Arquivo.zip"
                                FileOperations.zipFiles(selectedFiles, File(currentDir, zipName))
                                refreshFiles()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (selectedFiles.size == 1 && selectedFiles.first().extension.lowercase() == "zip") {
                        TextButton(
                            text = "Extrair Arquivo .ZIP Aqui",
                            onClick = {
                                showSelectionMoreDialog = false
                                val zf = selectedFiles.first()
                                scope.launch {
                                    FileOperations.unzip(zf, File(currentDir, zf.nameWithoutExtension))
                                    refreshFiles()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TextButton(
                        text = "Compartilhar / Enviar",
                        onClick = {
                            showSelectionMoreDialog = false
                            val f = selectedFiles.firstOrNull() ?: return@TextButton
                            runCatching {
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "*/*"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Compartilhar ${f.name}"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        text = "Propriedades e Checksum (MD5)",
                        onClick = {
                            showSelectionMoreDialog = false
                            showPropertiesDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        text = "Cancelar",
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = { showSelectionMoreDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    if (showPropertiesDialog) {
        val selectedFiles = remember(files, selectedPaths) {
            files.filter { selectedPaths.contains(it.path) }.map { it.file }
        }
        val first = selectedFiles.firstOrNull()
        var md5 by remember(first) { mutableStateOf("Calculando...") }
        LaunchedEffect(first) {
            if (first != null && first.isFile && first.length() < 150 * 1024 * 1024) {
                md5 = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching {
                        val digest = java.security.MessageDigest.getInstance("MD5")
                        first.inputStream().use { input ->
                            val buf = ByteArray(16384)
                            var r: Int
                            while (input.read(buf).also { r = it } > 0) digest.update(buf, 0, r)
                        }
                        digest.digest().joinToString("") { "%02x".format(it) }
                    }.getOrDefault("-")
                }
            } else {
                md5 = "-"
            }
        }
        OverlayDialog(
            show = showPropertiesDialog,
            onDismissRequest = { showPropertiesDialog = false },
            title = "Propriedades",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (first != null) {
                        Text("Nome: ${first.name}", fontSize = 13.5.sp, color = EsTextPrimary, fontWeight = FontWeight.Bold)
                        Text("Caminho: ${first.absolutePath}", fontSize = 12.sp, color = EsTextSecondary)
                        Text("Tipo: ${if (first.isDirectory) "Pasta" else "Arquivo (.${first.extension})"}", fontSize = 12.sp, color = EsTextSecondary)
                        Text("Tamanho: ${StorageManager.formatFileSize(selectedFiles.sumOf { if (it.isDirectory) 0L else it.length() })}", fontSize = 12.sp, color = EsTextSecondary)
                        Text("Modificado: ${StorageManager.formatDate(first.lastModified())}", fontSize = 12.sp, color = EsTextSecondary)
                        Text("Permissões: ${if (first.canRead()) "r" else "-"}${if (first.canWrite()) "w" else "-"}${if (first.canExecute()) "x" else "-"}", fontSize = 12.sp, color = EsTextSecondary)
                        if (first.isFile) {
                            Text("MD5: $md5", fontSize = 11.5.sp, color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        text = "Fechar",
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = { showPropertiesDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}

@Composable
private fun ExplorerBottomActionItem(
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
