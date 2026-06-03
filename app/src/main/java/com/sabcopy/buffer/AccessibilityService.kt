package com.sabcopy.buffer

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class HcbAccessibilityService : AccessibilityService() {

    private var lastText: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        StorageManager.init(this)
        NotificationHelper.createChannel(this)
        NotificationHelper.show(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            val root = rootInActiveWindow ?: return
            val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (node != null && node.text != null && node.textSelectionStart >= 0 && node.textSelectionEnd > node.textSelectionStart) {
                lastText = node.text.toString().substring(node.textSelectionStart, node.textSelectionEnd)
            }
            root.recycle()
        }
    }

    fun grabText(): String {
        val root = rootInActiveWindow
        if (root != null) {
            val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (node != null && node.text != null && node.textSelectionStart >= 0 && node.textSelectionEnd > node.textSelectionStart) {
                val s = node.text.toString().substring(node.textSelectionStart, node.textSelectionEnd)
                root.recycle()
                return s
            }
            root.recycle()
        }
        if (lastText != null && lastText!!.isNotEmpty()) return lastText!!
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (cm.hasPrimaryClip()) {
            val clip = cm.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val s = clip.getItemAt(0).text?.toString() ?: ""
                if (s.isNotEmpty()) return s
            }
        }
        return ""
    }

    fun putText(text: String) {
        val root = rootInActiveWindow ?: return
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (node == null) { root.recycle(); return }
        if (!node.isEditable) { root.recycle(); return }

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

        if (!ok) {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("", text))
            node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        }

        root.recycle()
    }

    override fun onInterrupt() {}
    override fun onDestroy() { super.onDestroy() }

    companion object { var instance: HcbAccessibilityService? = null }
    override fun onCreate() { super.onCreate(); instance = this }
}
