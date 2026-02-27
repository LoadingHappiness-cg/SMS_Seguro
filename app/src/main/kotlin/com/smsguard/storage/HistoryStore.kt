package com.smsguard.storage

import android.content.Context
import com.smsguard.core.HistoryEvent
import androidx.core.util.AtomicFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.StandardCharsets

class HistoryStore(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val historyDir = File(context.filesDir, "history")
    private val historyFile = File(historyDir, "history.json")
    private val atomicFile = AtomicFile(historyFile)

    @Serializable
    private data class HistoryEnvelope(
        val version: Int = 1,
        val events: List<HistoryEvent> = emptyList(),
    )

    init {
        if (!historyDir.exists()) historyDir.mkdirs()
    }

    fun saveEvent(event: HistoryEvent) {
        val current = getAllEvents().toMutableList()
        current.add(0, event)
        val limited = current.take(200)

        val envelope = HistoryEnvelope(events = limited)
        val bytes = json.encodeToString(envelope).toByteArray(StandardCharsets.UTF_8)

        val out = atomicFile.startWrite()
        try {
            out.use { it.write(bytes) }
            atomicFile.finishWrite(out)
        } catch (e: Exception) {
            atomicFile.failWrite(out)
        }
    }

    fun getAllEvents(): List<HistoryEvent> {
        return try {
            if (historyFile.exists()) {
                atomicFile.openRead().use { input ->
                    val text = input.readBytes().toString(StandardCharsets.UTF_8)
                    val envelope = json.decodeFromString<HistoryEnvelope>(text)
                    envelope.events
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clear() {
        if (historyFile.exists()) atomicFile.delete()
    }
}
