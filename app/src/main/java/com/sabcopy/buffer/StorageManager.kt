package com.sabcopy.buffer

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object StorageManager {

    private lateinit var itemsDir: File
    private var masterKey: MasterKey? = null
    private var appContext: Context? = null
    private val lock = ReentrantReadWriteLock()
    private val cache = ConcurrentHashMap<Int, BufferItem>()

    data class ItemPreview(val id: Int, val length: Int, val preview: String, val timestamp: Long)
    data class BufferItem(val id: Int, val text: String, val timestamp: Long, val length: Int)

    fun init(context: Context) {
        appContext = context.applicationContext
        itemsDir = File(context.filesDir, "sabcopy_items")
        itemsDir.mkdirs()
        masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private fun writeEncrypted(file: File, data: String) {
        val ctx = appContext ?: return
        val mk = masterKey ?: return
        val ef = EncryptedFile.Builder(ctx, file, mk, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build()
        ef.openFileOutput().use { it.write(data.toByteArray(Charsets.UTF_8)) }
    }

    private fun readEncrypted(file: File): String? {
        val ctx = appContext ?: return null
        val mk = masterKey ?: return null
        if (!file.exists()) return null
        val ef = EncryptedFile.Builder(ctx, file, mk, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build()
        return ef.openFileInput().use { it.readBytes().toString(Charsets.UTF_8) }
    }

    fun addText(text: String): Int {
        val id = lock.write {
            val existing = itemsDir.listFiles()?.mapNotNull { it.nameWithoutExtension.toIntOrNull() }?.maxOrNull() ?: 0
            existing + 1
        }
        val timestamp = System.currentTimeMillis()
        cache[id] = BufferItem(id, text, timestamp, text.length)
        writeEncrypted(File(itemsDir, "$id.txt"), "$id|$timestamp|${text.length}\n$text")
        return id
    }

    fun getText(id: Int): String? {
        cache[id]?.let { return it.text }
        val content = readEncrypted(File(itemsDir, "$id.txt")) ?: return null
        val idx = content.indexOf('\n')
        return if (idx > 0) content.substring(idx + 1) else null
    }

    fun getAllPreviews(): List<ItemPreview> {
        val files = itemsDir.listFiles() ?: return emptyList()
        return files.mapNotNull { file ->
            val id = file.nameWithoutExtension.toIntOrNull() ?: return@mapNotNull null
            val content = readEncrypted(file) ?: return@mapNotNull null
            val idx = content.indexOf('\n')
            if (idx <= 0) return@mapNotNull null
            val parts = content.substring(0, idx).split("|")
            if (parts.size < 3) return@mapNotNull null
            val text = content.substring(idx + 1)
            ItemPreview(id, parts[2].toIntOrNull() ?: 0, if (text.length > 50) text.take(50) + "…" else text, parts[1].toLongOrNull() ?: 0L)
        }.sortedByDescending { it.id }
    }

    fun deleteItem(id: Int): Boolean { cache.remove(id); File(itemsDir, "$id.txt").delete(); return true }
    fun clearAll() { cache.clear(); itemsDir.listFiles()?.forEach { it.delete() } }
    fun getCount(): Int = itemsDir.listFiles()?.size ?: 0
}
