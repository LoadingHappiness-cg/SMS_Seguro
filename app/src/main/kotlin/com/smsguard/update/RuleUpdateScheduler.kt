package com.smsguard.update

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.smsguard.BuildConfig
import java.util.UUID
import java.util.concurrent.TimeUnit

object RuleUpdateScheduler {

    private const val WORK_PERIODIC_NAME = "ruleset_update_periodic"
    const val WORK_CHECK_NOW_NAME = "ruleset_update_check_now"

    const val INPUT_URL_JSON = "url_json"
    const val INPUT_URL_SIG = "url_sig"
    const val INPUT_INTERACTIVE = "interactive"

    private fun constraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    fun schedulePeriodic(context: Context) {
        val data =
            workDataOf(
                INPUT_URL_JSON to BuildConfig.RULESET_JSON_URL,
                INPUT_URL_SIG to BuildConfig.RULESET_SIG_URL
            )

        val request =
            PeriodicWorkRequestBuilder<RuleUpdateWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.MINUTES
                )
                .setInputData(data)
                .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                WORK_PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
    }

    fun runCheckNow(context: Context): UUID {
        val data =
            workDataOf(
                INPUT_URL_JSON to BuildConfig.RULESET_JSON_URL,
                INPUT_URL_SIG to BuildConfig.RULESET_SIG_URL,
                INPUT_INTERACTIVE to true
            )

        val request =
            OneTimeWorkRequestBuilder<RuleUpdateWorker>()
                .setConstraints(constraints())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.MINUTES
                )
                .setInputData(data)
                .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                WORK_CHECK_NOW_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )

        return request.id
    }
}
