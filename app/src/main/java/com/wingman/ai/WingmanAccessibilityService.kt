package com.wingman.ai

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

class WingmanAccessibilityService : AccessibilityService() {

    companion object {
        var instance: WingmanAccessibilityService? = null
            private set

        private val ALLOWED_PACKAGE_PREFIXES = listOf(
            "com.bumble",
            "co.hinge",
            "com.tinder",
            "com.instagram",
            "com.wingman"
        )

        fun isCurrentAppAllowed(): Boolean {
            val service = instance ?: return true
            val pkg = service.rootInActiveWindow?.packageName?.toString() ?: ""
            if (pkg.isBlank()) return true
            return ALLOWED_PACKAGE_PREFIXES.any { pkg.startsWith(it, ignoreCase = true) }
        }

        /**
         * Extracts visible conversation history, distinguishing between
         * the match's incoming texts and the user's outgoing texts.
         */
        fun extractChatContext(): String {
            val service = instance ?: return ""
            val rootNode = service.rootInActiveWindow ?: return ""

            if (!isCurrentAppAllowed()) {
                return "[Protected app - Wingman only operates on Hinge, Bumble, Tinder, and Instagram]"
            }

            val chatLines = mutableListOf<String>()
            extractChatNodes(rootNode, chatLines)

            val fullText = chatLines.joinToString("\n").trim()
            return fullText.take(2000)
        }

        private fun extractChatNodes(node: AccessibilityNodeInfo?, lines: MutableList<String>) {
            if (node == null) return
            val text = node.text?.toString()?.trim()

            if (!text.isNullOrBlank() && text.length > 1 && !isSystemUiText(text)) {
                // Determine alignment: left-aligned typically incoming, right-aligned outgoing
                val rect = android.graphics.Rect()
                node.getBoundsInScreen(rect)
                val displayMetrics = instance?.resources?.displayMetrics
                val screenWidth = displayMetrics?.widthPixels ?: 1080

                val prefix = if (rect.centerX() < screenWidth / 2) "Match:" else "You:"
                lines.add("$prefix $text")
            }

            for (i in 0 until node.childCount) {
                extractChatNodes(node.getChild(i), lines)
            }
        }

        private fun isSystemUiText(text: String): Boolean {
            val ignored = listOf("type a message", "say something", "reply", "send", "chat", "online", "active now", "read")
            return ignored.any { text.equals(it, ignoreCase = true) }
        }

        fun getVisibleScreenText(): String {
            val service = instance ?: return ""
            val rootNode = service.rootInActiveWindow ?: return ""

            if (!isCurrentAppAllowed()) {
                return ""
            }

            val sb = StringBuilder()
            extractAllText(rootNode, sb)
            return sb.toString().trim().take(4000)
        }

        private fun extractAllText(node: AccessibilityNodeInfo?, sb: StringBuilder) {
            if (node == null) return
            val text = node.text
            if (!text.isNullOrBlank()) {
                sb.append(text).append("\n")
            }
            for (i in 0 until node.childCount) {
                extractAllText(node.getChild(i), sb)
            }
        }

        /**
         * Anti-Ban Humanized Typing:
         * Simulates natural human typing jitter with 20-45ms variance.
         * Injects directly into Hinge/Bumble input box and leaves cursor ready.
         */
        suspend fun autoTypeWithHumanJitter(textToType: String): Boolean = withContext(Dispatchers.Main) {
            val service = instance ?: return@withContext false
            val rootNode = service.rootInActiveWindow ?: return@withContext false

            val editableNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: findEditableNode(rootNode)
                ?: return@withContext false

            // Type in natural word/character bursts with simulated human jitter
            val currentBuilder = StringBuilder()
            val chars = textToType.toCharArray()

            for (i in chars.indices) {
                currentBuilder.append(chars[i])
                val args = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        currentBuilder.toString()
                    )
                }
                editableNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

                // Simulated human typing delay: 18ms to 42ms with pause at spaces
                val delayTime = if (chars[i] == ' ') {
                    Random.nextLong(60, 110)
                } else {
                    Random.nextLong(18, 42)
                }
                delay(delayTime)
            }
            true
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
        // Active on-demand
    }

    override fun onInterrupt() {
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
