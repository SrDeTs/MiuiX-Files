package top.yukonga.miuix.files.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.yukonga.miuix.files.core.model.FileItem
import top.yukonga.miuix.files.core.model.SortOption
import top.yukonga.miuix.files.core.storage.FileOperations
import top.yukonga.miuix.files.core.storage.StorageManager
import top.yukonga.miuix.files.ui.components.*
import top.yukonga.miuix.files.ui.theme.MiuiXFilesPreferences
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
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

    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = if (isSelectionMode) "${selectedPaths.size} selecionados" else currentDir.name.ifEmpty { "Arquivos" },
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (isSelectionMode) selectedPaths = emptySet()
                            else {
                                val parent = currentDir.parentFile
                                if (parent != null && parent.canRead()) currentDir = parent
                                else onNavigateBack()
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(text = "←", fontSize = 20.sp)
                }
            },
            actions = {
                if (isSelectionMode) {
                    // Select all / Invert
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedPaths = if (selectedPaths.size == displayFiles.size) {
                                    emptySet()
                                } else {
                                    displayFiles.map { it.path }.toSet()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (selectedPaths.size == displayFiles.size) "Desmarcar" else "Todos",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.primary
                        )
                    }
                } else {
                    // New folder
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                newFolderName = ""
                                showNewFolderDialog = true
                            }
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Text(text = "➕", fontSize = 16.sp)
                    }
                    // Toggle hidden
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showHidden = !showHidden
                                prefs.edit().putBoolean(MiuiXFilesPreferences.KEY_SHOW_HIDDEN, showHidden).apply()
                            }
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Text(text = if (showHidden) "👁️" else "🙈", fontSize = 16.sp)
                    }
                }
            }
        )

        // Breadcrumbs
        BreadcrumbBar(
            currentDir = currentDir,
            onNavigate = { target -> currentDir = target }
        )

        // File List
        Box(modifier = Modifier.weight(1f)) {
            if (displayFiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Esta pasta está vazia.",
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = if (isSelectionMode || clipboardState != null) 90.dp else 16.dp)
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
                                    if (item.isDirectory) {
                                        currentDir = item.file
                                    } else {
                                        onOpenFile(item.file)
                                    }
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    selectedPaths = setOf(item.path)
                                }
                            }
                        )
                    }
                }
            }

            // Floating Bottom Bar when items are selected
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
                    onDelete = {
                        showDeleteDialog = true
                    },
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
                            val destZip = File(currentDir, zipName)
                            FileOperations.zipFiles(selectedFiles, destZip)
                            refreshFiles()
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else if (clipboardState != null) {
                // Clipboard paste bar
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
                        color = MiuixTheme.colorScheme.onSurface
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
