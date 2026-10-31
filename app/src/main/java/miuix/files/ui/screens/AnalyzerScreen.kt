package miuix.files.ui.screens

import android.os.Environment
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import miuix.files.R
import miuix.files.core.model.FileItem
import miuix.files.core.storage.StorageManager
import miuix.files.ui.components.CircularStorageGauge
import miuix.files.ui.components.EsCardColor
import miuix.files.ui.components.EsDividerColor
import miuix.files.ui.components.EsTextPrimary
import miuix.files.ui.components.EsTextSecondary
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import java.io.File

private data class AnalyzerResult(val dirs: List<Pair<File, Long>>, val bigFiles: List<FileItem>)

private var analyzerCache: AnalyzerResult? = null

/** Analis. de espaço: pastas que mais ocupam e arquivos grandes (>10 MB), como no ES. */
@Composable
fun AnalyzerScreen(onBack: () -> Unit, onOpenDir: (File) -> Unit, onOpenFile: (File) -> Unit) {
    val root = remember { Environment.getExternalStorageDirectory() }
    var result by remember { mutableStateOf(analyzerCache) }
    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        if (result != null) return@LaunchedEffect
        result = withContext(Dispatchers.IO) {
            val big = ArrayList<FileItem>()
            val dirs = root.listFiles().orEmpty().filter { it.isDirectory }.map { d ->
                var total = 0L
                d.walkTopDown().onEnter { it.canRead() }.forEach { f ->
                    if (f.isFile) {
                        val len = f.length()
                        total += len
                        if (len > 10L * 1024 * 1024) big.add(FileItem(f, size = len, isDirectory = false))
                    }
                }
                d to total
            }.sortedByDescending { it.second }
            AnalyzerResult(dirs, big.sortedByDescending { it.size }.take(50))
        }.also { analyzerCache = it }
    }

    val total = root.totalSpace
    val used = total - root.freeSpace
    EsSubScreen("Analis. de espaço", onBack) {
        LazyColumn(Modifier.fillMaxSize().overScrollVertical(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(
                    Modifier.fillMaxWidth(), cornerRadius = 10.dp, insideMargin = PaddingValues(16.dp),
                    colors = CardDefaults.defaultColors(color = EsCardColor, contentColor = EsTextPrimary)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val p = if (total > 0) used.toFloat() / total else 0f
                        CircularStorageGauge(p, (p * 100).toInt())
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Armazenamento interno", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EsTextPrimary)
                            Text("Usado ${StorageManager.formatFileSize(used)} de ${StorageManager.formatFileSize(total)}", fontSize = 12.sp, color = EsTextSecondary)
                        }
                    }
                }
            }
            val r = result
            if (r == null) {
                item { Text("Analisando armazenamento...", color = EsTextSecondary, fontSize = 13.sp, modifier = Modifier.padding(8.dp)) }
            } else {
                item { Text("Pastas maiores", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.primary) }
                val maxDir = r.dirs.firstOrNull()?.second?.coerceAtLeast(1L) ?: 1L
                items(r.dirs.take(15), key = { "d" + it.first.path }) { (d, size) ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(EsCardColor)
                            .clickable { onOpenDir(d) }.padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.format_folder), null, Modifier.size(30.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(d.name, fontSize = 13.5.sp, color = EsTextPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(StorageManager.formatFileSize(size), fontSize = 12.5.sp, color = EsTextSecondary)
                        }
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(EsDividerColor)) {
                            Box(Modifier.fillMaxWidth(size.toFloat() / maxDir).fillMaxHeight().background(MiuixTheme.colorScheme.primary))
                        }
                    }
                }
                item { Text("Arquivos grandes (> 10 MB)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp)) }
                if (r.bigFiles.isEmpty()) item { Text("Nenhum arquivo grande", color = EsTextSecondary, fontSize = 12.sp) }
                items(r.bigFiles, key = { "f" + it.path }) { f ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(EsCardColor)
                            .clickable { onOpenFile(f.file) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(painterResource(iconFor(f)), null, Modifier.size(30.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.name, fontSize = 13.sp, color = EsTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(f.file.parent ?: "", fontSize = 10.5.sp, color = EsTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(StorageManager.formatFileSize(f.size), fontSize = 12.5.sp, color = EsTextSecondary)
                    }
                }
            }
        }
    }
}
