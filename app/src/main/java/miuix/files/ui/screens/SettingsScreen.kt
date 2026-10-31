package miuix.files.ui.screens

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import miuix.files.R
import miuix.files.core.cleaner.CleanerEngine
import miuix.files.core.storage.FileOperations
import miuix.files.core.storage.StorageManager
import miuix.files.ui.components.EsBgColor
import miuix.files.ui.components.EsCardColor
import miuix.files.ui.components.EsTextPrimary
import miuix.files.ui.components.EsTextSecondary
import miuix.files.ui.components.EsTopColor
import miuix.files.ui.components.clearThumbnailCache
import miuix.files.ui.theme.MiuiXFilesPreferences
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import java.io.File

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { context.getSharedPreferences(MiuiXFilesPreferences.PREFS_NAME, Context.MODE_PRIVATE) }

    BackHandler(onBack = onNavigateBack)

    // States backed by SharedPreferences (0 = ES Dark default)
    var themeMode by remember { mutableIntStateOf(prefs.getInt(MiuiXFilesPreferences.KEY_THEME_MODE, 0).coerceIn(0, 5)) }
    var defaultViewMode by remember { mutableIntStateOf(prefs.getInt(MiuiXFilesPreferences.KEY_DEFAULT_VIEW_MODE, 0)) }
    var defaultSortOption by remember { mutableIntStateOf(prefs.getInt(MiuiXFilesPreferences.KEY_SORT_OPTION, 0)) }
    var showHidden by remember { mutableStateOf(prefs.getBoolean(MiuiXFilesPreferences.KEY_SHOW_HIDDEN, false)) }
    var foldersFirst by remember { mutableStateOf(prefs.getBoolean(MiuiXFilesPreferences.KEY_FOLDERS_FIRST, true)) }
    var pinMediaFolders by remember { mutableStateOf(prefs.getBoolean(MiuiXFilesPreferences.KEY_PIN_MEDIA_FOLDERS, true)) }
    var showThumbnails by remember { mutableStateOf(prefs.getBoolean(MiuiXFilesPreferences.KEY_SHOW_THUMBNAILS, true)) }
    var showFolderAppIcons by remember { mutableStateOf(prefs.getBoolean(MiuiXFilesPreferences.KEY_SHOW_FOLDER_APP_ICONS, true)) }
    var showRecentsHome by remember { mutableStateOf(prefs.getBoolean(MiuiXFilesPreferences.KEY_SHOW_RECENTS_HOME, true)) }
    var useRecycleBin by remember { mutableStateOf(prefs.getBoolean(MiuiXFilesPreferences.KEY_USE_RECYCLE_BIN, true)) }
    var confirmBeforeDelete by remember { mutableStateOf(prefs.getBoolean(MiuiXFilesPreferences.KEY_CONFIRM_BEFORE_DELETE, true)) }
    var rootExplorerEnabled by remember { mutableStateOf(prefs.getBoolean(MiuiXFilesPreferences.KEY_ROOT_EXPLORER, false)) }
    var drawerShowLocal by remember { mutableStateOf(prefs.getBoolean(MiuiXFilesPreferences.KEY_DRAWER_SHOW_LOCAL, false)) }
    var drawerShowLibrary by remember { mutableStateOf(prefs.getBoolean(MiuiXFilesPreferences.KEY_DRAWER_SHOW_LIBRARY, false)) }
    var drawerShowTools by remember { mutableStateOf(prefs.getBoolean(MiuiXFilesPreferences.KEY_DRAWER_SHOW_TOOLS, false)) }
    var backupDirPath by remember { mutableStateOf(prefs.getString(MiuiXFilesPreferences.KEY_BACKUP_DIR, "backups/apps") ?: "backups/apps") }
    var defaultHomeDir by remember { mutableStateOf(prefs.getString(MiuiXFilesPreferences.KEY_DEFAULT_HOME_DIR, "/storage/emulated/0") ?: "/storage/emulated/0") }

    // Live storage & recycle metrics
    var recycleCount by remember { mutableIntStateOf(0) }
    var recycleBytes by remember { mutableLongStateOf(0L) }
    var showBackupDirDialog by remember { mutableStateOf(false) }
    var showHomeDirDialog by remember { mutableStateOf(false) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var showResetSettingsDialog by remember { mutableStateOf(false) }
    var editingPath by remember { mutableStateOf("") }

    fun refreshMetrics() {
        scope.launch(Dispatchers.IO) {
            val files = FileOperations.recycleDir.listFiles().orEmpty().filter { it.name != ".index" }
            val bytes = files.sumOf { if (it.isDirectory) it.walkTopDown().filter { f -> f.isFile }.sumOf { f -> f.length() } else it.length() }
            withContext(Dispatchers.Main) {
                recycleCount = files.size
                recycleBytes = bytes
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshMetrics()
    }

    val themeDropdownNames = remember {
        listOf(
            "ES Dark (Padrão)",
            "MIUIX Escuro",
            "MIUIX Claro",
            "Monet Escuro",
            "Monet Claro",
            "Automático (Sistema)"
        )
    }
    val viewModeItems = remember { listOf("Grade (4 colunas)", "Grade (5 colunas)", "Lista Detalhada") }
    val sortItems = remember { listOf("Nome (A → Z)", "Nome (Z → A)", "Data (Recente)", "Tamanho (Maior)", "Tipo / Extensão") }

    val cardColors = CardDefaults.defaultColors(
        color = EsCardColor,
        contentColor = EsTextPrimary
    )

    Column(modifier = modifier.fillMaxSize().background(EsBgColor)) {
        // Header Bar reactive to current theme
        Row(
            Modifier
                .fillMaxWidth()
                .background(EsTopColor)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onNavigateBack),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.toolbar_return),
                    contentDescription = "Voltar",
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(EsTextPrimary)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Configurações",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = EsTextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. EXIBIÇÃO E ORDENAÇÃO (1 único seletor limpo de Tema + Exibição)
            item {
                SmallTitle(text = "Exibição e Aparência")
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    colors = cardColors
                ) {
                    Column {
                        OverlayDropdownPreference(
                            title = "Tema do Aplicativo",
                            summary = "Estilo visual da interface",
                            items = themeDropdownNames,
                            selectedIndex = themeMode.coerceIn(0, themeDropdownNames.lastIndex),
                            onSelectedIndexChange = { index ->
                                themeMode = index
                                prefs.edit().putInt(MiuiXFilesPreferences.KEY_THEME_MODE, index).apply()
                            }
                        )
                        OverlayDropdownPreference(
                            title = "Modo de Visualização Padrão",
                            summary = "Layout inicial ao abrir pastas no explorador",
                            items = viewModeItems,
                            selectedIndex = defaultViewMode.coerceIn(0, viewModeItems.lastIndex),
                            onSelectedIndexChange = { index ->
                                defaultViewMode = index
                                prefs.edit().putInt(MiuiXFilesPreferences.KEY_DEFAULT_VIEW_MODE, index).apply()
                            }
                        )

                        OverlayDropdownPreference(
                            title = "Ordenação Padrão",
                            summary = "Critério de classificação de pastas e arquivos",
                            items = sortItems,
                            selectedIndex = defaultSortOption.coerceIn(0, sortItems.lastIndex),
                            onSelectedIndexChange = { index ->
                                defaultSortOption = index
                                prefs.edit().putInt(MiuiXFilesPreferences.KEY_SORT_OPTION, index).apply()
                                StorageManager.invalidateCaches()
                            }
                        )

                        SwitchPreference(
                            title = "Mostrar Arquivos Ocultos",
                            summary = "Exibir pastas e arquivos iniciados por ponto (.)",
                            checked = showHidden,
                            onCheckedChange = { checked ->
                                showHidden = checked
                                prefs.edit().putBoolean(MiuiXFilesPreferences.KEY_SHOW_HIDDEN, checked).apply()
                                StorageManager.invalidateCaches()
                            }
                        )

                        SwitchPreference(
                            title = "Listar Pastas Primeiro",
                            summary = "Manter diretórios agrupados antes dos arquivos",
                            checked = foldersFirst,
                            onCheckedChange = { checked ->
                                foldersFirst = checked
                                prefs.edit().putBoolean(MiuiXFilesPreferences.KEY_FOLDERS_FIRST, checked).apply()
                            }
                        )

                        SwitchPreference(
                            title = "Fixar DCIM e Download no Topo",
                            summary = "Priorizar pastas de câmera e downloads no armazenamento interno",
                            checked = pinMediaFolders,
                            onCheckedChange = { checked ->
                                pinMediaFolders = checked
                                prefs.edit().putBoolean(MiuiXFilesPreferences.KEY_PIN_MEDIA_FOLDERS, checked).apply()
                            }
                        )
                    }
                }
            }

            // 3. MINIATURAS E PÁGINA INICIAL
            item {
                SmallTitle(text = "Miniaturas e Página Inicial")
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    colors = cardColors
                ) {
                    Column {
                        SwitchPreference(
                            title = "Miniaturas de Fotos, Vídeos e APKs",
                            summary = "Carregar prévias reais de mídia e ícones de pacotes APK",
                            checked = showThumbnails,
                            onCheckedChange = { checked ->
                                showThumbnails = checked
                                prefs.edit().putBoolean(MiuiXFilesPreferences.KEY_SHOW_THUMBNAILS, checked).apply()
                            }
                        )

                        SwitchPreference(
                            title = "Ícones de Apps nas Pastas",
                            summary = "Exibir selo do aplicativo dono da pasta (ex: MT2, WhatsApp, Telegram)",
                            checked = showFolderAppIcons,
                            onCheckedChange = { checked ->
                                showFolderAppIcons = checked
                                prefs.edit().putBoolean(MiuiXFilesPreferences.KEY_SHOW_FOLDER_APP_ICONS, checked).apply()
                                clearThumbnailCache()
                            }
                        )

                        SwitchPreference(
                            title = "Arquivado Recentemente na Home",
                            summary = "Mostrar linha do tempo de arquivos recentes na tela inicial",
                            checked = showRecentsHome,
                            onCheckedChange = { checked ->
                                showRecentsHome = checked
                                prefs.edit().putBoolean(MiuiXFilesPreferences.KEY_SHOW_RECENTS_HOME, checked).apply()
                            }
                        )
                    }
                }
            }

            // 3.5 MENU LATERAL (GAVETA)
            item {
                SmallTitle(text = "Menu Lateral (Gaveta)")
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    colors = cardColors
                ) {
                    Column {
                        SwitchPreference(
                            title = "Exibir Seção 'Local'",
                            summary = "Mostrar Página Inicial, Interno e Dispositivo na gaveta lateral",
                            checked = drawerShowLocal,
                            onCheckedChange = { checked ->
                                drawerShowLocal = checked
                                prefs.edit().putBoolean(MiuiXFilesPreferences.KEY_DRAWER_SHOW_LOCAL, checked).apply()
                            }
                        )

                        SwitchPreference(
                            title = "Exibir Seção 'Biblioteca'",
                            summary = "Mostrar Fotografias, Música, Filmes, Livros, APP, Download e Reciclagem",
                            checked = drawerShowLibrary,
                            onCheckedChange = { checked ->
                                drawerShowLibrary = checked
                                prefs.edit().putBoolean(MiuiXFilesPreferences.KEY_DRAWER_SHOW_LIBRARY, checked).apply()
                            }
                        )

                        SwitchPreference(
                            title = "Exibir Seção 'Ferramentas'",
                            summary = "Mostrar Cleaner, Analis. de espaço e Gerenciador de APPs na gaveta",
                            checked = drawerShowTools,
                            onCheckedChange = { checked ->
                                drawerShowTools = checked
                                prefs.edit().putBoolean(MiuiXFilesPreferences.KEY_DRAWER_SHOW_TOOLS, checked).apply()
                            }
                        )
                    }
                }
            }

            // 4. DIRETÓRIOS E RECICLAGEM
            item {
                SmallTitle(text = "Diretórios e Reciclagem")
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    colors = cardColors
                ) {
                    Column {
                        SwitchPreference(
                            title = "Ativar Reciclagem por Padrão",
                            summary = "Marcar 'Mover para a Reciclagem' ao excluir arquivos",
                            checked = useRecycleBin,
                            onCheckedChange = { checked ->
                                useRecycleBin = checked
                                prefs.edit().putBoolean(MiuiXFilesPreferences.KEY_USE_RECYCLE_BIN, checked).apply()
                            }
                        )

                        SwitchPreference(
                            title = "Confirmar Antes de Excluir",
                            summary = "Exibir diálogo de segurança antes de remover itens",
                            checked = confirmBeforeDelete,
                            onCheckedChange = { checked ->
                                confirmBeforeDelete = checked
                                prefs.edit().putBoolean(MiuiXFilesPreferences.KEY_CONFIRM_BEFORE_DELETE, checked).apply()
                            }
                        )

                        ArrowPreference(
                            title = "Pasta Padrão da Aba Local",
                            summary = defaultHomeDir,
                            onClick = {
                                editingPath = defaultHomeDir
                                showHomeDirDialog = true
                            }
                        )

                        ArrowPreference(
                            title = "Diretório de Backup de APKs",
                            summary = "/sdcard/$backupDirPath",
                            onClick = {
                                editingPath = backupDirPath
                                showBackupDirDialog = true
                            }
                        )

                        ArrowPreference(
                            title = "Esvaziar Reciclagem Agora",
                            summary = if (recycleCount > 0) "$recycleCount item(ns) • ${StorageManager.formatFileSize(recycleBytes)} ocupados" else "Reciclagem vazia (0 B)",
                            onClick = {
                                if (recycleCount > 0) {
                                    showEmptyTrashDialog = true
                                } else {
                                    Toast.makeText(context, "A Reciclagem já está vazia", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            // 5. FERRAMENTAS, CACHE E AVANÇADO
            item {
                SmallTitle(text = "Ferramentas, Cache e Avançado")
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    colors = cardColors
                ) {
                    Column {
                        SwitchPreference(
                            title = "Explorador Root ( / )",
                            summary = "Permitir navegação completa na partição raiz do sistema",
                            checked = rootExplorerEnabled,
                            onCheckedChange = { checked ->
                                rootExplorerEnabled = checked
                                prefs.edit().putBoolean(MiuiXFilesPreferences.KEY_ROOT_EXPLORER, checked).apply()
                            }
                        )

                        ArrowPreference(
                            title = "Limpar Cache de Miniaturas e Índices",
                            summary = "Libera memória de ícones e força reindexação das pastas",
                            onClick = {
                                val evicted = clearThumbnailCache()
                                StorageManager.invalidateCaches()
                                Toast.makeText(
                                    context,
                                    "Cache limpo ($evicted miniaturas removidas)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )

                        ArrowPreference(
                            title = "Executar Varredura Rápida de Lixo",
                            summary = "Analisar logs, arquivos .tmp e caches residuais",
                            onClick = {
                                scope.launch {
                                    val scan = CleanerEngine.scanJunk(context, forceRefresh = true)
                                    Toast.makeText(
                                        context,
                                        "Encontrados ${StorageManager.formatFileSize(scan.totalReclaimableBytes)} em ${scan.totalFileCount} arquivo(s)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )

                        ArrowPreference(
                            title = "Restaurar Configurações Padrão",
                            summary = "Redefinir todas as preferências para o ES Dark original",
                            onClick = { showResetSettingsDialog = true }
                        )
                    }
                }
            }

            // 6. SOBRE O MIUIX FILES
            item {
                SmallTitle(text = "Sobre")
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    colors = cardColors
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_app_logo),
                                contentDescription = null,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "MiuiX Files",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = EsTextPrimary
                                )
                                Text(
                                    text = "Versão 1.0.0 • MIUIX / ES Dark Edition",
                                    fontSize = 12.5.sp,
                                    color = MiuixTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Gerenciador de arquivos avançado em Jetpack Compose e compose-miuix-ui 0.9.4, com fidelidade 1:1 ao ES File Explorer Dark, temas MIUIX Claro/Escuro e Monet dinâmico do Android.",
                            fontSize = 12.5.sp,
                            color = EsTextSecondary
                        )
                    }
                }
            }
        }
    }

    // Dialog: Pasta Padrão da Aba Local
    if (showHomeDirDialog) {
        OverlayDialog(
            show = showHomeDirDialog,
            onDismissRequest = { showHomeDirDialog = false },
            title = "Pasta Padrão Local",
            summary = "Caminho aberto ao criar uma nova aba Local"
        ) {
            TextField(
                value = editingPath,
                onValueChange = { editingPath = it },
                label = "/storage/emulated/0",
                useLabelAsPlaceholder = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    text = "Cancelar",
                    onClick = { showHomeDirDialog = false },
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = "Salvar",
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        val trimmed = editingPath.trim().ifEmpty { Environment.getExternalStorageDirectory().absolutePath }
                        defaultHomeDir = trimmed
                        prefs.edit().putString(MiuiXFilesPreferences.KEY_DEFAULT_HOME_DIR, trimmed).apply()
                        showHomeDirDialog = false
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Dialog: Diretório de Backup de APKs
    if (showBackupDirDialog) {
        OverlayDialog(
            show = showBackupDirDialog,
            onDismissRequest = { showBackupDirDialog = false },
            title = "Diretório de Backup de APKs",
            summary = "Caminho relativo ao armazenamento interno (/sdcard/)"
        ) {
            TextField(
                value = editingPath,
                onValueChange = { editingPath = it },
                label = "backups/apps",
                useLabelAsPlaceholder = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    text = "Cancelar",
                    onClick = { showBackupDirDialog = false },
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = "Salvar",
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        val trimmed = editingPath.trim().trimStart('/').ifEmpty { "backups/apps" }
                        backupDirPath = trimmed
                        prefs.edit().putString(MiuiXFilesPreferences.KEY_BACKUP_DIR, trimmed).apply()
                        File(Environment.getExternalStorageDirectory(), trimmed).mkdirs()
                        showBackupDirDialog = false
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Dialog: Esvaziar Reciclagem
    if (showEmptyTrashDialog) {
        OverlayDialog(
            show = showEmptyTrashDialog,
            onDismissRequest = { showEmptyTrashDialog = false },
            title = "Esvaziar Reciclagem",
            summary = "Excluir permanentemente $recycleCount item(ns) (${StorageManager.formatFileSize(recycleBytes)}) da Reciclagem?"
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    text = "Cancelar",
                    onClick = { showEmptyTrashDialog = false },
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = "Esvaziar",
                    colors = ButtonDefaults.textButtonColorsPrimary(
                        color = Color(0xFFE53935),
                        textColor = Color.White
                    ),
                    onClick = {
                        showEmptyTrashDialog = false
                        scope.launch {
                            FileOperations.emptyRecycle()
                            refreshMetrics()
                            Toast.makeText(context, "Reciclagem esvaziada com sucesso", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Dialog: Restaurar Padrões
    if (showResetSettingsDialog) {
        OverlayDialog(
            show = showResetSettingsDialog,
            onDismissRequest = { showResetSettingsDialog = false },
            title = "Restaurar Padrões",
            summary = "Deseja redefinir todas as configurações para os valores originais (ES Dark)?"
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    text = "Cancelar",
                    onClick = { showResetSettingsDialog = false },
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = "Redefinir",
                    colors = ButtonDefaults.textButtonColorsPrimary(
                        color = Color(0xFFE53935),
                        textColor = Color.White
                    ),
                    onClick = {
                        prefs.edit().clear().apply()
                        themeMode = 0
                        defaultViewMode = 0
                        defaultSortOption = 0
                        showHidden = false
                        foldersFirst = true
                        pinMediaFolders = true
                        showThumbnails = true
                        showFolderAppIcons = true
                        showRecentsHome = true
                        useRecycleBin = true
                        confirmBeforeDelete = true
                        rootExplorerEnabled = false
                        drawerShowLocal = false
                        drawerShowLibrary = false
                        drawerShowTools = false
                        backupDirPath = "backups/apps"
                        defaultHomeDir = "/storage/emulated/0"
                        showResetSettingsDialog = false
                        Toast.makeText(context, "Configurações restauradas (ES Dark)", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
