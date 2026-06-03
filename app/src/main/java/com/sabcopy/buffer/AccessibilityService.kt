package com.sabcopy.buffer

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class HcbAccessibilityService : AccessibilityService() {

    companion object {
        var instance: HcbAccessibilityService? = null
        var isRunning: Boolean = false
    }

    private var lastText: String? = null
    private val clipboard by lazy { getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        StorageManager.init(this)
        NotificationHelper.createChannel(this)
        NotificationHelper.show(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val root = rootInActiveWindow ?: return
                val node = findFocusedEditable(root)
                if (node != null && node.text != null) {
                    val start = node.textSelectionStart
                    val end = node.textSelectionEnd
                    if (start >= 0 && end > start) {
                        lastText = node.text.toString().substring(start, end)
                    }
                }
                root.recycle()
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        isRunning = false
        instance = null
        super.onDestroy()
    }

    fun grabText(): String {
        val root = rootInActiveWindow
        if (root != null) {
            val node = findFocusedEditable(root)
            if (node != null && node.text != null) {
                val start = node.textSelectionStart
                val end = node.textSelectionEnd
                if (start >= 0 && end > start) {
                    val s = node.text.toString().substring(start, end)
                    root.recycle()
                    return s
                }
            }
            root.recycle()
        }
        val lt = lastText
        if (!lt.isNullOrEmpty()) return lt
        if (clipboard.hasPrimaryClip()) {
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val s = clip.getItemAt(0).text?.toString() ?: ""
                if (s.isNotEmpty()) return s
            }
        }
        return ""
    }

    fun putText(text: String) {
        val root = rootInActiveWindow ?: return
        val node = findFocusedEditable(root)
        if (node == null) {
            root.recycle()
            return
        }
        if (!node.isEditable) {
            root.recycle()
            return
        }

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

        if (!ok) {
            clipboard.setPrimaryClip(ClipData.newPlainText("", text))
            node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        }

        root.recycle()
    }

    private fun findFocusedEditable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null) {
            if (focused.isEditable) return focused
            focused.recycle()
        }
        return findEditableRecursive(root)
    }

    private fun findEditableRecursive(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isFocused) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findEditableRecursive(child)
            if (result != null) return result
            child.recycle()
        }
        return null
    }
}

