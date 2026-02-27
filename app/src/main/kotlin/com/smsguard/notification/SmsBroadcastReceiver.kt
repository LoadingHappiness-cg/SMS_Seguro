package com.smsguard.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*> ?: return
        val format = bundle.getString("format")

        val textBuilder = StringBuilder()
        var sender = ""

        for (pdu in pdus) {
            val bytes = pdu as? ByteArray ?: continue
            val sms =
                if (format != null) {
                    SmsMessage.createFromPdu(bytes, format)
                } else {
                    SmsMessage.createFromPdu(bytes)
                }
            if (sender.isBlank()) {
                sender = sms.displayOriginatingAddress.orEmpty()
            }
            textBuilder.append(sms.displayMessageBody.orEmpty())
        }

        val messageText = textBuilder.toString().trim()
        if (messageText.isBlank()) return

        try {
            SmsEventProcessor.process(
                context = context.applicationContext,
                sender = sender.ifBlank { "SMS" },
                rawText = messageText,
                source = "sms_broadcast",
                packageName = "telephony",
            )
        } catch (e: Exception) {
            Log.e("SMS_SEGURO", "Error processing SMS broadcast", e)
        }
    }
}

