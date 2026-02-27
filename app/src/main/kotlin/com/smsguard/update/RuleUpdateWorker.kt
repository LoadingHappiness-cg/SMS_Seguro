package com.smsguard.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data
import androidx.work.workDataOf
import com.smsguard.core.RuleSet
import com.smsguard.rules.RuleLoader
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import android.util.Base64
import android.content.Context.MODE_PRIVATE
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class RuleUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val OUTCOME_KEY = "outcome"
        const val OUTCOME_UPDATED = "updated"
        const val OUTCOME_NOOP = "noop"
        const val OUTCOME_NO_NETWORK = "no_network"
        const val OUTCOME_INVALID_SIG = "invalid_signature"
        const val OUTCOME_FAILED = "failed"
    }

    private val client = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result {
        val rulesUrl = inputData.getString(RuleUpdateScheduler.INPUT_URL_JSON).orEmpty().trim()
        val sigUrl = inputData.getString(RuleUpdateScheduler.INPUT_URL_SIG).orEmpty().trim()
        val interactive = inputData.getBoolean(RuleUpdateScheduler.INPUT_INTERACTIVE, false)

        val prefs = applicationContext.getSharedPreferences("ruleset_meta", MODE_PRIVATE)
        fun markLastCheck() {
            prefs.edit()
                .putLong("last_check_at", System.currentTimeMillis())
                .apply()
        }

        if (rulesUrl.isBlank() || sigUrl.isBlank()) {
            markLastCheck()
            return Result.success(outcomeData(OUTCOME_NOOP))
        }

        return try {
            val rulesResponse = downloadBytes(rulesUrl)
            if (rulesResponse.size > 1_000_000) {
                markLastCheck()
                return Result.failure(outcomeData(OUTCOME_FAILED))
            }

            val sigText = downloadText(sigUrl)
            val signatureBytes = try {
                Base64.decode(sigText.trim(), Base64.DEFAULT)
            } catch (_: IllegalArgumentException) {
                markLastCheck()
                return Result.failure(outcomeData(OUTCOME_FAILED))
            }

            val isValid =
                SignatureVerifier.verifyEd25519(
                    messageBytes = rulesResponse,
                    signatureBytes = signatureBytes
                )

            if (!isValid) {
                markLastCheck()
                return Result.failure(outcomeData(OUTCOME_INVALID_SIG))
            }

            val jsonString = String(rulesResponse, StandardCharsets.UTF_8)
            val newRuleSet = RuleLoader.json.decodeFromString(RuleSet.serializer(), jsonString)

            val currentVersion = prefs.getInt("ruleset_version", 0).let { stored ->
                if (stored > 0) stored else RuleLoader(applicationContext).loadCurrent().version
            }

            if (newRuleSet.version <= currentVersion) {
                // No-op (anti-downgrade)
                markLastCheck()
                return Result.success(outcomeData(OUTCOME_NOOP))
            }

            val ruleLoader = RuleLoader(applicationContext)
            val persisted = ruleLoader.persistNewRuleset(
                newRulesetBytes = rulesResponse,
                newVersion = newRuleSet.version
            )

            if (!persisted) {
                markLastCheck()
                return Result.failure(outcomeData(OUTCOME_FAILED))
            }

            prefs.edit()
                .putInt("ruleset_version", newRuleSet.version)
                .putLong("last_update_at", System.currentTimeMillis())
                .putLong("last_check_at", System.currentTimeMillis())
                .apply()

            Result.success(outcomeData(OUTCOME_UPDATED))
        } catch (e: HttpErrorException) {
            markLastCheck()
            if (interactive) {
                Result.failure(outcomeData(OUTCOME_FAILED))
            } else {
                Result.retry()
            }
        } catch (e: IOException) {
            if (interactive) {
                markLastCheck()
                Result.failure(outcomeData(networkOutcomeFor(e)))
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            markLastCheck()
            Result.failure(outcomeData(OUTCOME_FAILED))
        }
    }

    private fun downloadBytes(url: String): ByteArray {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            return when {
                response.isSuccessful -> response.body?.bytes() ?: throw IOException("Empty body")
                else -> throw HttpErrorException(response.code)
            }
        }
    }

    private fun downloadText(url: String): String {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            return when {
                response.isSuccessful -> response.body?.string() ?: throw IOException("Empty body")
                else -> throw HttpErrorException(response.code)
            }
        }
    }

    private fun outcomeData(outcome: String): Data =
        workDataOf(OUTCOME_KEY to outcome)

    private fun networkOutcomeFor(e: IOException): String =
        when (e) {
            is UnknownHostException,
            is ConnectException,
            is SocketTimeoutException,
            is FileNotFoundException -> OUTCOME_NO_NETWORK
            else -> OUTCOME_FAILED
        }

    private class HttpErrorException(val code: Int) : IOException("HTTP $code")
}
