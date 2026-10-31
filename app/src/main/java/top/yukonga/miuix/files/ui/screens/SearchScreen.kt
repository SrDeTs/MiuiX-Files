package top.yukonga.miuix.files.ui.screens

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.files.R
import top.yukonga.miuix.files.core.model.FileItem
import top.yukonga.miuix.files.core.storage.FileOperations
import top.yukonga.miuix.files.core.storage.StorageManager
import top.yukonga.miuix.files.ui.components.*
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket

/** Procurar: busca rápida por nome via MediaStore + diretórios principais. */
@Composable
fun SearchScreen(onBack: () -> Unit, onOpen: (FileItem) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    LaunchedEffect(query) {
        if (query.length < 2) { results = emptyList(); return@LaunchedEffect }
        delay(120)
        searching = true
        val q = query.trim().lowercase()
        results = withContext(Dispatchers.IO) {
            val out = ArrayList<FileItem>()
            val seen = HashSet<String>()
            // 1. Consulta indexada no MediaStore (<15ms)
            runCatching {
                val uri = android.provider.MediaStore.Files.getContentUri("external")
                val proj = arrayOf(
                    android.provider.MediaStore.MediaColumns.DATA,
                    android.provider.MediaStore.MediaColumns.SIZE,
                    android.provider.MediaStore.MediaColumns.DATE_MODIFIED
                )
                context.contentResolver.query(
                    uri, proj,
                    "${android.provider.MediaStore.MediaColumns.DATA} LIKE ?",
                    arrayOf("%$q%"),
                    "${android.provider.MediaStore.MediaColumns.DATE_MODIFIED} DESC"
                )?.use { c ->
                    val dCol = c.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATA)
                    while (c.moveToNext() && out.size < 200) {
                        val p = c.getString(dCol) ?: continue
                        val f = File(p)
                        if (f.name.lowercase().contains(q) && f.exists() && seen.add(p)) {
                            out.add(FileItem(f))
                        }
                    }
                }
            }
            // 2. Complementa com busca rasa (até 2 níveis) na raiz
            Environment.getExternalStorageDirectory().walkTopDown()
                .maxDepth(2)
                .onEnter { !it.name.startsWith(".") && it.name != "Android" }
                .filter { it.name.lowercase().contains(q) && seen.add(it.absolutePath) }
                .take(100)
                .forEach { out.add(FileItem(it)) }
            out
        }
        searching = false
    }

    EsSubScreen("Procurar", onBack) {
        TextField(
            value = query, onValueChange = { query = it }, label = "Digite o nome do arquivo",
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        )
        if (searching) Text("Procurando...", color = Color(0xFFA0A3A6), fontSize = 13.sp, modifier = Modifier.padding(16.dp))
        else if (query.length >= 2 && results.isEmpty()) Text("Nenhum resultado", color = Color(0xFFA0A3A6), fontSize = 13.sp, modifier = Modifier.padding(16.dp))
        FileRows(results, onOpen)
    }
}

/** Histórico: arquivos modificados recentemente. */
@Composable
fun HistoryScreen(onBack: () -> Unit, onOpen: (FileItem) -> Unit) {
    val context = LocalContext.current
    var files by remember { mutableStateOf(StorageManager.getCachedRecents()) }
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { files = StorageManager.getRecentFiles(limit = 200, context = context) }
    EsSubScreen("Histórico", onBack) {
        if (files == null) Text("Carregando...", color = Color(0xFFA0A3A6), fontSize = 13.sp, modifier = Modifier.padding(16.dp))
        FileRows(files.orEmpty(), onOpen)
    }
}

@Composable
private fun ColumnScope.FileRows(list: List<FileItem>, onOpen: (FileItem) -> Unit) {
    LazyColumn(Modifier.weight(1f)) {
        items(list, key = { it.path }) { f ->
            Row(
                Modifier.fillMaxWidth().clickable { onOpen(f) }.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FileThumbnail(f.file, iconFor(f), Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(f.name, fontSize = 14.sp, color = EsTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${relativeTime(f.lastModified)} • ${if (f.isDirectory) "Pasta" else StorageManager.formatFileSize(f.size)} • ${f.file.parent}",
                        fontSize = 11.sp, color = EsTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun EsSubScreen(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(EsBgColor)) {
        Row(
            Modifier.fillMaxWidth().background(EsTopColor).statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.toolbar_return), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(EsTextPrimary))
            }
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = EsTextPrimary)
        }
        Column(Modifier.weight(1f).navigationBarsPadding(), content = content)
    }
}

/** ES Note Editor: editor de texto embutido (lê e salva arquivos de texto ou cria notas novas). */
@Composable
fun TextEditorScreen(initialFile: File?, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var targetFile by remember {
        mutableStateOf(
            initialFile ?: File(Environment.getExternalStorageDirectory(), "Nota_${System.currentTimeMillis() / 1000}.txt")
        )
    }
    var fileName by remember { mutableStateOf(targetFile.name) }
    var text by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    LaunchedEffect(initialFile) {
        if (initialFile != null && initialFile.exists() && initialFile.isFile) {
            text = withContext(Dispatchers.IO) {
                runCatching { initialFile.readText() }.getOrDefault("")
            }
        }
        loaded = true
    }

    val lineCount = remember(text) { if (text.isEmpty()) 1 else text.count { it == '\n' } + 1 }

    EsSubScreen("ES Note Editor", onBack) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = "Arquivo",
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    scope.launch {
                        val dest = if (initialFile != null) File(initialFile.parentFile, fileName.trim())
                        else File(Environment.getExternalStorageDirectory(), fileName.trim())
                        val ok = withContext(Dispatchers.IO) {
                            runCatching { dest.writeText(text); true }.getOrDefault(false)
                        }
                        if (ok) {
                            targetFile = dest
                            StorageManager.invalidateCaches()
                            Toast.makeText(context, "Salvo em ${dest.absolutePath}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Erro ao salvar arquivo", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Salvar")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$lineCount linhas • ${text.length} caracteres",
                    fontSize = 11.5.sp,
                    color = EsTextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Copiar Tudo",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            cm?.setPrimaryClip(android.content.ClipData.newPlainText("note", text))
                            Toast.makeText(context, "Texto copiado", Toast.LENGTH_SHORT).show()
                        }
                    )
                    Text(
                        text = "Compartilhar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            runCatching {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, fileName)
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartilhar $fileName"))
                            }
                        }
                    )
                }
            }
            if (loaded) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    label = "Conteúdo do arquivo",
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }
    }
}

/**
 * ES Image Browser 1:1 — feito do zero:
 * - Deslize lateral (HorizontalPager) entre todas as imagens da pasta
 * - Pinça para zoom (1x..5x), arrastar (pan) e toque duplo para zoom 2.5x / 1x
 * - Toque simples alterna modo tela cheia (oculta/mostra barras superior e inferior)
 * - Barra inferior 1:1 com ícones ES: Girar, Definir como, Compartilhar, Excluir, Propriedades
 */
@Composable
fun ImageViewerScreen(file: File, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imgExtensions = remember { setOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "heic") }

    var siblingImages by remember(file.path) {
        mutableStateOf(
            file.parentFile?.listFiles()
                ?.filter { it.isFile && it.extension.lowercase() in imgExtensions }
                ?.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                ?.takeIf { it.isNotEmpty() }
                ?: listOf(file)
        )
    }

    val initialIdx = remember(siblingImages, file.path) {
        siblingImages.indexOfFirst { it.absolutePath == file.absolutePath }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = initialIdx,
        pageCount = { siblingImages.size }
    )

    val currentFile = siblingImages.getOrNull(pagerState.currentPage) ?: file
    var showBars by remember { mutableStateOf(true) }
    var isCurrentPageZoomed by remember { mutableStateOf(false) }
    var rotationMap by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var resolutionMap by remember { mutableStateOf<Map<String, Pair<Int, Int>>>(emptyMap()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPropsDialog by remember { mutableStateOf(false) }

    val currentRotation = rotationMap[currentFile.absolutePath] ?: 0f
    val animatedRotation by animateFloatAsState(
        targetValue = currentRotation,
        animationSpec = folmeSpring(damping = 0.85f, response = 0.28f),
        label = "img_rotation"
    )

    LaunchedEffect(pagerState.currentPage) {
        isCurrentPageZoomed = false
    }

    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isCurrentPageZoomed,
            beyondViewportPageCount = 1
        ) { page ->
            val pageFile = siblingImages.getOrNull(page) ?: return@HorizontalPager
            val isCurrentPage = page == pagerState.currentPage
            var bmp by remember(pageFile.absolutePath) { mutableStateOf<ImageBitmap?>(null) }
            var scale by remember(pageFile.absolutePath, isCurrentPage) { mutableFloatStateOf(1f) }
            var offsetX by remember(pageFile.absolutePath, isCurrentPage) { mutableFloatStateOf(0f) }
            var offsetY by remember(pageFile.absolutePath, isCurrentPage) { mutableFloatStateOf(0f) }

            LaunchedEffect(scale, isCurrentPage) {
                if (isCurrentPage) {
                    isCurrentPageZoomed = scale > 1.02f
                }
            }

            val pageRotation = if (isCurrentPage) animatedRotation else (rotationMap[pageFile.absolutePath] ?: 0f)

            LaunchedEffect(pageFile.absolutePath) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        android.graphics.BitmapFactory.decodeFile(pageFile.absolutePath, bounds)
                        if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                            resolutionMap = resolutionMap + (pageFile.absolutePath to (bounds.outWidth to bounds.outHeight))
                        }
                        var sample = 1
                        while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) {
                            sample *= 2
                        }
                        val decoded = android.graphics.BitmapFactory.decodeFile(
                            pageFile.absolutePath,
                            android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
                        )?.asImageBitmap()
                        bmp = decoded
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(pageFile.absolutePath) {
                        detectTapGestures(
                            onTap = { showBars = !showBars },
                            onDoubleTap = {
                                if (scale > 1.05f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    }
                    .pointerInput(pageFile.absolutePath) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                val pointerCount = event.changes.count { it.pressed }
                                if (pointerCount >= 2 || scale > 1.01f) {
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    if (zoomChange != 1f || panChange != androidx.compose.ui.geometry.Offset.Zero) {
                                        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                        scale = newScale
                                        if (newScale > 1.01f) {
                                            val maxX = (size.width * (newScale - 1f)) / 2f
                                            val maxY = (size.height * (newScale - 1f)) / 2f
                                            offsetX = (offsetX + panChange.x).coerceIn(-maxX, maxX)
                                            offsetY = (offsetY + panChange.y).coerceIn(-maxY, maxY)
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                        event.changes.forEach { change ->
                                            if (change.position != change.previousPosition) {
                                                change.consume()
                                            }
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val bitmap = bmp
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = pageFile.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY,
                                rotationZ = pageRotation
                            )
                    )
                } else {
                    Text(
                        text = "Carregando imagem...",
                        color = Color(0xFF9EA2A8),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Top Bar Overlay (ES Image Browser Header)
        AnimatedVisibility(
            visible = showBars,
            enter = slideInVertically(animationSpec = folmeSpring(damping = 0.88f, response = 0.28f)) { -it } + fadeIn(),
            exit = slideOutVertically(animationSpec = folmeSpring(damping = 0.9f, response = 0.22f)) { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xD9202326))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.toolbar_return),
                        contentDescription = "Voltar",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentFile.name,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val res = resolutionMap[currentFile.absolutePath]
                    val resText = if (res != null) "${res.first}×${res.second} • " else ""
                    Text(
                        text = "$resText${StorageManager.formatFileSize(currentFile.length())} • ${pagerState.currentPage + 1}/${siblingImages.size}",
                        fontSize = 11.5.sp,
                        color = Color(0xFFB8BCC2),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Bottom Action Toolbar Overlay (1:1 ES Image Browser: Girar, Definir como, Compartilhar, Excluir, Propriedades)
        AnimatedVisibility(
            visible = showBars,
            enter = slideInVertically(animationSpec = folmeSpring(damping = 0.88f, response = 0.28f)) { it } + fadeIn(),
            exit = slideOutVertically(animationSpec = folmeSpring(damping = 0.9f, response = 0.22f)) { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xD9202326))
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0x26FFFFFF))
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ImageViewerBottomAction(
                        title = "Girar",
                        iconRes = R.drawable.toolbar_refresh,
                        modifier = Modifier.weight(1f)
                    ) {
                        val nextRot = (currentRotation + 90f) % 360f
                        rotationMap = rotationMap + (currentFile.absolutePath to nextRot)
                    }
                    ImageViewerBottomAction(
                        title = "Definir como",
                        iconRes = R.drawable.toolbar_image,
                        modifier = Modifier.weight(1f)
                    ) {
                        runCatching {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", currentFile)
                            val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                                setDataAndType(uri, "image/*")
                                putExtra("mimeType", "image/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Definir como"))
                        }
                    }
                    ImageViewerBottomAction(
                        title = "Compartilhar",
                        iconRes = R.drawable.toolbar_share,
                        modifier = Modifier.weight(1f)
                    ) {
                        runCatching {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", currentFile)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Compartilhar ${currentFile.name}"))
                        }
                    }
                    ImageViewerBottomAction(
                        title = "Excluir",
                        iconRes = R.drawable.toolbar_delete,
                        modifier = Modifier.weight(1f)
                    ) {
                        showDeleteConfirm = true
                    }
                    ImageViewerBottomAction(
                        title = "Propriedades",
                        iconRes = R.drawable.toolbar_more,
                        modifier = Modifier.weight(1f)
                    ) {
                        showPropsDialog = true
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        val prefs = remember(context) { context.getSharedPreferences(top.yukonga.miuix.files.ui.theme.MiuiXFilesPreferences.PREFS_NAME, Context.MODE_PRIVATE) }
        val defaultRecycle = remember(prefs) { prefs.getBoolean(top.yukonga.miuix.files.ui.theme.MiuiXFilesPreferences.KEY_USE_RECYCLE_BIN, true) }
        var moveToRecycle by remember(defaultRecycle) { mutableStateOf(defaultRecycle) }
        OverlayDialog(
            show = showDeleteConfirm,
            onDismissRequest = { showDeleteConfirm = false },
            title = "Excluir",
            summary = if (moveToRecycle) {
                "Mover \"${currentFile.name}\" para a Reciclagem?"
            } else {
                "Tem certeza de que deseja excluir permanentemente \"${currentFile.name}\"?"
            },
            content = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { moveToRecycle = !moveToRecycle }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    top.yukonga.miuix.kmp.basic.Checkbox(
                        state = if (moveToRecycle) androidx.compose.ui.state.ToggleableState.On else androidx.compose.ui.state.ToggleableState.Off,
                        onClick = { moveToRecycle = !moveToRecycle }
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
                            text = if (moveToRecycle) "Pode ser restaurado depois" else "Desmarcado: exclusão direta e permanente",
                            fontSize = 11.5.sp,
                            color = if (moveToRecycle) EsTextSecondary else Color(0xFFFF6B6B)
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
                        onClick = { showDeleteConfirm = false },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        text = if (moveToRecycle) "Mover" else "Excluir",
                        colors = ButtonDefaults.textButtonColorsPrimary(
                            color = if (moveToRecycle) Color(0xFF2F7DF6) else Color(0xFFE53935),
                            textColor = Color.White
                        ),
                        onClick = {
                            val target = currentFile
                            val recycle = moveToRecycle
                            showDeleteConfirm = false
                            scope.launch {
                                if (recycle) {
                                    FileOperations.moveToRecycle(listOf(target))
                                } else {
                                    FileOperations.deleteFiles(listOf(target))
                                }
                                val updated = siblingImages.filter { it.absolutePath != target.absolutePath }
                                if (updated.isEmpty()) {
                                    onBack()
                                } else {
                                    siblingImages = updated
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        )
    }

    if (showPropsDialog) {
        val res = resolutionMap[currentFile.absolutePath]
        OverlayDialog(
            show = showPropsDialog,
            onDismissRequest = { showPropsDialog = false },
            title = "Propriedades da Imagem",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nome: ${currentFile.name}", fontSize = 13.5.sp, color = EsTextPrimary, fontWeight = FontWeight.Bold)
                    Text("Caminho: ${currentFile.absolutePath}", fontSize = 12.sp, color = EsTextSecondary)
                    if (res != null) {
                        Text("Resolução: ${res.first} × ${res.second} px", fontSize = 12.sp, color = EsTextSecondary)
                    }
                    Text("Tamanho: ${StorageManager.formatFileSize(currentFile.length())} (${currentFile.length()} B)", fontSize = 12.sp, color = EsTextSecondary)
                    Text("Modificado: ${StorageManager.formatDate(currentFile.lastModified())}", fontSize = 12.sp, color = EsTextSecondary)
                    Spacer(Modifier.height(10.dp))
                    TextButton(
                        text = "Fechar",
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = { showPropsDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}

@Composable
private fun ImageViewerBottomAction(
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
            .pressable(interactionSource = interactionSource, indication = SinkFeedback())
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = title,
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.92f))
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = title,
            fontSize = 10.5.sp,
            color = Color(0xFFD0D4DA),
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** ES Zip Viewer: inspeciona entradas de um arquivo .zip e extrai com 1 toque. */
@Composable
fun ZipViewerScreen(zipFile: File, onBack: () -> Unit, onNavigateDir: (File) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var extracting by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    LaunchedEffect(zipFile.path) {
        entries = withContext(Dispatchers.IO) {
            val out = ArrayList<Pair<String, Long>>()
            runCatching {
                java.util.zip.ZipFile(zipFile).use { zf ->
                    val e = zf.entries()
                    while (e.hasMoreElements() && out.size < 500) {
                        val ze = e.nextElement()
                        out.add(ze.name to ze.size.coerceAtLeast(0L))
                    }
                }
            }
            out
        }
        loading = false
    }

    EsSubScreen(zipFile.name, onBack) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${entries.size} itens no arquivo", fontSize = 13.sp, color = Color(0xFFA0A3A6))
            Button(
                enabled = !extracting,
                onClick = {
                    scope.launch {
                        extracting = true
                        val targetDir = File(zipFile.parentFile, zipFile.nameWithoutExtension)
                        val res = top.yukonga.miuix.files.core.storage.FileOperations.unzip(zipFile, targetDir)
                        extracting = false
                        if (res.isSuccess) {
                            Toast.makeText(context, "Extraído para ${targetDir.name}", Toast.LENGTH_SHORT).show()
                            onNavigateDir(targetDir)
                        } else {
                            Toast.makeText(context, "Falha ao extrair arquivo", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ) {
                Text(if (extracting) "Extraindo..." else "Extrair Aqui")
            }
        }
        if (loading) {
            Text("Lendo arquivo compactado...", color = Color(0xFFA0A3A6), fontSize = 13.sp, modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(entries, key = { it.first }) { (name, size) ->
                    val isDir = name.endsWith("/")
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(painterResource(if (isDir) R.drawable.format_folder else R.drawable.format_document), null, Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name.trimEnd('/'), fontSize = 13.5.sp, color = EsTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (!isDir) Text(StorageManager.formatFileSize(size), fontSize = 11.sp, color = EsTextSecondary)
                        }
                    }
                }
            }
        }
    }
}

/** ES Audio Player: reprodutor de música embutido. */
@Composable
fun AudioPlayerScreen(file: File, onBack: () -> Unit) {
    var playing by remember { mutableStateOf(false) }
    var durationMs by remember { mutableIntStateOf(0) }
    var posMs by remember { mutableIntStateOf(0) }
    val player = remember { android.media.MediaPlayer() }

    BackHandler {
        runCatching { player.stop(); player.release() }
        onBack()
    }
    DisposableEffect(file.path) {
        runCatching {
            player.setDataSource(file.path)
            player.prepare()
            durationMs = player.duration
            player.start()
            playing = true
            player.setOnCompletionListener { playing = false }
        }
        onDispose {
            runCatching { player.release() }
        }
    }
    LaunchedEffect(playing) {
        while (playing) {
            posMs = runCatching { player.currentPosition }.getOrDefault(0)
            delay(300)
        }
    }

    fun fmt(ms: Int): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return String.format("%02d:%02d", s / 60, s % 60)
    }

    EsSubScreen("ES Media Player", onBack) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                insideMargin = PaddingValues(24.dp),
                colors = CardDefaults.defaultColors(color = EsCardColor, contentColor = EsTextPrimary)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Image(painterResource(R.drawable.es_cat_music), null, Modifier.size(72.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(file.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EsTextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Text("${fmt(posMs)} / ${fmt(durationMs)}", fontSize = 13.sp, color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary)
                    Spacer(Modifier.height(14.dp))
                    top.yukonga.miuix.kmp.basic.LinearProgressIndicator(
                        progress = if (durationMs > 0) (posMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                        height = 5.dp
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                runCatching {
                                    val next = (player.currentPosition - 10_000).coerceAtLeast(0)
                                    player.seekTo(next)
                                    posMs = next
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("-10s")
                        }
                        Button(
                            onClick = {
                                runCatching {
                                    if (player.isPlaying) {
                                        player.pause()
                                        playing = false
                                    } else {
                                        player.start()
                                        playing = true
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.4f)
                        ) {
                            Text(if (playing) "Pausar" else "Reproduzir")
                        }
                        Button(
                            onClick = {
                                runCatching {
                                    val next = (player.currentPosition + 10_000).coerceAtMost(durationMs)
                                    player.seekTo(next)
                                    posMs = next
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+10s")
                        }
                    }
                }
            }
        }
    }
}

private fun getLocalIpAddress(): String {
    runCatching {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val intf = interfaces.nextElement()
            val addrs = intf.inetAddresses
            while (addrs.hasMoreElements()) {
                val addr = addrs.nextElement()
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    return addr.hostAddress ?: "127.0.0.1"
                }
            }
        }
    }
    return "127.0.0.1"
}

/** Ver no PC: servidor HTTP local real para navegar e baixar arquivos do celular pelo navegador do PC. */
@Composable
fun ViewOnPcScreen(onBack: () -> Unit) {
    var running by remember { mutableStateOf(false) }
    var serverSocket by remember { mutableStateOf<ServerSocket?>(null) }
    val ip = remember { getLocalIpAddress() }
    val port = 8080
    val scope = rememberCoroutineScope()

    BackHandler {
        runCatching { serverSocket?.close() }
        onBack()
    }
    DisposableEffect(Unit) {
        onDispose { runCatching { serverSocket?.close() } }
    }

    fun startServer() {
        running = true
        scope.launch(Dispatchers.IO) {
            runCatching {
                val ss = ServerSocket(port)
                serverSocket = ss
                val root = Environment.getExternalStorageDirectory()
                while (!ss.isClosed) {
                    val client = ss.accept()
                    launch(Dispatchers.IO) {
                        runCatching {
                            val reader = client.getInputStream().bufferedReader()
                            val requestLine = reader.readLine() ?: ""
                            val rawPath = requestLine.split(" ").getOrNull(1)?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: "/"
                            val target = if (rawPath == "/" || rawPath.isEmpty()) root else File(root, rawPath.removePrefix("/"))
                            val out = client.getOutputStream()
                            if (target.isDirectory) {
                                val items = target.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })).orEmpty()
                                val html = buildString {
                                    append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>MiuiX Files - Ver no PC</title>")
                                    append("<style>body{background:#292C2E;color:#fff;font-family:sans-serif;padding:24px}a{color:#3FE0F0;text-decoration:none}li{padding:6px 0;border-bottom:1px solid #383C42}</style></head><body>")
                                    append("<h2>📁 ${target.absolutePath}</h2><ul>")
                                    if (target != root) {
                                        val parentRel = target.parentFile?.absolutePath?.removePrefix(root.absolutePath) ?: "/"
                                        append("<li><a href='${if (parentRel.isEmpty()) "/" else parentRel}'>⬅ .. (Pasta anterior)</a></li>")
                                    }
                                    for (f in items) {
                                        val rel = f.absolutePath.removePrefix(root.absolutePath)
                                        val icon = if (f.isDirectory) "📁" else "📄"
                                        append("<li>$icon <a href='$rel'>${f.name}</a> (${if (f.isDirectory) "Pasta" else StorageManager.formatFileSize(f.length())})</li>")
                                    }
                                    append("</ul></body></html>")
                                }
                                val bytes = html.toByteArray()
                                out.write("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\n\r\n".toByteArray())
                                out.write(bytes)
                            } else if (target.isFile && target.exists()) {
                                out.write("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Disposition: attachment; filename=\"${target.name}\"\r\nContent-Length: ${target.length()}\r\n\r\n".toByteArray())
                                target.inputStream().use { it.copyTo(out) }
                            } else {
                                out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 9\r\n\r\nNot Found".toByteArray())
                            }
                            out.flush()
                            client.close()
                        }
                    }
                }
            }.onFailure {
                running = false
            }
        }
    }

    fun stopServer() {
        runCatching { serverSocket?.close() }
        serverSocket = null
        running = false
    }

    EsSubScreen("Ver no PC", onBack) {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                Modifier.fillMaxWidth(),
                cornerRadius = 12.dp,
                insideMargin = PaddingValues(20.dp),
                colors = CardDefaults.defaultColors(color = EsCardColor, contentColor = EsTextPrimary)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Image(painterResource(R.drawable.es_tool_viewpc), null, Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (running) "Servidor ativo na rede Wi-Fi" else "Servidor desligado",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EsTextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (running) "Abra no navegador do PC:\nhttp://$ip:$port"
                        else "Toque em Iniciar para acessar seus arquivos do celular no navegador do computador.",
                        fontSize = 13.sp, color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { if (running) stopServer() else startServer() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (running) "Parar Servidor" else "Iniciar Servidor")
                    }
                }
            }
        }
    }
}

/** Rede / Unidade de Nuvem: exibe conexões de rede local (LAN/SMB/FTP/WebDAV) e status do Wi-Fi. */
@Composable
fun NetworkScreen(title: String, onBack: () -> Unit, onOpenPcServer: () -> Unit) {
    val ip = remember { getLocalIpAddress() }
    val interfaces = remember {
        runCatching {
            NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback }
        }.getOrDefault(emptyList())
    }
    BackHandler(onBack = onBack)

    EsSubScreen(title, onBack) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    cornerRadius = 10.dp,
                    insideMargin = PaddingValues(16.dp),
                    colors = CardDefaults.defaultColors(color = EsCardColor, contentColor = EsTextPrimary)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Status da Rede Local", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EsTextPrimary)
                        Text("Endereço IPv4: $ip", fontSize = 13.sp, color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary)
                        Text("Interfaces ativas: ${interfaces.joinToString { it.displayName }.ifEmpty { "Wi-Fi / Dados" }}", fontSize = 12.sp, color = EsTextSecondary)
                    }
                }
            }
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    cornerRadius = 10.dp,
                    insideMargin = PaddingValues(16.dp),
                    colors = CardDefaults.defaultColors(color = EsCardColor, contentColor = EsTextPrimary),
                    onClick = onOpenPcServer
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.es_tool_viewpc), null, Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Servidor Remoto (Ver no PC)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EsTextPrimary)
                            Text("Compartilhar arquivos via HTTP na porta 8080", fontSize = 12.sp, color = EsTextSecondary)
                        }
                    }
                }
            }
        }
    }
}

/** Remetente: escolha qualquer arquivo recente ou aplicativo para enviar via compartilhamento Android. */
@Composable
fun SenderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var files by remember { mutableStateOf(StorageManager.getCachedRecents().orEmpty()) }
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { files = StorageManager.getRecentFiles(limit = 100, context = context) }

    EsSubScreen("Remetente (Enviar Arquivos)", onBack) {
        Text(
            "Toque em um arquivo para enviar via Bluetooth, Wi-Fi Direct ou Apps:",
            fontSize = 12.5.sp, color = Color(0xFFA0A3A6),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
        FileRows(files) { item ->
            runCatching {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", item.file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Enviar ${item.name}"))
            }
        }
    }
}
