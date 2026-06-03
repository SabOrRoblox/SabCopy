package com.sabcopy.buffer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast

object NotificationHelper {

    private const val CH_ID = "sabcopy_actions"
    private const val NOTIF_ID = 1001

    fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CH_ID, "ClipVault", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) }
            )
        }
    }

    fun show(ctx: Context) {
        val cap = PendingIntent.getBroadcast(ctx, 0, Intent(ctx, ActionReceiver::class.java).apply { action = "CAP" }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val pas = PendingIntent.getBroadcast(ctx, 1, Intent(ctx, ActionReceiver::class.java).apply { action = "PAS" }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val app = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val n = Notification.Builder(ctx, CH_ID)
            .setContentTitle("ClipVault")
            .setContentText("Записей: ${StorageManager.getCount()}")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentIntent(app)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_save, "Захватить", cap)
            .addAction(android.R.drawable.ic_menu_edit, "Вставить", pas)
            .build()

        ctx.getSystemService(NotificationManager::class.java).notify(NOTIF_ID, n)
    }

    class ActionReceiver : BroadcastReceiver() {
        private val handler = Handler(Looper.getMainLooper())
        override fun onReceive(ctx: Context, intent: Intent) {
            val acc = HcbAccessibilityService.instance ?: return
            when (intent.action) {
                "CAP" -> {
                    val t = acc.grabText()
                    if (t.isNotEmpty()) {
                        val id = StorageManager.addText(t)
                        handler.post { Toast.makeText(ctx, "Захвачено #$id (${t.length})", Toast.LENGTH_SHORT).show() }
                    } else {
                        handler.post { Toast.makeText(ctx, "Ничего не выделено", Toast.LENGTH_SHORT).show() }
                    }
                    show(ctx)
                }
                "PAS" -> {
                    val previews = StorageManager.getAllPreviews()
                    if (previews.isEmpty()) {
                        handler.post { Toast.makeText(ctx, "Буфер пуст", Toast.LENGTH_SHORT).show() }
                        return
                    }
                    val last = StorageManager.getText(previews.first().id)
                    if (last != null) {
                        acc.putText(last)
                        handler.post { Toast.makeText(ctx, "Вставлено ${last.length}", Toast.LENGTH_SHORT).show() }
                    } else {
                        handler.post { Toast.makeText(ctx, "Ошибка", Toast.LENGTH_SHORT).show() }
                    }
                }
            }
        }
    }
}
