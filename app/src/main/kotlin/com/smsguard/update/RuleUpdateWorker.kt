package com.smsguard.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smsguard.rules.RuleLoader
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class RuleUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result {
        val rulesUrl = "https://example.com/ruleset-latest.json"
        val sigUrl = "https://example.com/ruleset-latest.sig"

        return try {
            val rulesResponse = download(rulesUrl) ?: return Result.retry()
            val sigResponse = download(sigUrl) ?: return Result.retry()

            if (rulesResponse.size > 1024 * 1024) return Result.failure() // 1MB limit

            val isValid = SignatureVerifier.verify(rulesResponse, sigResponse)
            if (isValid) {
                val ruleLoader = RuleLoader(applicationContext)
                val success = ruleLoader.saveNewRuleset(String(rulesResponse))
                if (success) Result.success() else Result.failure()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun download(url: String): ByteArray? {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.bytes()
        }
    }
}
