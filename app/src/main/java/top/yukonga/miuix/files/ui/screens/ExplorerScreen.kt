package top.yukonga.miuix.files.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    onAnalyzerClick: (() -> Unit)? = null,
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

    var showNewTypeDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameFileName by remember { mutableStateOf("") }
    var showBatchRenameDialog by remember { mutableStateOf(false) }
    var batchRenamePattern by remember { mutableStateOf("") }
    var showZipDialog by remember { mutableStateOf(false) }
    var zipArchiveName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showViewMenu by remember { mutableStateOf(false) }
    var viewGridColumns by remember(defaultViewMode) { mutableIntStateOf(if (defaultViewMode == 1) 5 else 4) }
    var showSelectionMoreDialog by remember { mutableStateOf(false) }
    var showOpenAsDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    var tappedApkFile by remember { mutableStateOf<File?>(null) }

    // 1:1 ES File Explorer Drag & Drop state (only active on ALREADY selected items; moves directly without popup)
    val itemBoundsMap = remember(currentDir) { mutableStateMapOf<String, Rect>() }
    val folderBoundsMap = remember(currentDir) { mutableStateMapOf<String, Rect>() }
    val breadcrumbBoundsMap = remember(currentDir) { mutableStateMapOf<String, Rect>() }
    var rootWindowSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var rootWindowOffset by remember { mutableStateOf(Offset.Zero) }
    var isDraggingItems by remember { mutableStateOf(false) }
    var dragStartedMoving by remember { mutableStateOf(false) }
    var dragWindowPos by remember { mutableStateOf(Offset.Zero) }
    var draggedPrimaryItem by remember { mutableStateOf<FileItem?>(null) }
    var hoveredFolderPath by remember { mutableStateOf<String?>(null) }
    var hoveredBreadcrumbPath by remember { mutableStateOf<String?>(null) }
    var hoveredCornerZone by remember { mutableIntStateOf(0) } // 0=none, 1=TopLeft(Share), 2=TopRight(Copy), 3=BottomLeft(Send), 4=BottomRight(Delete)

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

    val currentSelectedPaths by rememberUpdatedState(selectedPaths)
    val currentDisplayFiles by rememberUpdatedState(displayFiles)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val cornerRadiusPx = with(density) { 125.dp.toPx() }

    fun handleItemDragStart(item: FileItem, windowPos: Offset) {
        draggedPrimaryItem = item
        isDraggingItems = true
        dragStartedMoving = true
        dragWindowPos = windowPos
        hoveredFolderPath = null
        hoveredBreadcrumbPath = null
        hoveredCornerZone = 0
    }

    fun handleItemDragMove(windowPos: Offset) {
        dragStartedMoving = true
        dragWindowPos = windowPos
        val localX = windowPos.x - rootWindowOffset.x
        val localY = windowPos.y - rootWindowOffset.y
        val w = rootWindowSize.width.toFloat().coerceAtLeast(1f)
        val h = rootWindowSize.height.toFloat().coerceAtLeast(1f)

        // Check 4 triangular corner action zones first (1:1 ES File Explorer)
        val cornerHit = when {
            localX >= 0f && localY >= 0f && (localX + localY) <= cornerRadiusPx -> 1 // Top-Left: Share
            localX <= w && localY >= 0f && ((w - localX) + localY) <= cornerRadiusPx -> 2 // Top-Right: Copy to clipboard
            localX >= 0f && localY <= h && (localX + (h - localY)) <= cornerRadiusPx -> 3 // Bottom-Left: Send
            localX <= w && localY <= h && ((w - localX) + (h - localY)) <= cornerRadiusPx -> 4 // Bottom-Right: Delete
            else -> 0
        }
        hoveredCornerZone = cornerHit
        if (cornerHit != 0) {
            hoveredBreadcrumbPath = null
            hoveredFolderPath = null
            return
        }

        val bcHit = breadcrumbBoundsMap.entries.firstOrNull { (segPath, rect) ->
            rect.inflate(14f).contains(windowPos) && segPath != currentDir.absolutePath
        }?.key
        hoveredBreadcrumbPath = bcHit
        if (bcHit != null) {
            hoveredFolderPath = null
        } else {
            val folderHit = folderBoundsMap.entries.firstOrNull { (dirPath, rect) ->
                rect.contains(windowPos) && !currentSelectedPaths.contains(dirPath)
            }?.key
            hoveredFolderPath = folderHit
        }
    }

    fun handleItemDragEnd() {
        val corner = hoveredCornerZone
        val targetPath = hoveredBreadcrumbPath ?: hoveredFolderPath
        val draggedItems = currentDisplayFiles.filter { currentSelectedPaths.contains(it.path) }.map { it.file }

        if (dragStartedMoving && corner != 0 && draggedItems.isNotEmpty()) {
            when (corner) {
                1, 3 -> {
                    // Share / Send selected files
                    val filesOnly = draggedItems.filter { it.isFile }
                    if (filesOnly.isNotEmpty()) {
                        runCatching {
                            if (filesOnly.size == 1) {
                                val f = filesOnly.first()
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "*/*"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Compartilhar ${f.name}"))
                            } else {
                                val uris = ArrayList(filesOnly.map {
                                    androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
                                })
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "*/*"
                                    putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Compartilhar ${filesOnly.size} arquivos"))
                            }
                        }
                    }
                    selectedPaths = emptySet()
                }
                2 -> {
                    // Copy to ES Clipboard
                    FileOperations.currentClipboard = FileOperations.ClipboardItem(draggedItems, FileOperations.ClipboardMode.COPY)
                    selectedPaths = emptySet()
                }
                4 -> {
                    // Delete confirmation dialog
                    showDeleteDialog = true
                }
            }
        } else if (dragStartedMoving && targetPath != null) {
            val destDir = File(targetPath)
            val dragged = draggedItems.filter { it.absolutePath != destDir.absolutePath && it.parentFile?.absolutePath != destDir.absolutePath }
            if (destDir.isDirectory && dragged.isNotEmpty()) {
                selectedPaths = emptySet()
                scope.launch {
                    FileOperations.currentClipboard = FileOperations.ClipboardItem(dragged, FileOperations.ClipboardMode.CUT)
                    FileOperations.paste(destDir)
                    refreshFiles()
                }
            }
        }
        isDraggingItems = false
        dragStartedMoving = false
        draggedPrimaryItem = null
        hoveredFolderPath = null
        hoveredBreadcrumbPath = null
        hoveredCornerZone = 0
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EsBgColor)
            .onGloballyPositioned { coords ->
                rootWindowOffset = coords.boundsInWindow().topLeft
                rootWindowSize = coords.size
            }
            .pointerInput(currentDir) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val startWindowPos = rootWindowOffset + down.position
                    var draggingActive = false
                    val slop = viewConfiguration.touchSlop * 0.65f
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                        if (change == null || !change.pressed) {
                            if (draggingActive) {
                                handleItemDragEnd()
                            }
                            break
                        }
                        val currentWindowPos = rootWindowOffset + change.position
                        if (!draggingActive) {
                            if (currentSelectedPaths.isNotEmpty() && (currentWindowPos - startWindowPos).getDistance() > slop) {
                                val hitSelectedPath = currentSelectedPaths.firstOrNull { path ->
                                    val r = itemBoundsMap[path]
                                    r != null && (r.inflate(8f).contains(startWindowPos) || r.inflate(8f).contains(currentWindowPos))
                                }
                                val primaryItem = if (hitSelectedPath != null) {
                                    currentDisplayFiles.firstOrNull { it.path == hitSelectedPath }
                                } else if (currentSelectedPaths.size == 1) {
                                    val onlyPath = currentSelectedPaths.first()
                                    val r = itemBoundsMap[onlyPath]
                                    if (r == null || r.inflate(24f).contains(startWindowPos) || r.inflate(24f).contains(currentWindowPos)) {
                                        currentDisplayFiles.firstOrNull { it.path == onlyPath }
                                    } else null
                                } else null

                                if (primaryItem != null) {
                                    draggingActive = true
                                    handleItemDragStart(primaryItem, currentWindowPos)
                                    handleItemDragMove(currentWindowPos)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        } else {
                            handleItemDragMove(currentWindowPos)
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1:1 Top Bar for Explorer (collapses during Drag & Drop just like original ES File Explorer!)
            val displayTabs = remember(tabs, activeTabId, isSelectionMode, selectedPaths.size, currentDir) {
                if (tabs.isEmpty()) emptyList()
                else tabs.map { t ->
                    if (t.id == activeTabId && isSelectionMode) {
                        t.copy(title = "${selectedPaths.size} selecionados")
                    } else t
                }
            }
            if (!isDraggingItems) {
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
            } else {
                Spacer(modifier = Modifier.statusBarsPadding().height(54.dp))
            }

            // Path Breadcrumb Bar with Right Pointed Gauge Tag (opens Space Analyzer) & Drop Targets for parent folders
            val totalSpace = currentDir.totalSpace
            val usedPct = if (totalSpace > 0) (((totalSpace - currentDir.freeSpace) * 100L) / totalSpace).toInt().coerceIn(0, 100) else 22
            EsPathBar(
                currentDir = currentDir,
                onNavigate = { target -> navigateToDir(target) },
                percentage = usedPct,
                onAnalyzerClick = onAnalyzerClick,
                hoveredDropPath = hoveredBreadcrumbPath,
                onSegmentBoundsChanged = { segPath, rect ->
                    breadcrumbBoundsMap[segPath] = rect
                }
            )

            // Inline Copy / Cut Progress Strip (never blocks the full screen)
            InlineTransferProgressStrip(state = transferState)

            AnimatedVisibility(
                visible = showSearchBar && !isDraggingItems,
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

            Box(
                modifier = Modifier.weight(1f)
            ) {
                AnimatedContent(
                    targetState = Pair(currentDir.absolutePath, isGridView),
                    transitionSpec = {
                        val dirChanged = initialState.first != targetState.first
                        if (dirChanged) {
                            // Subtle, instant-feeling fade + micro-scale inside the content area (no full-screen slide over)
                            val enterScale = if (navForward) 0.985f else 1.015f
                            (fadeIn(animationSpec = folmeSpring(damping = 0.96f, response = 0.16f)) +
                                scaleIn(initialScale = enterScale, animationSpec = folmeSpring(damping = 0.94f, response = 0.18f))) togetherWith
                                fadeOut(animationSpec = folmeSpring(damping = 0.96f, response = 0.12f))
                        } else {
                            // Grid <-> List view switch: authentic MIUIX Folme spring zoom + fade transition
                            val toGrid = targetState.second
                            val inScale = if (toGrid) 0.94f else 1.04f
                            val outScale = if (toGrid) 1.04f else 0.94f
                            (scaleIn(
                                initialScale = inScale,
                                animationSpec = folmeSpring(damping = 0.88f, response = 0.22f)
                            ) + fadeIn(
                                animationSpec = folmeSpring(damping = 0.92f, response = 0.18f)
                            )) togetherWith (scaleOut(
                                targetScale = outScale,
                                animationSpec = folmeSpring(damping = 0.92f, response = 0.16f)
                            ) + fadeOut(
                                animationSpec = folmeSpring(damping = 0.92f, response = 0.14f)
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
                            columns = GridCells.Fixed(viewGridColumns),
                            userScrollEnabled = !isDraggingItems,
                            modifier = Modifier.fillMaxSize().overScrollVertical(),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(displayFiles, key = { it.path }) { item ->
                                val isSelected = selectedPaths.contains(item.path)
                                val isBeingDragged = isDraggingItems && dragStartedMoving && isSelected
                                val isDropTarget = item.isDirectory && hoveredFolderPath == item.path
                                FileGridItem(
                                    item = item,
                                    isSelected = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    isBeingDragged = isBeingDragged,
                                    isDropTarget = isDropTarget,
                                    onBoundsChanged = { rect ->
                                        itemBoundsMap[item.path] = rect
                                        if (item.isDirectory) folderBoundsMap[item.path] = rect
                                    },
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
                            userScrollEnabled = !isDraggingItems,
                            modifier = Modifier.fillMaxSize().overScrollVertical(),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(displayFiles, key = { it.path }) { item ->
                                val isSelected = selectedPaths.contains(item.path)
                                val isBeingDragged = isDraggingItems && dragStartedMoving && isSelected
                                val isDropTarget = item.isDirectory && hoveredFolderPath == item.path
                                FileListItem(
                                    item = item,
                                    isSelected = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    isBeingDragged = isBeingDragged,
                                    isDropTarget = isDropTarget,
                                    onBoundsChanged = { rect ->
                                        itemBoundsMap[item.path] = rect
                                        if (item.isDirectory) folderBoundsMap[item.path] = rect
                                    },
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

            // 1:1 Bottom Bar (hidden during active Drag & Drop so the bottom corner triangles are unobstructed, while keeping exact layout height!)
            if (!isDraggingItems) {
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
                                if (selectedPaths.size > 1) {
                                    val first = files.firstOrNull { selectedPaths.contains(it.path) }
                                    batchRenamePattern = first?.file?.nameWithoutExtension ?: "Arquivo"
                                    showBatchRenameDialog = true
                                } else {
                                    val selected = files.firstOrNull { selectedPaths.contains(it.path) }
                                    if (selected != null) {
                                        renameFileName = selected.name
                                        showRenameDialog = true
                                    }
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
                                    showNewTypeDialog = true
                                }
                                ExplorerBottomActionItem("Procurar", R.drawable.toolbar_search, modifier = Modifier.weight(1f)) {
                                    showSearchBar = !showSearchBar
                                    if (!showSearchBar) searchQuery = ""
                                }
                                ExplorerBottomActionItem("Atualizar", R.drawable.toolbar_refresh, modifier = Modifier.weight(1f)) { refreshFiles() }
                                ExplorerBottomActionItem("Visualizar", R.drawable.toolbar_view, modifier = Modifier.weight(1f)) {
                                    showViewMenu = true
                                }
                                ExplorerBottomActionItem("Mais", R.drawable.toolbar_more, modifier = Modifier.weight(1f)) {
                                    showMoreMenu = true
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .navigationBarsPadding()
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
        }

        // 1:1 ES File Explorer Drag & Drop Full-Screen Overlay:
        // - 4 Triangular Corner Drop Zones (Top-Left: Share, Top-Right: Copy, Bottom-Left: Send, Bottom-Right: Delete)
        // - Real Floating File/Folder Icon + Item Name following finger everywhere on screen without clipping
        if (isDraggingItems && dragStartedMoving) {
            val cornerSize = 110.dp
            val normalCornerColor = Color(0xE8373B40)
            val activeCornerColor = Color(0xF02F7DF6)
            val activeDeleteColor = Color(0xF0E53935)
            val cornerEdgeColor = Color(0xFF1F2225)
            val cornerIconInactive = Color(0xFF9EA2A6)

            // Top-Left Triangle (Share)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .size(cornerSize)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path, if (hoveredCornerZone == 1) activeCornerColor else normalCornerColor)
                    drawLine(
                        color = cornerEdgeColor,
                        start = Offset(size.width, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                Image(
                    painter = painterResource(R.drawable.toolbar_share),
                    contentDescription = "Compartilhar",
                    modifier = Modifier
                        .padding(start = 18.dp, top = 16.dp)
                        .size(25.dp),
                    colorFilter = ColorFilter.tint(if (hoveredCornerZone == 1) Color.White else cornerIconInactive)
                )
            }

            // Top-Right Triangle (Copy)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .size(cornerSize)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(size.width, 0f)
                        lineTo(0f, 0f)
                        lineTo(size.width, size.height)
                        close()
                    }
                    drawPath(path, if (hoveredCornerZone == 2) activeCornerColor else normalCornerColor)
                    drawLine(
                        color = cornerEdgeColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                Image(
                    painter = painterResource(R.drawable.toolbar_copy),
                    contentDescription = "Copiar",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 18.dp, top = 16.dp)
                        .size(25.dp),
                    colorFilter = ColorFilter.tint(if (hoveredCornerZone == 2) Color.White else cornerIconInactive)
                )
            }

            // Bottom-Left Triangle (Send)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .size(cornerSize)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(0f, size.height)
                        lineTo(size.width, size.height)
                        lineTo(0f, 0f)
                        close()
                    }
                    drawPath(path, if (hoveredCornerZone == 3) activeCornerColor else normalCornerColor)
                    drawLine(
                        color = cornerEdgeColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                Image(
                    painter = painterResource(R.drawable.icon_home_tab_sender),
                    contentDescription = "Enviar",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 18.dp, bottom = 16.dp)
                        .size(25.dp),
                    colorFilter = ColorFilter.tint(if (hoveredCornerZone == 3) Color.White else cornerIconInactive)
                )
            }

            // Bottom-Right Triangle (Delete)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .size(cornerSize)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(size.width, size.height)
                        lineTo(0f, size.height)
                        lineTo(size.width, 0f)
                        close()
                    }
                    drawPath(path, if (hoveredCornerZone == 4) activeDeleteColor else normalCornerColor)
                    drawLine(
                        color = cornerEdgeColor,
                        start = Offset(size.width, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                Image(
                    painter = painterResource(R.drawable.toolbar_delete),
                    contentDescription = "Excluir",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 18.dp, bottom = 16.dp)
                        .size(25.dp),
                    colorFilter = ColorFilter.tint(if (hoveredCornerZone == 4) Color.White else cornerIconInactive)
                )
            }

            // Floating Icon + Name Label (1:1 ES File Explorer)
            val localX = (dragWindowPos.x - rootWindowOffset.x).toInt()
            val localY = (dragWindowPos.y - rootWindowOffset.y).toInt()
            val primaryItem = draggedPrimaryItem ?: files.firstOrNull { selectedPaths.contains(it.path) }
            if (primaryItem != null) {
                val dragIconRes = when {
                    primaryItem.isDirectory -> R.drawable.format_folder
                    primaryItem.extension == "apk" -> R.drawable.format_apk
                    primaryItem.extension in setOf("jpg", "jpeg", "png", "webp", "gif") -> R.drawable.format_picture
                    primaryItem.extension in setOf("mp4", "mkv", "avi", "mov") -> R.drawable.format_video
                    primaryItem.extension in setOf("mp3", "flac", "wav", "m4a") -> R.drawable.format_music
                    primaryItem.extension in setOf("zip", "rar", "7z", "tar") -> R.drawable.format_zip
                    primaryItem.extension == "pdf" -> R.drawable.format_pdf
                    primaryItem.extension in setOf("doc", "docx") -> R.drawable.format_word
                    primaryItem.extension in setOf("xls", "xlsx", "csv") -> R.drawable.format_excel
                    primaryItem.extension in setOf("ppt", "pptx") -> R.drawable.format_ppt
                    primaryItem.extension in setOf("txt", "log", "json", "xml", "md", "ini", "conf", "prop") -> R.drawable.format_text
                    else -> R.drawable.format_unkown
                }
                val folderBadge = if (primaryItem.isDirectory) getEsFolderBadgeRes(primaryItem.name) else null
                var folderAppIcon by remember(primaryItem.path) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
                LaunchedEffect(primaryItem.path) {
                    if (primaryItem.isDirectory && folderBadge == null) {
                        folderAppIcon = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            top.yukonga.miuix.files.ui.components.loadFolderAppBitmap(context, primaryItem.file)
                        }
                    }
                }
                val halfWidthPx = with(density) { 48.dp.roundToPx() }
                val offsetYAdjPx = with(density) { 54.dp.roundToPx() }
                Column(
                    modifier = Modifier
                        .offset { IntOffset(localX - halfWidthPx, localY - offsetYAdjPx) }
                        .width(96.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedPaths.size > 1) {
                            Image(
                                painter = painterResource(dragIconRes),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(62.dp)
                                    .offset(x = 6.dp, y = (-5).dp),
                                alpha = 0.55f
                            )
                        }
                        if (isMediaFile(primaryItem.file)) {
                            FileThumbnail(
                                file = primaryItem.file,
                                fallbackRes = dragIconRes,
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            Image(
                                painter = painterResource(dragIconRes),
                                contentDescription = null,
                                modifier = Modifier.size(68.dp),
                                alpha = 0.92f
                            )
                        }
                        if (primaryItem.isDirectory && folderBadge != null) {
                            Image(
                                painter = painterResource(folderBadge),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(30.dp)
                                    .offset(y = 3.dp),
                                alpha = 0.92f
                            )
                        } else if (primaryItem.isDirectory && folderAppIcon != null) {
                            Image(
                                bitmap = folderAppIcon!!,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(28.dp)
                                    .offset(y = 3.dp)
                                    .clip(CircleShape),
                                alpha = 0.92f
                            )
                        }
                        if (selectedPaths.size > 1) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2F7DF6))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${selectedPaths.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = primaryItem.name,
                        fontSize = 14.sp,
                        color = EsTextPrimary.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // New Type Choice (Pasta vs Arquivo)
    if (showNewTypeDialog) {
        OverlayDialog(
            show = showNewTypeDialog,
            onDismissRequest = { showNewTypeDialog = false },
            title = "Novo",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        text = "Pasta",
                        onClick = {
                            showNewTypeDialog = false
                            newFolderName = ""
                            showNewFolderDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        text = "Arquivo",
                        onClick = {
                            showNewTypeDialog = false
                            newFolderName = ""
                            showNewFileDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        text = "Cancelar",
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = { showNewTypeDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    // 1:1 View Mode & Sort Dialog ("Visualizar")
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
                        onClick = {
                            isGridView = true
                            viewGridColumns = 4
                            prefs.edit().putInt(MiuiXFilesPreferences.KEY_DEFAULT_VIEW_MODE, 0).apply()
                            showViewMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        text = if (isGridView && viewGridColumns == 5) "✓ Ícone Médio (5 colunas)" else "Ícone Médio (5 colunas)",
                        onClick = {
                            isGridView = true
                            viewGridColumns = 5
                            prefs.edit().putInt(MiuiXFilesPreferences.KEY_DEFAULT_VIEW_MODE, 1).apply()
                            showViewMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        text = if (!isGridView) "✓ Lista Detalhada" else "Lista Detalhada",
                        onClick = {
                            isGridView = false
                            prefs.edit().putInt(MiuiXFilesPreferences.KEY_DEFAULT_VIEW_MODE, 2).apply()
                            showViewMenu = false
                        },
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

    // 1:1 ES File Explorer New Folder Dialog
    if (showNewFolderDialog) {
        EsInputDialog(
            title = "Nova Pasta",
            value = newFolderName,
            onValueChange = { newFolderName = it },
            placeholder = "Nome da pasta",
            confirmText = "Criar",
            onConfirm = {
                if (newFolderName.isNotBlank()) {
                    scope.launch {
                        FileOperations.createDirectory(currentDir, newFolderName.trim())
                        showNewFolderDialog = false
                        refreshFiles()
                    }
                }
            },
            onDismiss = { showNewFolderDialog = false }
        )
    }

    // 1:1 ES File Explorer New File Dialog
    if (showNewFileDialog) {
        EsInputDialog(
            title = "Novo Arquivo",
            value = newFolderName,
            onValueChange = { newFolderName = it },
            placeholder = "Nome do arquivo (ex: nota.txt)",
            confirmText = "Criar",
            onConfirm = {
                if (newFolderName.isNotBlank()) {
                    scope.launch {
                        FileOperations.createFile(currentDir, newFolderName.trim())
                        showNewFileDialog = false
                        refreshFiles()
                    }
                }
            },
            onDismiss = { showNewFileDialog = false }
        )
    }

    // 1:1 ES File Explorer Rename Dialog
    if (showRenameDialog) {
        EsInputDialog(
            title = "Renomear",
            value = renameFileName,
            onValueChange = { renameFileName = it },
            confirmText = "Salvar",
            onConfirm = {
                val selected = files.firstOrNull { selectedPaths.contains(it.path) }
                if (selected != null && renameFileName.isNotBlank()) {
                    scope.launch {
                        FileOperations.rename(selected.file, renameFileName.trim())
                        showRenameDialog = false
                        refreshFiles()
                    }
                }
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    // 1:1 ES File Explorer Batch Rename Dialog (when 2+ items are selected)
    if (showBatchRenameDialog) {
        EsInputDialog(
            title = "Renomear em Lote (${selectedPaths.size})",
            value = batchRenamePattern,
            onValueChange = { batchRenamePattern = it },
            placeholder = "Nome base (ex: Foto_#)",
            confirmText = "Renomear",
            onConfirm = {
                val selectedFiles = files.filter { selectedPaths.contains(it.path) }.map { it.file }
                if (selectedFiles.isNotEmpty() && batchRenamePattern.isNotBlank()) {
                    scope.launch {
                        FileOperations.batchRename(selectedFiles, batchRenamePattern.trim(), startNumber = 1)
                        showBatchRenameDialog = false
                        selectedPaths = emptySet()
                        refreshFiles()
                    }
                }
            },
            onDismiss = { showBatchRenameDialog = false }
        )
    }

    // 1:1 ES File Explorer Compress (.ZIP) Name Dialog
    if (showZipDialog) {
        EsInputDialog(
            title = "Compactar em .ZIP",
            value = zipArchiveName,
            onValueChange = { zipArchiveName = it },
            placeholder = "Nome do arquivo .zip",
            confirmText = "Compactar",
            onConfirm = {
                val selectedFiles = files.filter { selectedPaths.contains(it.path) }.map { it.file }
                val raw = zipArchiveName.trim()
                if (selectedFiles.isNotEmpty() && raw.isNotBlank()) {
                    val finalZipName = if (raw.lowercase().endsWith(".zip")) raw else "$raw.zip"
                    showZipDialog = false
                    selectedPaths = emptySet()
                    scope.launch {
                        FileOperations.zipFiles(selectedFiles, File(currentDir, finalZipName))
                        refreshFiles()
                    }
                }
            },
            onDismiss = { showZipDialog = false }
        )
    }

    // Delete Confirmation Dialog (MIUIX OverlayDialog with "Mover para a Reciclagem" checkbox vs direct permanent deletion)
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
                    if (selectedFiles.size == 1 && selectedFiles.first().isFile) {
                        TextButton(
                            text = "Abrir como...",
                            onClick = {
                                showSelectionMoreDialog = false
                                showOpenAsDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (selectedFiles.size == 1) {
                        val single = selectedFiles.first()
                        val isFav = remember(single) { MiuiXFilesPreferences.isFavorite(context, single) }
                        TextButton(
                            text = if (isFav) "Remover dos Favoritos" else "Adicionar aos Favoritos",
                            onClick = {
                                val added = MiuiXFilesPreferences.toggleFavorite(context, single)
                                showSelectionMoreDialog = false
                                selectedPaths = emptySet()
                                android.widget.Toast.makeText(
                                    context,
                                    if (added) "Adicionado aos Favoritos" else "Removido dos Favoritos",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(
                            text = "Copiar Caminho Completo",
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                cm?.setPrimaryClip(android.content.ClipData.newPlainText("path", single.absolutePath))
                                showSelectionMoreDialog = false
                                selectedPaths = emptySet()
                                android.widget.Toast.makeText(context, "Caminho copiado: ${single.absolutePath}", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        val isHiddenItem = single.name.startsWith(".")
                        TextButton(
                            text = if (isHiddenItem) "Desocultar (remover '.' do nome)" else "Ocultar (adicionar '.' ao nome)",
                            onClick = {
                                showSelectionMoreDialog = false
                                val targetName = if (isHiddenItem) single.name.removePrefix(".") else ".${single.name}"
                                scope.launch {
                                    if (targetName.isNotBlank()) {
                                        FileOperations.rename(single, targetName)
                                        refreshFiles()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TextButton(
                        text = "Compactar em .ZIP",
                        onClick = {
                            showSelectionMoreDialog = false
                            zipArchiveName = if (selectedFiles.size == 1) {
                                "${selectedFiles.first().nameWithoutExtension}.zip"
                            } else {
                                "${currentDir.name.ifBlank { "Arquivo" }}.zip"
                            }
                            showZipDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (selectedFiles.size == 1 && selectedFiles.first().extension.lowercase() == "zip") {
                        TextButton(
                            text = "Extrair para /${selectedFiles.first().nameWithoutExtension}/",
                            onClick = {
                                showSelectionMoreDialog = false
                                val zf = selectedFiles.first()
                                selectedPaths = emptySet()
                                scope.launch {
                                    FileOperations.unzip(zf, File(currentDir, zf.nameWithoutExtension))
                                    refreshFiles()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(
                            text = "Extrair no Diretório Atual",
                            onClick = {
                                showSelectionMoreDialog = false
                                val zf = selectedFiles.first()
                                selectedPaths = emptySet()
                                scope.launch {
                                    FileOperations.unzip(zf, currentDir)
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
                            val filesOnly = selectedFiles.filter { it.isFile }
                            if (filesOnly.isEmpty()) return@TextButton
                            selectedPaths = emptySet()
                            runCatching {
                                if (filesOnly.size == 1) {
                                    val f = filesOnly.first()
                                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "*/*"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Compartilhar ${f.name}"))
                                } else {
                                    val uris = ArrayList(filesOnly.map {
                                        androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
                                    })
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                                        type = "*/*"
                                        putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Compartilhar ${filesOnly.size} arquivos"))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        text = "Propriedades",
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

    if (showOpenAsDialog) {
        val targetFile = remember(files, selectedPaths) {
            files.firstOrNull { selectedPaths.contains(it.path) }?.file
        }
        fun launchOpenAs(mimeType: String) {
            showOpenAsDialog = false
            val f = targetFile ?: return
            selectedPaths = emptySet()
            runCatching {
                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Abrir como"))
            }
        }
        OverlayDialog(
            show = showOpenAsDialog,
            onDismissRequest = { showOpenAsDialog = false },
            title = "Abrir como",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(text = "Texto", onClick = { launchOpenAs("text/plain") }, modifier = Modifier.fillMaxWidth())
                    TextButton(text = "Áudio", onClick = { launchOpenAs("audio/*") }, modifier = Modifier.fillMaxWidth())
                    TextButton(text = "Vídeo", onClick = { launchOpenAs("video/*") }, modifier = Modifier.fillMaxWidth())
                    TextButton(text = "Imagem", onClick = { launchOpenAs("image/*") }, modifier = Modifier.fillMaxWidth())
                    TextButton(text = "Outros", onClick = { launchOpenAs("*/*") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        text = "Cancelar",
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = { showOpenAsDialog = false },
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
        var sha1 by remember(first) { mutableStateOf("Calculando...") }
        var totalBytes by remember(selectedFiles) {
            mutableLongStateOf(selectedFiles.sumOf { if (it.isDirectory) 0L else it.length() })
        }
        var totalFilesCount by remember(selectedFiles) { mutableIntStateOf(0) }
        var totalFoldersCount by remember(selectedFiles) { mutableIntStateOf(0) }
        var isCalculatingSize by remember(selectedFiles) { mutableStateOf(selectedFiles.any { it.isDirectory }) }

        LaunchedEffect(selectedFiles) {
            if (selectedFiles.any { it.isDirectory }) {
                isCalculatingSize = true
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    var bytes = 0L
                    var fCount = 0
                    var dCount = 0
                    for (root in selectedFiles) {
                        if (root.isFile) {
                            bytes += root.length()
                            fCount++
                        } else {
                            root.walkTopDown().forEach { sub ->
                                if (sub != root) {
                                    if (sub.isDirectory) dCount++
                                    else {
                                        fCount++
                                        bytes += sub.length()
                                    }
                                }
                            }
                        }
                    }
                    totalBytes = bytes
                    totalFilesCount = fCount
                    totalFoldersCount = dCount
                }
                isCalculatingSize = false
            }
            if (first != null && first.isFile && first.length() < 150 * 1024 * 1024) {
                val hashes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching {
                        val md5Digest = java.security.MessageDigest.getInstance("MD5")
                        val sha1Digest = java.security.MessageDigest.getInstance("SHA-1")
                        first.inputStream().use { input ->
                            val buf = ByteArray(32768)
                            var r: Int
                            while (input.read(buf).also { r = it } > 0) {
                                md5Digest.update(buf, 0, r)
                                sha1Digest.update(buf, 0, r)
                            }
                        }
                        val m = md5Digest.digest().joinToString("") { "%02x".format(it) }
                        val s = sha1Digest.digest().joinToString("") { "%02x".format(it) }
                        m to s
                    }.getOrDefault("-" to "-")
                }
                md5 = hashes.first
                sha1 = hashes.second
            } else {
                md5 = "-"
                sha1 = "-"
            }
        }
        OverlayDialog(
            show = showPropertiesDialog,
            onDismissRequest = { showPropertiesDialog = false },
            title = "Propriedades",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (first != null) {
                        val titleName = if (selectedFiles.size == 1) first.name else "${selectedFiles.size} itens selecionados"
                        Text("Nome: $titleName", fontSize = 13.5.sp, color = EsTextPrimary, fontWeight = FontWeight.Bold)
                        Text("Caminho: ${if (selectedFiles.size == 1) first.absolutePath else currentDir.absolutePath}", fontSize = 12.sp, color = EsTextSecondary)
                        if (selectedFiles.size == 1) {
                            Text("Tipo: ${if (first.isDirectory) "Pasta" else "Arquivo (.${first.extension})"}", fontSize = 12.sp, color = EsTextSecondary)
                        }
                        if (selectedFiles.any { it.isDirectory }) {
                            val countLabel = if (isCalculatingSize) "Calculando..." else "$totalFilesCount arquivos, $totalFoldersCount pastas"
                            Text("Contém: $countLabel", fontSize = 12.sp, color = EsTextSecondary)
                        }
                        val sizeLabel = if (isCalculatingSize) "${StorageManager.formatFileSize(totalBytes)} (calculando...)" else "${StorageManager.formatFileSize(totalBytes)} ($totalBytes B)"
                        Text("Tamanho: $sizeLabel", fontSize = 12.sp, color = EsTextSecondary)
                        Text("Modificado: ${StorageManager.formatDate(first.lastModified())}", fontSize = 12.sp, color = EsTextSecondary)
                        Text("Permissões: ${if (first.canRead()) "r" else "-"}${if (first.canWrite()) "w" else "-"}${if (first.canExecute()) "x" else "-"}", fontSize = 12.sp, color = EsTextSecondary)
                        if (selectedFiles.size == 1 && first.isFile) {
                            Text(
                                text = "MD5: $md5 (toque para copiar)",
                                fontSize = 11.5.sp,
                                color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    cm?.setPrimaryClip(android.content.ClipData.newPlainText("MD5", md5))
                                    android.widget.Toast.makeText(context, "MD5 copiado", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                            Text(
                                text = "SHA-1: $sha1 (toque para copiar)",
                                fontSize = 11.5.sp,
                                color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    cm?.setPrimaryClip(android.content.ClipData.newPlainText("SHA-1", sha1))
                                    android.widget.Toast.makeText(context, "SHA-1 copiado", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
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
