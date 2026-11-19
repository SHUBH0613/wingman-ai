package com.wingman.ai

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class WingmanAccessibilityService : AccessibilityService() {

    companion object {
        var instance: WingmanAccessibilityService? = null
            private set

        fun getVisibleScreenText(): String {
            val service = instance ?: return ""
            val rootNode = service.rootInActiveWindow ?: return ""
            val sb = StringBuilder()
            extractText(rootNode, sb)
            return sb.toString().trim()
        }

        private fun extractText(node: AccessibilityNodeInfo?, sb: StringBuilder) {
            if (node == null) return
            val text = node.text
            if (!text.isNullOrBlank()) {
                sb.append(text).append("\n")
            }
            for (i in 0 until node.childCount) {
                extractText(node.getChild(i), sb)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Active on-demand via getVisibleScreenText()
    }

    override fun onInterrupt() {
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
