package com.smsguard.rules

import android.content.Context
import com.smsguard.core.RuleSet
import kotlinx.serialization.json.Json
import java.io.File

class RuleLoader(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val rulesDir = File(context.filesDir, "rules")
    private val currentFile = File(rulesDir, "ruleset_current.json")
    private val previousFile = File(rulesDir, "ruleset_previous.json")

    init {
        if (!rulesDir.exists()) rulesDir.mkdirs()
    }

    fun loadCurrent(): RuleSet {
        return try {
            if (currentFile.exists()) {
                json.decodeFromString<RuleSet>(currentFile.readText())
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
            
            currentFile.writeText(newJson)
            true
        } catch (e: Exception) {
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
}
