package com.wingman.ai

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
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

        /**
         * Auto-types the chosen text directly into Hinge/Bumble/IG's active input box.
         * Leaves the cursor ready so the user can review and manually press Send!
         */
        fun autoTypeIntoChat(textToType: String): Boolean {
            val service = instance ?: return false
            val rootNode = service.rootInActiveWindow ?: return false

            val editableNode = findEditableNode(rootNode)
            if (editableNode != null) {
                val arguments = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        textToType
                    )
                }
                return editableNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            }
            return false
        }

        private fun findEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.isEditable) return node
            if (node.className?.toString()?.contains("EditText", ignoreCase = true) == true) {
                return node
            }
            for (i in 0 until node.childCount) {
                val child = findEditableNode(node.getChild(i))
                if (child != null) return child
            }
            return null
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Accessibility active
    }

    override fun onInterrupt() {
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
