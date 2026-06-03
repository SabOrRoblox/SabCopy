package com.sabcopy.buffer

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.ViewGroup
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

        val root = ScrollView(this).apply {
            setBackgroundColor(bgColor)
            setPadding(0, 0, 0, 0)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 64, 32, 48)
        }

        container.addView(TextView(this).apply {
            text = "ClipVault"
            textSize = 30f
            setTextColor(accentColor)
            setPadding(0, 0, 0, 4)
        })

        container.addView(TextView(this).apply {
            text = "умный буфер обмена"
            textSize = 14f
            setTextColor(subColor)
            setPadding(0, 0, 0, 32)
        })

        val accOk = isAccessibilityEnabled()
        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(cardColor)
            setPadding(24, 20, 24, 20)
            (layoutParams as LinearLayout.LayoutParams).bottomMargin = 16
        }

        val statusText = TextView(this).apply {
            text = if (accOk) "Служба активна" else "Служба отключена"
            textSize = 15f
            setTextColor(if (accOk) 0xFF2E7D32.toInt() else 0xFFB71C1C.toInt())
            (layoutParams as LinearLayout.LayoutParams).weight = 1f
        }
        statusCard.addView(statusText)

        val toggle = Switch(this).apply {
            isChecked = accOk
            setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } else {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
        }
        statusCard.addView(toggle)
        container.addView(statusCard)

        container.addView(card("Сохранено записей", "${StorageManager.getCount()}", cardColor, textColor, subColor))

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
            Toast.makeText(this, "Очищено", Toast.LENGTH_SHORT).show()
        })

        root.addView(container)
        setContentView(root)
    }

    private fun card(title: String, value: String, bg: Int, textColor: Int, subColor: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(24, 20, 24, 20)
            (layoutParams as LinearLayout.LayoutParams).bottomMargin = 16
            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 12f
                setTextColor(subColor)
            })
            addView(TextView(this@MainActivity).apply {
                text = value
                textSize = 24f
                setTextColor(textColor)
            })
        }
    }

    private fun btn(text: String, color: Int, click: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
            setBackgroundColor(color)
            setPadding(0, 18, 0, 18)
            (layoutParams as LinearLayout.LayoutParams).bottomMargin = 10
            setOnClickListener { click() }
        }
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
