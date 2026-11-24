package ro.trom.cuplaj

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build

const val ACT_ANSWER = "ro.trom.cuplaj.ANSWER"
const val ACT_REJECT = "ro.trom.cuplaj.REJECT"

/** Notificarea de apel: butoane Răspunde / Respinge direct de pe ecranul blocat. */
object Notif {
    private const val ID = 2
    private const val CH = "cuplaj_ring"

    fun showRing(ctx: Context, title: String, text: String, answer: String, reject: String) {
        try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= 26) {
                val ch = NotificationChannel(CH, "CUPLAJ · apel", NotificationManager.IMPORTANCE_HIGH)
                ch.setSound(null, null)
                ch.enableVibration(false)
                nm.createNotificationChannel(ch)
            }
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), flags)
            val ans = PendingIntent.getBroadcast(ctx, 1, Intent(ctx, ActionReceiver::class.java).setAction(ACT_ANSWER), flags)
            val rej = PendingIntent.getBroadcast(ctx, 2, Intent(ctx, ActionReceiver::class.java).setAction(ACT_REJECT), flags)
            val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(ctx, CH) else Notification.Builder(ctx)
            b.setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_phone_call)
                .setContentIntent(open)
                .setFullScreenIntent(open, true)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_CALL)
                .addAction(Notification.Action.Builder(Icon.createWithResource(ctx, android.R.drawable.ic_menu_call), answer, ans).build())
                .addAction(Notification.Action.Builder(Icon.createWithResource(ctx, android.R.drawable.ic_menu_close_clear_cancel), reject, rej).build())
            nm.notify(ID, b.build())
        } catch (_: Exception) {
        }
    }

    fun cancel(ctx: Context) {
        try {
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(ID)
        } catch (_: Exception) {
        }
    }
}

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        App.init(c)
        when (i.action) {
            ACT_ANSWER -> App.ui.post { App.lift() }
            ACT_REJECT -> App.ui.post { App.hang() }
        }
    }
}
