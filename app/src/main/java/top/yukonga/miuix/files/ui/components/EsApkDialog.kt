package top.yukonga.miuix.files.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.files.R
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable
import java.io.File
import java.util.Locale

data class ApkParsedDetails(
    val appLabel: String,
    val packageName: String,
    val versionDisplay: String,
    val installedDisplay: String,
    val sizeDisplay: String,
    val icon: ImageBitmap?
)

private fun formatVersionInfo(pkgInfo: PackageInfo): String {
    val verName = pkgInfo.versionName ?: "1.0"
    val verCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        pkgInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        pkgInfo.versionCode.toLong()
    }
    return "$verName ($verCode)"
}

private fun formatPtBrSize(bytes: Long): String {
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb >= 1.0) {
        String.format(Locale("pt", "BR"), "%.2f MB", mb)
    } else {
        val kb = bytes.toDouble() / 1024.0
        String.format(Locale("pt", "BR"), "%.2f KB", kb)
    }
}

private fun parseApkFile(context: Context, apkFile: File): ApkParsedDetails {
    val pm = context.packageManager
    val archiveInfo = runCatching { pm.getPackageArchiveInfo(apkFile.absolutePath, 0) }.getOrNull()
    val appInfo = archiveInfo?.applicationInfo
    if (appInfo != null) {
        appInfo.sourceDir = apkFile.absolutePath
        appInfo.publicSourceDir = apkFile.absolutePath
    }
    val label = appInfo?.let { runCatching { pm.getApplicationLabel(it).toString() }.getOrNull() } ?: apkFile.nameWithoutExtension
    val pkgName = archiveInfo?.packageName ?: "-"
    val verDisplay = archiveInfo?.let { formatVersionInfo(it) } ?: "-"
    val installedInfo = if (pkgName != "-") {
        runCatching { pm.getPackageInfo(pkgName, 0) }.getOrNull()
    } else null
    val installedDisplay = installedInfo?.let { formatVersionInfo(it) } ?: "Não instalado"
    val icon = loadApkIconBitmap(context, apkFile)

    return ApkParsedDetails(
        appLabel = label,
        packageName = pkgName,
        versionDisplay = verDisplay,
        installedDisplay = installedDisplay,
        sizeDisplay = formatPtBrSize(apkFile.length()),
        icon = icon
    )
}

@Composable
fun EsApkPropertiesDialog(
    apkFile: File,
    onDismiss: () -> Unit,
    onViewContent: (File) -> Unit
) {
    val context = LocalContext.current
    var details by remember(apkFile.path) { mutableStateOf<ApkParsedDetails?>(null) }

    LaunchedEffect(apkFile.path) {
        details = withContext(Dispatchers.IO) {
            parseApkFile(context, apkFile)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White)
                .padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            // Title: Propriedades
            Text(
                text = "Propriedades",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Icon + File Name row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val ic = details?.icon
                if (ic != null) {
                    Image(
                        bitmap = ic,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp)
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.format_apk),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = apkFile.nameWithoutExtension,
                    fontSize = 15.sp,
                    color = Color(0xFF616161),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Thin light gray divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFEEEEEE))
            )

            Spacer(modifier = Modifier.height(14.dp))

            val d = details
            ApkDetailRow("Nome:", d?.appLabel ?: "...")
            Spacer(modifier = Modifier.height(10.dp))
            ApkDetailRow("Versão:", d?.versionDisplay ?: "...")
            Spacer(modifier = Modifier.height(10.dp))
            ApkDetailRow("Instalado:", d?.installedDisplay ?: "...")
            Spacer(modifier = Modifier.height(10.dp))
            ApkDetailRow("Tamanho:", d?.sizeDisplay ?: "...")
            Spacer(modifier = Modifier.height(10.dp))
            ApkDetailRow("Nome do Pacote:", d?.packageName ?: "...")

            Spacer(modifier = Modifier.height(18.dp))

            // Blue pill button: VIEW APK CONTENT
            val btnInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1576D2))
                    .pressable(interactionSource = btnInteraction, indication = SinkFeedback())
                    .clickable(interactionSource = btnInteraction, indication = null) {
                        onViewContent(apkFile)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "VIEW APK CONTENT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Action Row: GOOGLE PLAY | CANCELAR | INSTALAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GOOGLE PLAY",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1576D2),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            val pkg = d?.packageName ?: return@clickable
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
                            }.onFailure {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
                                }
                            }
                            onDismiss()
                        }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                )

                Text(
                    text = "CANCELAR",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1576D2),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                )

                Text(
                    text = "INSTALAR",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1576D2),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            runCatching {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    apkFile
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/vnd.android.package-archive")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                            onDismiss()
                        }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ApkDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF757575),
            modifier = Modifier.width(114.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = Color(0xFF616161),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}