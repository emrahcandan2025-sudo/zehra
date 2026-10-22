package com.zehramuzik.app

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.database.Cursor
import java.net.URL

/** Persistent downloads backed by Android's system DownloadManager. */
class DownloadRepository(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    data class DownloadItem(
        val id: Long,
        val title: String,
        val status: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val localUri: String?,
        val reason: Int
    ) {
        val progress: Int get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
    }

    fun enqueue(fileUrl: String, displayName: String): Long {
        val uri = Uri.parse(fileUrl.trim())
        require(uri.scheme == "https" || uri.scheme == "http") { "İndirme adresi http:// veya https:// ile başlamalı." }
        require(!uri.host.isNullOrBlank()) { "Geçerli bir indirme adresi gir." }
        require(!uri.path.isNullOrBlank()) { "Bu adres doğrudan bir dosyaya işaret etmiyor olabilir." }
        val safeName = displayName.trim().replace(Regex("[^\\p{L}\\p{N}._ -]"), "_")
            .trim('.', ' ').take(100).ifBlank { "zehra_muzik_download" }
        val request = DownloadManager.Request(uri)
            .setTitle(safeName)
            .setDescription("Zehra Müzik · İndiriliyor")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "Zehra Müzik/$safeName")
        return manager.enqueue(request)
    }

    fun query(): List<DownloadItem> {
        // DownloadManager.Query.setFilterByStatus accepts a single bitmask, not varargs.
        // An empty Query returns all downloads; sort and display them below.
        val cursor = manager.query(DownloadManager.Query()) ?: return emptyList()
        return cursor.use { c ->
            val rows = mutableListOf<DownloadItem>()
            while (c.moveToNext()) rows.add(c.toItem())
            rows.sortedByDescending { it.id }
        }
    }

    fun cancel(id: Long) { manager.remove(id) }

    private fun Cursor.toItem(): DownloadItem {
        fun long(name: String) = getColumnIndex(name).takeIf { it >= 0 }?.let { getLong(it) } ?: 0L
        fun int(name: String) = getColumnIndex(name).takeIf { it >= 0 }?.let { getInt(it) } ?: 0
        fun string(name: String) = getColumnIndex(name).takeIf { it >= 0 && !isNull(it) }?.let { getString(it) }
        return DownloadItem(
            id = long(DownloadManager.COLUMN_ID),
            title = string(DownloadManager.COLUMN_TITLE) ?: "İndirilen dosya",
            status = int(DownloadManager.COLUMN_STATUS),
            downloadedBytes = long(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
            totalBytes = long(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
            localUri = string(DownloadManager.COLUMN_LOCAL_URI),
            reason = int(DownloadManager.COLUMN_REASON)
        )
    }

    companion object {
        fun statusText(item: DownloadItem): String = when (item.status) {
            DownloadManager.STATUS_PENDING -> "Sırada bekliyor"
            DownloadManager.STATUS_RUNNING -> "İndiriliyor · %${item.progress}"
            DownloadManager.STATUS_PAUSED -> "Duraklatıldı"
            DownloadManager.STATUS_SUCCESSFUL -> "Tamamlandı"
            DownloadManager.STATUS_FAILED -> "İndirme başarısız (kod ${item.reason})"
            else -> "Durum bilinmiyor"
        }
    }
}
