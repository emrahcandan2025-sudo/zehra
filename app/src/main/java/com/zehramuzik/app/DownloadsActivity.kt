package com.zehramuzik.app

import android.app.DownloadManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val DlInk = Color(0xFF090910)
private val DlPanel = Color(0xFF151421)
private val DlPurple = Color(0xFFB99AFF)
private val DlGold = Color(0xFFE7C98A)
private val DlMuted = Color(0xFFA5A0B5)
private val DlWhite = Color(0xFFF8F5FF)

class DownloadsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(9, 9, 16)
        window.navigationBarColor = android.graphics.Color.rgb(9, 9, 16)
        setContent { DownloadsScreen(onBack = { finish() }) }
    }
}

@Composable
private fun DownloadsScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { DownloadRepository(context) }
    var url by remember { mutableStateOf("") }
    var filename by remember { mutableStateOf("") }
    var downloads by remember { mutableStateOf(emptyList<DownloadRepository.DownloadItem>()) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun refresh() { downloads = repository.query() }
    LaunchedEffect(Unit) {
        while (true) { refresh(); delay(1000) }
    }

    MaterialTheme(colorScheme = darkColorScheme(background = DlInk, surface = DlPanel, primary = DlPurple, secondary = DlGold, onBackground = DlWhite, onSurface = DlWhite)) {
        Scaffold(containerColor = DlInk, topBar = {
            Row(Modifier.fillMaxWidth().background(DlInk).padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Geri", tint = DlWhite) }
                Column(Modifier.weight(1f)) {
                    Text("İNDİRME MERKEZİ", color = DlGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("İndirilen dosyalar", color = DlWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Filled.Download, null, tint = DlPurple, modifier = Modifier.size(28.dp))
            }
        }) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Column(Modifier.fillMaxWidth().background(DlPanel, RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Doğrudan ses dosyası indir", color = DlWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("HTTP/HTTPS ile erişilebilen ve indirme izniniz olan doğrudan dosya bağlantısını girin. Canlı radyo yayınları, tek parça dosya olmadığı için burada indirilebilir parça olarak desteklenmez.", color = DlMuted, fontSize = 12.sp, lineHeight = 17.sp)
                        OutlinedTextField(value = url, onValueChange = { url = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Dosya URL'si") }, placeholder = { Text("https://site.com/muzik.mp3") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DlPurple, unfocusedBorderColor = Color(0xFF393246), focusedTextColor = DlWhite, unfocusedTextColor = DlWhite, cursorColor = DlPurple))
                        OutlinedTextField(value = filename, onValueChange = { filename = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Dosya adı") }, placeholder = { Text("sarki.mp3") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DlPurple, unfocusedBorderColor = Color(0xFF393246), focusedTextColor = DlWhite, unfocusedTextColor = DlWhite, cursorColor = DlPurple))
                        Button(onClick = {
                            try {
                                if (url.isBlank()) throw IllegalArgumentException("Önce dosya adresini gir.")
                                val name = filename.trim().ifBlank { url.substringBefore('?').substringAfterLast('/').ifBlank { "muzik.mp3" } }
                                repository.enqueue(url, name)
                                url = ""; filename = ""; message = "İndirme sıraya eklendi."; refresh()
                            } catch (e: Exception) { message = e.message ?: "İndirme başlatılamadı." }
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DlPurple, contentColor = DlInk), shape = RoundedCornerShape(14.dp)) { Text("İndirmeyi başlat", fontWeight = FontWeight.Bold) }
                        message?.let { Text(it, color = if (it.contains("eklendi")) DlGold else Color(0xFFFF9D9D), fontSize = 12.sp) }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("İndirme geçmişi", color = DlWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${downloads.size} dosya", color = DlMuted, fontSize = 12.sp)
                    }
                }
                if (downloads.isEmpty()) item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Download, null, tint = DlPurple, modifier = Modifier.size(42.dp))
                        Spacer(Modifier.height(8.dp)); Text("Henüz indirme yok", color = DlMuted)
                    }
                }
                items(downloads, key = { it.id }) { item ->
                    Row(Modifier.fillMaxWidth().background(DlPanel, RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Download, null, tint = if (item.status == DownloadManager.STATUS_SUCCESSFUL) DlGold else DlPurple, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, color = DlWhite, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(3.dp))
                            Text(DownloadRepository.statusText(item), color = DlMuted, fontSize = 12.sp)
                            if (item.status == DownloadManager.STATUS_RUNNING || item.status == DownloadManager.STATUS_PENDING) {
                                Spacer(Modifier.height(6.dp)); LinearProgressIndicator(progress = { item.progress / 100f }, modifier = Modifier.fillMaxWidth(), color = DlPurple, trackColor = Color(0xFF30283D))
                            }
                            if (item.totalBytes > 0) Text("${formatBytes(item.downloadedBytes)} / ${formatBytes(item.totalBytes)}", color = DlMuted, fontSize = 10.sp)
                        }
                        IconButton(onClick = { repository.cancel(item.id); refresh() }) { Icon(Icons.Filled.DeleteOutline, "İndirmeyi kaldır", tint = DlMuted) }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024f * 1024f))
    bytes >= 1024L -> String.format("%.0f KB", bytes / 1024f)
    else -> "$bytes B"
}
