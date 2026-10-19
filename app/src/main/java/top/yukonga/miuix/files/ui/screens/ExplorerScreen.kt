package top.yukonga.miuix.files.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import java.io.File

@Composable
fun ExplorerScreen(
    initialDir: File,
    onNavigateBack: () -> Unit,
    onOpenFile: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { context.getSharedPreferences(MiuiXFilesPreferences.PREFS_NAME, Context.MODE_PRIVATE) }

    var currentDir by remember { mutableStateOf(initialDir) }
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showHidden by remember { mutableStateOf(prefs.getBoolean(MiuiXFilesPreferences.KEY_SHOW_HIDDEN, false)) }
    var currentSort by remember { mutableStateOf(SortOption.NAME_ASC) }
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isGridView by remember { mutableStateOf(true) } // Default to 4-column Grid matching ES File Explorer
    val isSelectionMode = selectedPaths.isNotEmpty()

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameFileName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var clipboardState by remember { mutableStateOf(FileOperations.currentClipboard) }

    fun refreshFiles() {
        scope.launch {
            files = StorageManager.listFiles(currentDir, showHidden = showHidden, sortOption = currentSort)
            selectedPaths = emptySet()
            clipboardState = FileOperations.currentClipboard
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
                currentDir = parent
            } else {
                onNavigateBack()
            }
        }
    }

    val displayFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) files
        else files.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1D1F24))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1:1 Top Bar for Explorer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Menu (☰)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (isSelectionMode) selectedPaths = emptySet()
                            else onNavigateBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(if (isSelectionMode) R.drawable.toolbar_cancel else R.drawable.toolbar_menu),
                        contentDescription = "Menu",
                        modifier = Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Small Home Icon
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.icon_home_tab_home),
                        contentDescription = "Início",
                        modifier = Modifier.size(16.dp),
                        colorFilter = ColorFilter.tint(Color(0xFFB0B3B8))
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Tab pill: 📁 Local ✕
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF2C2F36))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.format_folder),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSelectionMode) "${selectedPaths.size} selecionados" else "Local",
                        fontSize = 13.sp,
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
                        Text(text = "✕", fontSize = 11.sp, color = Color(0xFF9E9E9E))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Toggle Hidden Files
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            showHidden = !showHidden
                            prefs.edit().putBoolean(MiuiXFilesPreferences.KEY_SHOW_HIDDEN, showHidden).apply()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = if (showHidden) "👁️" else "🙈", fontSize = 16.sp)
                }
            }

            // Path Breadcrumb Bar with Right Gauge Chip
            EsPathBar(
                currentDir = currentDir,
                onNavigate = { target -> currentDir = target },
                percentage = 22
            )

            // Content Area (Grid or List)
            Box(modifier = Modifier.weight(1f)) {
                if (displayFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Pasta Vazia",
                            color = Color(0xFF9E9E9E),
                            fontSize = 14.sp
                        )
                    }
                } else if (isGridView) {
                    // 1:1 4-Column Grid View
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayFiles, key = { it.path }) { item ->
                            val isSelected = selectedPaths.contains(item.path)
                            FileGridItem(
                                item = item,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedPaths = if (isSelected) selectedPaths - item.path else selectedPaths + item.path
                                    } else {
                                        if (item.isDirectory) currentDir = item.file
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
                    // List View
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(displayFiles, key = { it.path }) { item ->
                            val isSelected = selectedPaths.contains(item.path)
                            FileListItem(
                                item = item,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedPaths = if (isSelected) selectedPaths - item.path else selectedPaths + item.path
                                    } else {
                                        if (item.isDirectory) currentDir = item.file
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

                // Clipboard Floating Indicator
                if (clipboardState != null && !isSelectionMode) {
                    ClipboardFloatingBar(
                        clipboard = clipboardState!!,
                        onPaste = {
                            scope.launch {
                                FileOperations.paste(currentDir)
                                refreshFiles()
                            }
                        },
                        onCancel = {
                            FileOperations.currentClipboard = null
                            clipboardState = null
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            // 1:1 Bottom Bar
            if (isSelectionMode) {
                FileActionBottomBar(
                    selectedCount = selectedPaths.size,
                    onCopy = {
                        val selectedFiles = files.filter { selectedPaths.contains(it.path) }.map { it.file }
                        FileOperations.currentClipboard = FileOperations.ClipboardItem(selectedFiles, FileOperations.ClipboardMode.COPY)
                        clipboardState = FileOperations.currentClipboard
                        selectedPaths = emptySet()
                    },
                    onCut = {
                        val selectedFiles = files.filter { selectedPaths.contains(it.path) }.map { it.file }
                        FileOperations.currentClipboard = FileOperations.ClipboardItem(selectedFiles, FileOperations.ClipboardMode.CUT)
                        clipboardState = FileOperations.currentClipboard
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
                    onZip = {
                        scope.launch {
                            val selectedFiles = files.filter { selectedPaths.contains(it.path) }.map { it.file }
                            val zipName = if (selectedFiles.size == 1) "${selectedFiles.first().nameWithoutExtension}.zip" else "Arquivo.zip"
                            FileOperations.zipFiles(selectedFiles, File(currentDir, zipName))
                            refreshFiles()
                        }
                    }
                )
            } else {
                // ES Bottom Bar with Visualizar toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF191B20))
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExplorerBottomActionItem("Novo", R.drawable.toolbar_new) {
                        newFolderName = ""
                        showNewFolderDialog = true
                    }
                    ExplorerBottomActionItem("Procurar", R.drawable.toolbar_search) {}
                    ExplorerBottomActionItem("Atualizar", R.drawable.toolbar_refresh) { refreshFiles() }
                    ExplorerBottomActionItem("Visualizar", R.drawable.toolbar_view) {
                        isGridView = !isGridView
                    }
                    ExplorerBottomActionItem("Mais", R.drawable.toolbar_more) {}
                }
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
                Column(modifier = Modifier.padding(16.dp)) {
                    TextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(text = "Cancelar", onClick = { showNewFolderDialog = false })
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newFolderName.isNotBlank()) {
                                    scope.launch {
                                        FileOperations.createDirectory(currentDir, newFolderName.trim())
                                        showNewFolderDialog = false
                                        refreshFiles()
                                    }
                                }
                            }
                        ) {
                            Text("Criar")
                        }
                    }
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
                Column(modifier = Modifier.padding(16.dp)) {
                    TextField(
                        value = renameFileName,
                        onValueChange = { renameFileName = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(text = "Cancelar", onClick = { showRenameDialog = false })
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val selected = files.firstOrNull { selectedPaths.contains(it.path) }
                                if (selected != null && renameFileName.isNotBlank()) {
                                    scope.launch {
                                        FileOperations.rename(selected.file, renameFileName.trim())
                                        showRenameDialog = false
                                        refreshFiles()
                                    }
                                }
                            }
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        OverlayDialog(
            show = showDeleteDialog,
            onDismissRequest = { showDeleteDialog = false },
            title = "Excluir Arquivos",
            content = {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tem certeza que deseja excluir ${selectedPaths.size} item(ns) permanentemente?",
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(text = "Cancelar", onClick = { showDeleteDialog = false })
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    val filesToDelete = files.filter { selectedPaths.contains(it.path) }.map { it.file }
                                    FileOperations.deleteFiles(filesToDelete)
                                    showDeleteDialog = false
                                    refreshFiles()
                                }
                            }
                        ) {
                            Text("Excluir")
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun ExplorerBottomActionItem(
    title: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 10.dp)
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
            fontWeight = FontWeight.Normal
        )
    }
}
