package com.zehramuzik.app

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

private val Ink = Color(0xFF090910)
private val Panel = Color(0xFF151421)
private val Panel2 = Color(0xFF201A31)
private val Purple = Color(0xFFB99AFF)
private val Gold = Color(0xFFE7C98A)
private val Muted = Color(0xFFA5A0B5)
private val White = Color(0xFFF8F5FF)

data class RadioStation(
    val name: String,
    val streamUrl: String,
    val homepage: String = "",
    val tags: String = "",
    val codec: String = "",
    val bitrate: Int = 0
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(9, 9, 16)
        window.navigationBarColor = android.graphics.Color.rgb(9, 9, 16)
        setContent { ZehraTheme { StreamingRadioApp() } }
    }
}

@Composable
private fun ZehraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Ink, surface = Panel, primary = Purple,
            secondary = Gold, onBackground = White, onSurface = White
        ), content = content
    )
}

@Composable
private fun StreamingRadioApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val player = remember { ExoPlayer.Builder(context).build() }
    var stations by remember { mutableStateOf<List<RadioStation>>(emptyList()) }
    var selected by remember { mutableStateOf<RadioStation?>(null) }
    var playing by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf("Dinle") }
    var playerMessage by remember { mutableStateOf("Bir istasyon seçerek dinlemeye başla.") }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlayerError(errorValue: PlaybackException) {
                playerMessage = "Yayın açılamadı. İstasyon çevrimdışı veya bölge kısıtlı olabilir."
                error = playerMessage
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_BUFFERING) playerMessage = "Yayın yükleniyor…"
                else if (playbackState == Player.STATE_READY) playerMessage = "Canlı yayın"
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    fun loadStations() {
        scope.launch {
            loading = true
            error = null
            try {
                stations = withContext(Dispatchers.IO) {
                    // Radio Browser publishes directory metadata as public-domain data.
                    // The actual stream remains hosted by each broadcaster.
                    val endpoints = listOf(
                        "https://de1.api.radio-browser.info/json/stations/bycountrycodeexact/TR?hidebroken=true&order=clickcount&reverse=true&limit=60",
                        "https://nl1.api.radio-browser.info/json/stations/bycountrycodeexact/TR?hidebroken=true&order=clickcount&reverse=true&limit=60"
                    )
                    var last: Exception? = null
                    var parsed: List<RadioStation> = emptyList()
                    for (endpoint in endpoints) {
                        try {
                            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                                connectTimeout = 8000
                                readTimeout = 10000
                                requestMethod = "GET"
                                setRequestProperty("User-Agent", "ZehraMuzik/1.0 (Android)")
                                setRequestProperty("Accept", "application/json")
                            }
                            val text = try {
                                conn.inputStream.bufferedReader().use { it.readText() }
                            } finally {
                                conn.disconnect()
                            }
                            val arr = JSONArray(text)
                            val out = mutableListOf<RadioStation>()
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val url = obj.optString("url_resolved").ifBlank { obj.optString("url") }
                                val name = obj.optString("name").trim()
                                if (name.isNotBlank() && (url.startsWith("https://") || url.startsWith("http://"))) {
                                    out.add(
                                        RadioStation(
                                            name = name,
                                            streamUrl = url,
                                            homepage = obj.optString("homepage"),
                                            tags = obj.optString("tags"),
                                            codec = obj.optString("codec"),
                                            bitrate = obj.optInt("bitrate")
                                        )
                                    )
                                }
                            }
                            parsed = out.distinctBy { it.streamUrl }
                            if (parsed.isNotEmpty()) break
                        } catch (e: Exception) { last = e }
                    }
                    if (parsed.isEmpty()) throw (last ?: Exception("Yayın listesi alınamadı."))
                    parsed
                }
            } catch (e: Exception) {
                error = "İstasyon listesi yüklenemedi. İnternet bağlantını kontrol edip yenile."
            } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { loadStations() }

    fun playStation(station: RadioStation) {
        selected = station
        error = null
        playerMessage = "Yayın yükleniyor…"
        try {
            player.setMediaItem(MediaItem.fromUri(station.streamUrl))
            player.prepare()
            player.play()
        } catch (e: Exception) {
            error = "Bu yayın başlatılamadı."
        }
    }

    val filtered = stations.filter {
        query.isBlank() || it.name.contains(query, true) || it.tags.contains(query, true)
    }

    Scaffold(
        containerColor = Ink,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF11101A), contentColor = White) {
                listOf("Dinle", "Ara").forEach { item ->
                    NavigationBarItem(
                        selected = activeTab == item,
                        onClick = { activeTab = item },
                        icon = { Icon(if (item == "Dinle") Icons.Filled.GraphicEq else Icons.Filled.Search, null) },
                        label = { Text(item) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Purple, selectedTextColor = Purple,
                            indicatorColor = Panel2, unselectedIconColor = Muted, unselectedTextColor = Muted
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.weight(1f).fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("ZEHRA", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                        Text("MÜZİK", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    }
                    IconButton(onClick = { context.startActivity(Intent(context, DownloadsActivity::class.java)) }) {
                        Icon(Icons.Filled.Download, "İndirme merkezi", tint = Gold)
                    }
                    IconButton(onClick = { loadStations() }) {
                        Icon(Icons.Filled.Refresh, "İstasyonları yenile", tint = Purple)
                    }
                }
                if (activeTab == "Ara") {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        placeholder = { Text("İstasyon veya tür ara…", color = Muted) },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = Purple) },
                        singleLine = true, shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Purple, unfocusedBorderColor = Color(0xFF393246),
                            focusedTextColor = White, unfocusedTextColor = White, cursorColor = Purple
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF392653), Panel, Color(0xFF28201D))))
                    .padding(18.dp)) {
                    Text("CANLI RADYO", color = Gold, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text(selected?.name ?: "Türkiye'nin sesleri", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(playerMessage, color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(Panel2), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.GraphicEq, null, tint = Gold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (playing) "Şimdi çalıyor" else "Zehra Müzik oynatıcı", color = White, fontWeight = FontWeight.SemiBold)
                            Text(selected?.let { "${it.codec.ifBlank { "Audio" }} · ${if (it.bitrate > 0) "${it.bitrate} kbps" else "Canlı yayın"}" }
                                ?: "Reklamsız arayüz", color = Muted, fontSize = 11.sp)
                        }
                        IconButton(onClick = {
                            if (selected == null) {
                                stations.firstOrNull()?.let { playStation(it) }
                            } else if (playing) player.pause() else player.play()
                        }) {
                            Icon(if (playing) Icons.Filled.PauseCircleFilled else Icons.Filled.PlayCircleFilled,
                                null, tint = Gold, modifier = Modifier.size(42.dp))
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (activeTab == "Ara") "Arama sonuçları" else "Türkiye istasyonları",
                        color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${filtered.size} istasyon", color = Muted, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Purple)
                    }
                    filtered.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.WifiOff, null, tint = Purple, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(error ?: "İstasyon bulunamadı.", color = Muted)
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(onClick = { loadStations() }) { Text("Tekrar dene") }
                        }
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered) { station ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .background(if (selected?.streamUrl == station.streamUrl) Panel2 else Panel)
                                    .clickable { playStation(station) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(48.dp).clip(RoundedCornerShape(13.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFF61468A), Color(0xFF211A30)))),
                                    contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Radio, null, tint = Gold, modifier = Modifier.size(25.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(station.name, color = White, fontWeight = FontWeight.SemiBold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        listOf(station.tags.split(",").firstOrNull()?.trim().orEmpty(),
                                            station.codec, if (station.bitrate > 0) "${station.bitrate} kbps" else "")
                                            .filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "İnternet radyosu" },
                                        color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    if (selected?.streamUrl == station.streamUrl && playing) Icons.Filled.Equalizer else Icons.Filled.PlayCircleOutline,
                                    null, tint = if (selected?.streamUrl == station.streamUrl) Gold else Purple,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
