package com.smsguard.storage

import android.content.Context
import com.smsguard.core.HistoryEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class HistoryStore(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val historyFile = File(context.filesDir, "history.json")

    fun saveEvent(event: HistoryEvent) {
        val events = getAllEvents().toMutableList()
        events.add(0, event) // Add to top
        // Limit to last 100 events
        val limited = events.take(100)
        historyFile.writeText(json.encodeToString(limited))
    }

    fun getAllEvents(): List<HistoryEvent> {
        return try {
            if (historyFile.exists()) {
                json.decodeFromString<List<HistoryEvent>>(historyFile.readText())
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clear() {
        if (historyFile.exists()) historyFile.delete()
    }
}
