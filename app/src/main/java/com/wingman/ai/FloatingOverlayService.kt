package com.wingman.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private lateinit var menuView: View

    private var bubbleParams: WindowManager.LayoutParams? = null
    private var menuParams: WindowManager.LayoutParams? = null

    private var isMenuOpen = false
    private var activeLanguage = LanguageMode.ENGLISH
    private var selectedReply: String = ""
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        startForegroundServiceNotification()
        setupBubbleView()
        setupMenuView()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "wingman_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Wingman Floating Bubble",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Wingman AI Active")
            .setContentText("Tap the floating flame over Hinge/Bumble for instant replies")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()

        startForeground(101, notification)
    }

    private fun setupBubbleView() {
        bubbleView = LayoutInflater.from(this).inflate(R.layout.layout_floating_bubble, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        bubbleView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams!!.x
                    initialY = bubbleParams!!.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        isClick = false
                    }
                    bubbleParams!!.x = initialX + deltaX
                    bubbleParams!!.y = initialY + deltaY
                    windowManager.updateViewLayout(bubbleView, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        openMenu()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(bubbleView, bubbleParams)
    }

    private fun setupMenuView() {
        menuView = LayoutInflater.from(this).inflate(R.layout.layout_floating_menu, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        menuParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val btnClose = menuView.findViewById<TextView>(R.id.btnCloseMenu)
        val tvLangEn = menuView.findViewById<TextView>(R.id.tvLangEnglish)
        val tvLangHinglish = menuView.findViewById<TextView>(R.id.tvLangHinglish)
        val btnOpener = menuView.findViewById<Button>(R.id.btnActionOpener)
        val btnBanter = menuView.findViewById<Button>(R.id.btnActionBanter)
        val btnDate = menuView.findViewById<Button>(R.id.btnActionDate)
        val etContext = menuView.findViewById<EditText>(R.id.etChatContext)
        val progressBar = menuView.findViewById<ProgressBar>(R.id.progressBar)
        val tvOption1 = menuView.findViewById<TextView>(R.id.tvOption1)
        val tvOption2 = menuView.findViewById<TextView>(R.id.tvOption2)
        val tvOption3 = menuView.findViewById<TextView>(R.id.tvOption3)
        val btnAutoType = menuView.findViewById<Button>(R.id.btnAutoType)

        btnClose.setOnClickListener { closeMenu() }

        // Language toggle
        tvLangEn.setOnClickListener {
            activeLanguage = LanguageMode.ENGLISH
            tvLangEn.setBackgroundColor(Color.parseColor("#E11D48"))
            tvLangEn.setTextColor(Color.WHITE)
            tvLangHinglish.setBackgroundColor(Color.parseColor("#334155"))
            tvLangHinglish.setTextColor(Color.parseColor("#94A3B8"))
        }

        tvLangHinglish.setOnClickListener {
            activeLanguage = LanguageMode.HINGLISH
            tvLangHinglish.setBackgroundColor(Color.parseColor("#E11D48"))
            tvLangHinglish.setTextColor(Color.WHITE)
            tvLangEn.setBackgroundColor(Color.parseColor("#334155"))
            tvLangEn.setTextColor(Color.parseColor("#94A3B8"))
        }

        fun triggerAi(stage: DatingStage) {
            val prefs = getSharedPreferences("wingman_prefs", Context.MODE_PRIVATE)
            val apiKey = prefs.getString("gemini_api_key", "") ?: ""

            val profile = FullUserProfile(
                vitals = UserVitals(
                    name = prefs.getString("user_name", "") ?: "",
                    city = prefs.getString("user_spots", "") ?: "",
                    work = prefs.getString("user_work", "") ?: ""
                ),
                lifestyle = UserLifestyle(
                    datingIntention = prefs.getString("user_intent", "Dating") ?: ""
                ),
                interests = UserInterests(
                    hobbies = (prefs.getString("user_passions", "") ?: "").split(",").map { it.trim() }
                )
            )

            // 1. Accessibility screen reader
            var contextText = WingmanAccessibilityService.getVisibleScreenText()
            if (contextText.isBlank()) {
                contextText = etContext.text.toString().trim()
            }
            if (contextText.isBlank()) {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                contextText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            }

            etContext.setText(contextText.take(120))
            progressBar.visibility = View.VISIBLE
            btnAutoType.visibility = View.GONE
            tvOption1.text = "Crafting human-like responses with Gemini..."
            tvOption2.visibility = View.GONE
            tvOption3.visibility = View.GONE

            serviceScope.launch {
                val bundle = GeminiDatingEngine.generateReplies(
                    apiKey = apiKey,
                    contextText = contextText,
                    stage = stage,
                    profile = profile,
                    language = activeLanguage,
                    flavor = HumorFlavor.WITTY_TEASE
                )
                progressBar.visibility = View.GONE

                val results = bundle.options
                if (results.isNotEmpty()) {
                    selectedReply = results[0]
                    tvOption1.text = results[0]
                    btnAutoType.visibility = View.VISIBLE
                    tvOption1.setOnClickListener {
                        selectedReply = results[0]
                        highlightOption(tvOption1, tvOption2, tvOption3)
                    }

                    if (results.size > 1) {
                        tvOption2.visibility = View.VISIBLE
                        tvOption2.text = results[1]
                        tvOption2.setOnClickListener {
                            selectedReply = results[1]
                            highlightOption(tvOption2, tvOption1, tvOption3)
                        }
                    }
                    if (results.size > 2) {
                        tvOption3.visibility = View.VISIBLE
                        tvOption3.text = results[2]
                        tvOption3.setOnClickListener {
                            selectedReply = results[2]
                            highlightOption(tvOption3, tvOption1, tvOption2)
                        }
                    }
                }
            }
        }

        btnOpener.setOnClickListener { triggerAi(DatingStage.STAGE_1_HOOK_OPENER) }
        btnBanter.setOnClickListener { triggerAi(DatingStage.STAGE_2_BANTER_TEASE) }
        btnDate.setOnClickListener { triggerAi(DatingStage.STAGE_4_DATE_CLOSER) }

        // Auto-Type execution
        btnAutoType.setOnClickListener {
            if (selectedReply.isNotBlank()) {
                val typed = WingmanAccessibilityService.autoTypeIntoChat(selectedReply)
                if (typed) {
                    Toast.makeText(this, "⚡ Auto-typed! Review and press Send.", Toast.LENGTH_SHORT).show()
                } else {
                    // Fallback to clipboard
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Reply", selectedReply))
                    Toast.makeText(this, "Copied to clipboard! (Enable Accessibility for auto-typing)", Toast.LENGTH_LONG).show()
                }
                closeMenu()
            }
        }
    }

    private fun highlightOption(selected: TextView, other1: TextView, other2: TextView) {
        selected.setBackgroundColor(Color.parseColor("#BE123C"))
        other1.setBackgroundColor(Color.parseColor("#334155"))
        other2.setBackgroundColor(Color.parseColor("#334155"))
    }

    private fun openMenu() {
        if (!isMenuOpen) {
            windowManager.addView(menuView, menuParams)
            bubbleView.visibility = View.GONE
            isMenuOpen = true
        }
    }

    private fun closeMenu() {
        if (isMenuOpen) {
            windowManager.removeView(menuView)
            bubbleView.visibility = View.VISIBLE
            isMenuOpen = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bubbleView.isInitialized) windowManager.removeView(bubbleView)
        if (isMenuOpen) windowManager.removeView(menuView)
    }
}
