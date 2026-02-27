package com.smsguard.rules

import android.content.Context
import com.smsguard.core.RuleSet
import kotlinx.serialization.json.Json
import androidx.core.util.AtomicFile
import java.io.File
import java.nio.charset.StandardCharsets

class RuleLoader(private val context: Context) {
    companion object {
        val json: Json = Json { ignoreUnknownKeys = true }
    }

    private val rulesDir = File(context.filesDir, "rules")
    private val currentFile = File(rulesDir, "ruleset_current.json")
    private val previousFile = File(rulesDir, "ruleset_previous.json")
    private val currentAtomicFile = AtomicFile(currentFile)

    init {
        if (!rulesDir.exists()) rulesDir.mkdirs()
    }

    fun loadCurrent(): RuleSet {
        return try {
            if (currentFile.exists()) {
                json.decodeFromString<RuleSet>(currentFile.readText(Charsets.UTF_8))
            } else {
                loadDefault()
            }
        } catch (e: Exception) {
            loadDefault()
        }
    }

    private fun loadDefault(): RuleSet {
        val jsonString = context.assets.open("ruleset_default.json").bufferedReader().use { it.readText() }
        return json.decodeFromString<RuleSet>(jsonString)
    }

    fun saveNewRuleset(newJson: String): Boolean {
        return try {
            val newRuleSet = json.decodeFromString<RuleSet>(newJson)
            val current = loadCurrent()
            
            if (newRuleSet.version <= current.version) return false

            // Rollback support
            if (currentFile.exists()) {
                currentFile.copyTo(previousFile, overwrite = true)
            }
            
            persistBytesAtomically(newJson.toByteArray(StandardCharsets.UTF_8))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun persistNewRuleset(newRulesetBytes: ByteArray, newVersion: Int): Boolean {
        return try {
            val current = loadCurrent()
            if (newVersion <= current.version) return false

            if (currentFile.exists()) {
                currentFile.copyTo(previousFile, overwrite = true)
            }

            persistBytesAtomically(newRulesetBytes)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun rollback(): Boolean {
        return if (previousFile.exists()) {
            previousFile.copyTo(currentFile, overwrite = true)
            previousFile.delete()
            true
        } else {
            false
        }
    }

    private fun persistBytesAtomically(bytes: ByteArray) {
        val out = currentAtomicFile.startWrite()
        try {
            out.use { it.write(bytes) }
            currentAtomicFile.finishWrite(out)
        } catch (e: Exception) {
            currentAtomicFile.failWrite(out)
            throw e
        }
    }
}
