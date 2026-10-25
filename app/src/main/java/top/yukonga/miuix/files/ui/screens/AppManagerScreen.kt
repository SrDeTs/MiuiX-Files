package top.yukonga.miuix.files.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.yukonga.miuix.files.core.apps.AppManager
import top.yukonga.miuix.files.core.apps.InstalledAppItem
import top.yukonga.miuix.files.core.storage.StorageManager
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun AppManagerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cached = remember { AppManager.getCachedApps() }
    var allApps by remember { mutableStateOf(cached ?: emptyList()) }
    var isLoading by remember { mutableStateOf(cached == null) }
    var filterUserOnly by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<InstalledAppItem?>(null) }
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (cached == null) isLoading = true
        allApps = AppManager.getInstalledApps(context)
        isLoading = false
    }

    val filteredApps = remember(allApps, filterUserOnly, searchQuery) {
        allApps.filter { app ->
            (!filterUserOnly || !app.isSystemApp) &&
                    (searchQuery.isBlank() || app.appName.contains(searchQuery, ignoreCase = true) || app.packageName.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(modifier = modifier.fillMaxSize().background(top.yukonga.miuix.files.ui.components.EsBgColor)) {
        Row(
            Modifier.fillMaxWidth().background(top.yukonga.miuix.files.ui.components.EsTopColor).statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onNavigateBack), contentAlignment = Alignment.Center) {
                Image(androidx.compose.ui.res.painterResource(top.yukonga.miuix.files.R.drawable.toolbar_return), null, Modifier.size(24.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(top.yukonga.miuix.files.ui.components.EsTextPrimary))
            }
            Spacer(Modifier.width(8.dp))
            Text("Gerenciador de APPs (${filteredApps.size})", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = top.yukonga.miuix.files.ui.components.EsTextPrimary, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                    .clickable { filterUserOnly = !filterUserOnly }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (filterUserOnly) "Usuário" else "Todos",
                    fontSize = 12.sp,
                    color = top.yukonga.miuix.files.ui.components.EsTextPrimary
                )
            }
        }

        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "Buscar aplicativo instalado...",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Carregando aplicativos instalados...",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().overScrollVertical(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedApp = app }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // App Icon
                            AppIconImage(iconBitmap = app.iconBitmap, drawable = app.icon)

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${app.versionName} • ${StorageManager.formatFileSize(app.apkSize)}",
                                    fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }

                            if (app.isSystemApp) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MiuixTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Sistema",
                                        fontSize = 10.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // App Action Dialog
    selectedApp?.let { app ->
        OverlayDialog(
            show = true,
            onDismissRequest = { selectedApp = null },
            title = app.appName,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Pacote: ${app.packageName}",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    Text(
                        text = "Versão: ${app.versionName}",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    Text(
                        text = "Tamanho do APK: ${StorageManager.formatFileSize(app.apkSize)}",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )

                    if (backupStatusMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = backupStatusMessage!!,
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            text = "Backup APK",
                            onClick = {
                                scope.launch {
                                    val res = AppManager.backupApp(context, app)
                                    backupStatusMessage = if (res.isSuccess) {
                                        "Salvo em: ${res.getOrNull()?.name}"
                                    } else {
                                        "Erro ao extrair APK"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        TextButton(
                            text = "Abrir",
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                            onClick = {
                                AppManager.launchApp(context, app.packageName)
                                selectedApp = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!app.isSystemApp) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            text = "Desinstalar",
                            colors = ButtonDefaults.textButtonColorsPrimary(
                                color = androidx.compose.ui.graphics.Color(0xFFE53935),
                                textColor = androidx.compose.ui.graphics.Color.White
                            ),
                            onClick = {
                                AppManager.uninstallApp(context, app.packageName)
                                selectedApp = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun AppIconImage(iconBitmap: androidx.compose.ui.graphics.ImageBitmap?, drawable: Drawable?) {
    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap,
            contentDescription = null,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        return
    }
    if (drawable == null) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MiuixTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "📱", fontSize = 20.sp)
        }
        return
    }

    val bitmap = remember(drawable) {
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bmp.asImageBitmap()
    }

    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
    )
}
