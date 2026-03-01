package com.smsguard.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.smsguard.R
import com.smsguard.core.AppLogger
import com.smsguard.core.AlertType
import com.smsguard.core.RiskAssessment
import com.smsguard.core.RiskLevel
import com.smsguard.ui.AlertActivity

object AlertNotifier {

    fun show(
        context: Context,
        sender: String,
        assessment: RiskAssessment,
    ): Boolean {

        AlertNotifierChannels.ensure(context)

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            AppLogger.w("ProbeC notify skipped reason=app_notifications_disabled")
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = notificationManager?.getNotificationChannel(AlertNotifierChannels.CHANNEL_ID)
            if (channel == null || channel.importance == NotificationManager.IMPORTANCE_NONE) {
                AppLogger.w("ProbeC notify skipped reason=alert_channel_disabled channel=${AlertNotifierChannels.CHANNEL_ID}")
                return false
            }
        }

        if (Build.VERSION.SDK_INT >= 33) {

            val granted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                AppLogger.w("ProbeC notify skipped reason=post_notifications_denied")
                return false
            }
        }

        val intent = Intent(
            context,
            AlertActivity::class.java
        ).apply {

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP

            putExtra("sender", sender)
            putExtra("url", assessment.primaryUrl)
            putExtra("message_text", assessment.messageText)
            putExtra("score", assessment.score)
            putExtra("level", assessment.level.name)
            putExtra("alert_type", assessment.alertType.name)

            assessment.multibancoData?.let { mb ->
                putExtra("mb_entidade", mb.entidade)
                putExtra("mb_referencia", mb.referencia)
                putExtra("mb_valor", mb.valor)
            }

            putStringArrayListExtra(
                "reasons",
                ArrayList(assessment.reasons)
            )
        }

        val notificationId = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val (title, content, bigText) =
            when (assessment.alertType) {
                AlertType.MULTIBANCO -> {
                    val mb = assessment.multibancoData
                    val titleMb = context.getString(R.string.mb_notif_title)
                    val contentMb =
                        if (mb != null) {
                            context.getString(
                                R.string.mb_notif_content,
                                mb.entidade,
                                mb.referencia
                            )
                        } else {
                            context.getString(R.string.mb_notif_content_missing)
                        }
                    val big =
                        if (mb?.valor.isNullOrBlank()) {
                            contentMb
                        } else {
                            context.getString(
                                R.string.mb_notif_bigtext_with_value,
                                mb!!.entidade,
                                mb.referencia,
                                mb.valor
                            )
                        }
                    Triple(titleMb, contentMb, big)
                }
                AlertType.URL -> {
                    val (titleRes, textRes) = when (assessment.level) {
                        RiskLevel.HIGH -> R.string.notif_high_title to R.string.notif_high_text
                        RiskLevel.MEDIUM -> R.string.notif_medium_title to R.string.notif_medium_text
                        RiskLevel.LOW -> R.string.notif_low_title to R.string.notif_low_text
                    }

                    val titleUrl = context.getString(titleRes)
                    val contentUrl = context.getString(
                        R.string.notif_content_with_domain,
                        assessment.primaryDomain,
                        context.getString(textRes)
                    )
                    Triple(titleUrl, contentUrl, contentUrl)
                }
            }

        val builder =
            NotificationCompat.Builder(context, AlertNotifierChannels.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)

        if (assessment.alertType == AlertType.MULTIBANCO || assessment.level == RiskLevel.HIGH) {
            builder.setFullScreenIntent(pendingIntent, true)
        }

        val notification = builder.build()

        return runCatching {
            AppLogger.d("ProbeC alert notified notificationId=$notificationId channelId=${AlertNotifierChannels.CHANNEL_ID} riskLevel=${assessment.level} timestamp=${System.currentTimeMillis()}")
            AlertPipelineDiagnostics.recordNotified(context)
            NotificationManagerCompat.from(context)
                .notify(
                    notificationId,
                    notification
                )
            true
        }.getOrElse { error ->
            AppLogger.e("ProbeC notify failed notificationId=$notificationId", error)
            false
        }
    }
}
