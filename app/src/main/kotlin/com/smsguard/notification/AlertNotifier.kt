package com.smsguard.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.smsguard.R
import com.smsguard.core.AlertType
import com.smsguard.core.RiskAssessment
import com.smsguard.core.RiskLevel
import com.smsguard.ui.AlertActivity

object AlertNotifier {

    fun show(
        context: Context,
        sender: String,
        assessment: RiskAssessment,
    ) {

        AlertNotifierChannels.ensure(context)

        if (android.os.Build.VERSION.SDK_INT >= 33) {

            val granted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                Log.d("SMS_SEGURO", "POST_NOTIFICATIONS not granted; skipping alert notification")
                return
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

        NotificationManagerCompat.from(context)
            .notify(
                notificationId,
                notification
            )
    }
}
