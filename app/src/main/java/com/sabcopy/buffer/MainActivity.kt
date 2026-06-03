package com.sabcopy.buffer

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        StorageManager.init(this)

        val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val bgColor = if (isDark) 0xFF0D0D0D.toInt() else 0xFFF0F4F0.toInt()
        val cardColor = if (isDark) 0xFF1A1A1A.toInt() else 0xFFFFFFFF.toInt()
        val textColor = if (isDark) 0xFFE0E0E0.toInt() else 0xFF1A1A1A.toInt()
        val subColor = if (isDark) 0xFF888888.toInt() else 0xFF666666.toInt()
        val accentColor = 0xFF1B5E20.toInt()

        val root = ScrollView(this)
        root.setBackgroundColor(bgColor)

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(32, 64, 32, 48)

        val title = TextView(this)
        title.text = "ClipVault"
        title.textSize = 30f
        title.setTextColor(accentColor)
        title.setPadding(0, 0, 0, 4)
        container.addView(title)

        val subtitle = TextView(this)
        subtitle.text = "умный буфер обмена"
        subtitle.textSize = 14f
        subtitle.setTextColor(subColor)
        subtitle.setPadding(0, 0, 0, 32)
        container.addView(subtitle)

        val accOk = isAccessibilityEnabled()
        val statusCard = LinearLayout(this)
        statusCard.orientation = LinearLayout.HORIZONTAL
        statusCard.setBackgroundColor(cardColor)
        statusCard.setPadding(24, 20, 24, 20)
        val scParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        scParams.bottomMargin = 16
        statusCard.layoutParams = scParams

        val statusText = TextView(this)
        statusText.text = if (accOk) "Служба активна" else "Служба отключена"
        statusText.textSize = 15f
        statusText.setTextColor(if (accOk) 0xFF2E7D32.toInt() else 0xFFB71C1C.toInt())
        val stParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        statusText.layoutParams = stParams
        statusCard.addView(statusText)

        val toggle = Switch(this)
        toggle.isChecked = accOk
        toggle.setOnCheckedChangeListener { _, _ ->
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        statusCard.addView(toggle)
        container.addView(statusCard)

        val countCard = LinearLayout(this)
        countCard.orientation = LinearLayout.VERTICAL
        countCard.setBackgroundColor(cardColor)
        countCard.setPadding(24, 20, 24, 20)
        val ccParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        ccParams.bottomMargin = 16
        countCard.layoutParams = ccParams

        val countLabel = TextView(this)
        countLabel.text = "Сохранено записей"
        countLabel.textSize = 12f
        countLabel.setTextColor(subColor)
        countCard.addView(countLabel)

        val countValue = TextView(this)
        countValue.text = "${StorageManager.getCount()}"
        countValue.textSize = 24f
        countValue.setTextColor(textColor)
        countCard.addView(countValue)
        container.addView(countCard)

        container.addView(btn("Открыть все записи", accentColor) { showAllItems() })
        container.addView(btn("Вставить последнюю", 0xFF2E7D32.toInt()) {
            val previews = StorageManager.getAllPreviews()
            if (previews.isEmpty()) {
                Toast.makeText(this, "Буфер пуст", Toast.LENGTH_SHORT).show()
            } else {
                val text = StorageManager.getText(previews.first().id)
                if (text != null) {
                    HcbAccessibilityService.instance?.putText(text)
                    Toast.makeText(this, "Вставлено ${text.length} симв.", Toast.LENGTH_SHORT).show()
                }
            }
        })
        container.addView(btn("Очистить хранилище", 0xFFB71C1C.toInt()) {
            StorageManager.clearAll()
            countValue.text = "0"
            Toast.makeText(this, "Очищено", Toast.LENGTH_SHORT).show()
        })

        root.addView(container)
        setContentView(root)
    }

    private fun btn(text: String, color: Int, click: () -> Unit): Button {
        val b = Button(this)
        b.text = text
        b.setTextColor(0xFFFFFFFF.toInt())
        b.textSize = 14f
        b.setBackgroundColor(color)
        b.setPadding(0, 18, 0, 18)
        val bp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        bp.bottomMargin = 10
        b.layoutParams = bp
        b.setOnClickListener { click() }
        return b
    }

    private fun isAccessibilityEnabled(): Boolean {
        val svc = "$packageName/.HcbAccessibilityService"
        val list = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return list.contains(svc)
    }

    private fun showAllItems() {
        val previews = StorageManager.getAllPreviews()
        if (previews.isEmpty()) {
            Toast.makeText(this, "Пусто", Toast.LENGTH_SHORT).show()
            return
        }
        val items = previews.map { "#${it.id} (${it.length}) ${it.preview}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Записи (${previews.size})")
            .setItems(items) { _, i ->
                Thread {
                    val txt = StorageManager.getText(previews[i].id)
                    handler.post { if (txt != null) showText(previews[i].id, txt) }
                }.start()
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun showText(id: Int, text: String) {
        val sv = ScrollView(this)
        val tv = TextView(this).apply {
            this.text = text
            textSize = 12f
            setPadding(24, 24, 24, 24)
            setTextIsSelectable(true)
            movementMethod = ScrollingMovementMethod()
        }
        sv.addView(tv)
        AlertDialog.Builder(this)
            .setTitle("#$id (${text.length})")
            .setView(sv)
            .setPositiveButton("Удалить") { _, _ ->
                StorageManager.deleteItem(id)
                Toast.makeText(this, "Удалено", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Закрыть", null)
            .show()
    }
}
