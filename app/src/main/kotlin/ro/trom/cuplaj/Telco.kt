package ro.trom.cuplaj

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager

/** Legătura cu rețeaua mobilă a telefonului: SMS reale, apel real (prin formatorul sistemului), tipul rețelei. */
object Telco {
    fun hasSms(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
            ctx.checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    fun sendSms(ctx: Context, to: String, text: String): Boolean {
        if (ctx.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) return false
        return try {
            val sm = if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(SmsManager::class.java) else SmsManager.getDefault()
            val parts = sm.divideMessage(text)
            if (parts.size > 1) sm.sendMultipartTextMessage(to, null, parts, null, null) else sm.sendTextMessage(to, null, text, null, null)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Deschide formatorul cu numărul completat; omul apasă „Apel” (nu cere permisiunea CALL_PHONE). */
    fun dial(ctx: Context, number: String) {
        try {
            val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(safeNumber(number))))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
        } catch (_: Exception) {
        }
    }

    /** "wifi", "mobile", "other" sau "none". */
    fun netKind(ctx: Context): String {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val n = cm.activeNetwork ?: return "none"
            val c = cm.getNetworkCapabilities(n) ?: return "none"
            when {
                c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile"
                else -> "other"
            }
        } catch (_: Exception) {
            "other"
        }
    }
}

/** SMS primit pe telefonul Regiei (doar când poarta SMS e pornită) → către echipă. */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        if (i.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(i) ?: return
        val from = msgs.firstOrNull()?.originatingAddress ?: return
        val text = msgs.joinToString("") { it.messageBody ?: "" }
        App.onSmsReceived(from, text)
    }
}
