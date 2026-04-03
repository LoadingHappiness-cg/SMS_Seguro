package com.smsguard.storage

import android.content.Context
import androidx.core.util.AtomicFile
import com.smsguard.core.HistoryEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.StandardCharsets

class HistoryStore private constructor(
    private val historyDir: File,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val historyFile = File(historyDir, "history.json")
    private val atomicFile = AtomicFile(historyFile)

    constructor(context: Context) : this(File(context.filesDir, "history"))

    companion object {
        internal fun forTest(historyDir: File): HistoryStore = HistoryStore(historyDir)
    }

    @Serializable
    private data class HistoryEnvelope(
        val version: Int = 1,
        val events: List<HistoryEvent> = emptyList(),
    )

    init {
        if (!historyDir.exists()) historyDir.mkdirs()
    }

    fun saveEvent(event: HistoryEvent): Boolean {
        val current = getAllEvents().toMutableList()
        current.add(0, event)
        val limited = current.take(200)

        val envelope = HistoryEnvelope(events = limited)
        val bytes = json.encodeToString(envelope).toByteArray(StandardCharsets.UTF_8)

        val out = atomicFile.startWrite()
        return try {
            out.use { it.write(bytes) }
            atomicFile.finishWrite(out)
            true
        } catch (e: Exception) {
            atomicFile.failWrite(out)
            false
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
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearProcessedHistory() {
        if (historyFile.exists()) atomicFile.delete()
    }

    fun clear() {
        clearProcessedHistory()
    }

    internal fun seedHistoryForTest(events: List<HistoryEvent>) {
        val envelope = HistoryEnvelope(events = events.take(200))
        historyFile.parentFile?.mkdirs()
        historyFile.writeText(json.encodeToString(envelope), StandardCharsets.UTF_8)
    }
}
