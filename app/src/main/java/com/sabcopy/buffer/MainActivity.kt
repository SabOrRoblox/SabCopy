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
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var adapter: ItemAdapter
    private lateinit var statusText: TextView
    private lateinit var statusToggle: Switch
    private lateinit var countValue: TextView

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

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(bgColor)
        val padding = (32 * resources.displayMetrics.density).toInt()
        root.setPadding(padding, padding, padding, padding)

        val title = TextView(this)
        title.text = "ClipVault"
        title.textSize = 30f
        title.setTextColor(accentColor)
        root.addView(title)

        val subtitle = TextView(this)
        subtitle.text = "умный буфер обмена"
        subtitle.textSize = 14f
        subtitle.setTextColor(subColor)
        subtitle.setPadding(0, 0, 0, (24 * resources.displayMetrics.density).toInt())
        root.addView(subtitle)

        val accOk = isAccessibilityEnabled()

        val statusCard = LinearLayout(this)
        statusCard.orientation = LinearLayout.HORIZONTAL
        statusCard.setBackgroundColor(cardColor)
        val cardPadding = (20 * resources.displayMetrics.density).toInt()
        statusCard.setPadding(cardPadding, cardPadding, cardPadding, cardPadding)
        val scParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        scParams.bottomMargin = (16 * resources.displayMetrics.density).toInt()
        statusCard.layoutParams = scParams

        statusText = TextView(this)
        statusText.text = if (accOk) "Служба активна" else "Служба отключена"
        statusText.textSize = 15f
        statusText.setTextColor(if (accOk) 0xFF2E7D32.toInt() else 0xFFB71C1C.toInt())
        val stParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        statusText.layoutParams = stParams
        statusCard.addView(statusText)

        statusToggle = Switch(this)
        statusToggle.isChecked = accOk
        statusToggle.setOnCheckedChangeListener { _, _ ->
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        statusCard.addView(statusToggle)
        root.addView(statusCard)

        val countCard = LinearLayout(this)
        countCard.orientation = LinearLayout.VERTICAL
        countCard.setBackgroundColor(cardColor)
        countCard.setPadding(cardPadding, cardPadding, cardPadding, cardPadding)
        val ccParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        ccParams.bottomMargin = (16 * resources.displayMetrics.density).toInt()
        countCard.layoutParams = ccParams

        val countLabel = TextView(this)
        countLabel.text = "Сохранено записей"
        countLabel.textSize = 12f
        countLabel.setTextColor(subColor)
        countCard.addView(countLabel)

        countValue = TextView(this)
        countValue.text = "${StorageManager.getCount()}"
        countValue.textSize = 24f
        countValue.setTextColor(textColor)
        countCard.addView(countValue)
        root.addView(countCard)

        val captureBtn = Button(this)
        captureBtn.text = "Захватить из текущего поля"
        captureBtn.setTextColor(0xFFFFFFFF.toInt())
        captureBtn.textSize = 14f
        captureBtn.setBackgroundColor(accentColor)
        captureBtn.setPadding(0, (18 * resources.displayMetrics.density).toInt(), 0, (18 * resources.displayMetrics.density).toInt())
        val bp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        bp.bottomMargin = (10 * resources.displayMetrics.density).toInt()
        captureBtn.layoutParams = bp
        captureBtn.setOnClickListener {
            val acc = HcbAccessibilityService.instance
            if (acc == null || !HcbAccessibilityService.isRunning) {
                Toast.makeText(this, "Служба не активна", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val t = acc.grabText()
            if (t.isNotEmpty()) {
                val id = StorageManager.addText(t)
                refreshList()
                Toast.makeText(this, "Захвачено #$id (${t.length})", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Ничего не выделено", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(captureBtn)

        val pasteBtn = Button(this)
        pasteBtn.text = "Вставить последнюю"
        pasteBtn.setTextColor(0xFFFFFFFF.toInt())
        pasteBtn.textSize = 14f
        pasteBtn.setBackgroundColor(0xFF2E7D32.toInt())
        pasteBtn.setPadding(0, (18 * resources.displayMetrics.density).toInt(), 0, (18 * resources.displayMetrics.density).toInt())
        pasteBtn.layoutParams = bp
        pasteBtn.setOnClickListener {
            Toast.makeText(this, "Вставка работает только из шторки уведомлений", Toast.LENGTH_LONG).show()
        }
        root.addView(pasteBtn)

        val clearBtn = Button(this)
        clearBtn.text = "Очистить хранилище"
        clearBtn.setTextColor(0xFFFFFFFF.toInt())
        clearBtn.textSize = 14f
        clearBtn.setBackgroundColor(0xFFB71C1C.toInt())
        clearBtn.setPadding(0, (18 * resources.displayMetrics.density).toInt(), 0, (18 * resources.displayMetrics.density).toInt())
        clearBtn.layoutParams = bp
        clearBtn.setOnClickListener {
            StorageManager.clearAll()
            refreshList()
            countValue.text = "0"
            Toast.makeText(this, "Очищено", Toast.LENGTH_SHORT).show()
        }
        root.addView(clearBtn)

        val recycler = RecyclerView(this)
        recycler.layoutManager = LinearLayoutManager(this)
        val rvParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        rvParams.topMargin = (16 * resources.displayMetrics.density).toInt()
        recycler.layoutParams = rvParams
        adapter = ItemAdapter(textColor, cardColor) { id, text -> showText(id, text) }
        recycler.adapter = adapter
        root.addView(recycler)

        setContentView(root)
        refreshList()
    }

    override fun onResume() {
        super.onResume()
        val accOk = isAccessibilityEnabled()
        statusText.text = if (accOk) "Служба активна" else "Служба отключена"
        statusText.setTextColor(if (accOk) 0xFF2E7D32.toInt() else 0xFFB71C1C.toInt())
        statusToggle.isChecked = accOk
        refreshList()
        countValue.text = "${StorageManager.getCount()}"
    }

    private fun refreshList() {
        val previews = StorageManager.getAllPreviews()
        adapter.submitList(previews)
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val fullName = "$packageName/com.sabcopy.buffer.HcbAccessibilityService"
        val shortName = "$packageName/.HcbAccessibilityService"
        return enabledServices.split(":").any { it == fullName || it == shortName }
    }

    private fun showText(id: Int, text: String) {
        val scroll = android.widget.ScrollView(this)
        val tv = TextView(this).apply {
            this.text = text
            textSize = 14f
            setPadding(24, 24, 24, 24)
            setTextIsSelectable(true)
        }
        scroll.addView(tv)
        AlertDialog.Builder(this)
            .setTitle("#$id (${text.length})")
            .setView(scroll)
            .setPositiveButton("Удалить") { _, _ ->
                StorageManager.deleteItem(id)
                refreshList()
                countValue.text = "${StorageManager.getCount()}"
                Toast.makeText(this, "Удалено", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Закрыть", null)
            .show()
    }

    class ItemAdapter(
        private val textColor: Int,
        private val cardColor: Int,
        private val onClick: (Int, String) -> Unit
    ) : RecyclerView.Adapter<ItemAdapter.VH>() {

        private var items: List<<StorageManager.ItemPreview> = emptyList()

        fun submitList(list: List<<StorageManager.ItemPreview>) {
            items = list
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tv = TextView(parent.context)
            tv.setPadding(24, 24, 24, 24)
            tv.textSize = 14f
            tv.setTextColor(textColor)
            tv.setBackgroundColor(cardColor)
            val params = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = 8
            tv.layoutParams = params
            return VH(tv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.tv.text = "#${item.id} (${item.length}) ${item.preview}"
            holder.tv.setOnClickListener {
                Thread {
                    val txt = StorageManager.getText(item.id)
                    if (txt != null) {
                        Handler(Looper.getMainLooper()).post { onClick(item.id, txt) }
                    }
                }.start()
            }
        }

        class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)
    }
}

