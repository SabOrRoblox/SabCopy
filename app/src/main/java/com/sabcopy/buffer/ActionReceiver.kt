package com.sabcopy.buffer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class ActionReceiver : BroadcastReceiver() {
    private val handler = Handler(Looper.getMainLooper())
    override fun onReceive(ctx: Context, intent: Intent) {
        val acc = HcbAccessibilityService.instance
        if (acc == null || !HcbAccessibilityService.isRunning) {
            handler.post { Toast.makeText(ctx, "Служба не готова", Toast.LENGTH_SHORT).show() }
            return
        }
        when (intent.action) {
            "CAP" -> {
                val t = acc.grabText()
                if (t.isNotEmpty()) {
                    val id = StorageManager.addText(t)
                    handler.post { Toast.makeText(ctx, "Захвачено #$id (${t.length})", Toast.LENGTH_SHORT).show() }
                } else {
                    handler.post { Toast.makeText(ctx, "Ничего не выделено", Toast.LENGTH_SHORT).show() }
                }
                NotificationHelper.show(ctx)
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
