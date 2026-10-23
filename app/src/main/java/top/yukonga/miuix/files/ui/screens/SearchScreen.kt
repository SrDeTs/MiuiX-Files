package top.yukonga.miuix.files.ui.screens

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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import top.yukonga.miuix.files.R
import top.yukonga.miuix.files.core.model.FileItem
import top.yukonga.miuix.files.core.storage.StorageManager
import top.yukonga.miuix.files.ui.components.*
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import java.io.File

/** Procurar: busca recursiva por nome no armazenamento interno. */
@Composable
fun SearchScreen(onBack: () -> Unit, onOpen: (FileItem) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    LaunchedEffect(query) {
        if (query.length < 2) { results = emptyList(); return@LaunchedEffect }
        delay(300)
        searching = true
        val q = query.lowercase()
        results = withContext(Dispatchers.IO) {
            val out = ArrayList<FileItem>()
            Environment.getExternalStorageDirectory().walkTopDown()
                .onEnter { !it.name.startsWith(".") && it.name != "Android" }
                .filter { it.name.lowercase().contains(q) }
                .take(300)
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
    var files by remember { mutableStateOf<List<FileItem>?>(null) }
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { files = StorageManager.getRecentFiles(limit = 200) }
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
                    Text(f.name, fontSize = 14.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${relativeTime(f.lastModified)} • ${if (f.isDirectory) "Pasta" else StorageManager.formatFileSize(f.size)} • ${f.file.parent}",
                        fontSize = 11.sp, color = Color(0xFFA0A3A6), maxLines = 1, overflow = TextOverflow.Ellipsis
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
                Image(painterResource(R.drawable.toolbar_return), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(Color.White))
            }
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Column(Modifier.weight(1f).navigationBarsPadding(), content = content)
    }
}
