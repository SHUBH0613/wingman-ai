package com.wingman.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FloatingOverlayService : Service() {

    companion object {
        var isRunning = false
            private set
        const val ACTION_STOP = "com.wingman.ai.ACTION_STOP"

        // Anti-Ban Velocity Tracker
        private var recentTypeTimestamps = mutableListOf<Long>()
        fun checkSpamVelocity(): Boolean {
            val now = System.currentTimeMillis()
            recentTypeTimestamps.removeAll { now - it > 120_000 } // Keep last 2 minutes
            return recentTypeTimestamps.size >= 6
        }
        fun recordTypeEvent() {
            recentTypeTimestamps.add(System.currentTimeMillis())
        }
    }

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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(101, notification)
        }
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
                    if (bubbleView.isAttachedToWindow) {
                        windowManager.updateViewLayout(bubbleView, bubbleParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        openMenuAndAutoDraft()
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
        val btnAutoType = menuView.findViewById<Button>(R.id.btnAutoType)

        btnClose.setOnClickListener { closeMenu() }

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

        btnOpener.setOnClickListener { triggerAi(DatingStage.STAGE_1_HOOK_OPENER) }
        btnBanter.setOnClickListener { triggerAi(DatingStage.STAGE_2_BANTER_TEASE) }
        btnDate.setOnClickListener { triggerAi(DatingStage.STAGE_4_DATE_CLOSER) }

        // Anti-Ban Humanized Stealth Auto-Type
        btnAutoType.setOnClickListener {
            if (selectedReply.isNotBlank()) {
                if (checkSpamVelocity()) {
                    Toast.makeText(this, "⏳ Anti-Ban: Slow down for 30s to keep profile safe!", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                recordTypeEvent()
                val textToInject = selectedReply

                // Minimize menu first to prevent touch-obscuring security flags
                closeMenu()

                serviceScope.launch {
                    val typed = WingmanAccessibilityService.autoTypeWithHumanJitter(textToInject)
                    if (typed) {
                        Toast.makeText(this@FloatingOverlayService, "⚡ Auto-typed! Review and press Send.", Toast.LENGTH_SHORT).show()
                    } else {
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Reply", textToInject))
                        Toast.makeText(this@FloatingOverlayService, "Copied to clipboard! (Enable Accessibility for auto-typing)", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun triggerAi(stage: DatingStage) {
        val prefs = getSharedPreferences("wingman_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("gemini_api_key", "") ?: FullUserProfile.BUNDLED_DEFAULT_API_KEY
        val profile = FullUserProfile.fromPreferences(prefs)

        val etContext = menuView.findViewById<EditText>(R.id.etChatContext)
        val progressBar = menuView.findViewById<ProgressBar>(R.id.progressBar)
        val btnAutoType = menuView.findViewById<Button>(R.id.btnAutoType)
        val tvOption1 = menuView.findViewById<TextView>(R.id.tvOption1)
        val tvOption2 = menuView.findViewById<TextView>(R.id.tvOption2)
        val tvOption3 = menuView.findViewById<TextView>(R.id.tvOption3)

        // Auto-read messages from screen
        var contextText = WingmanAccessibilityService.extractChatContext()
        if (contextText.isBlank()) {
            contextText = etContext.text.toString().trim()
        }
        if (contextText.isBlank()) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val primaryClip = clipboard.primaryClip
            if (primaryClip != null && primaryClip.itemCount > 0) {
                contextText = primaryClip.getItemAt(0)?.text?.toString() ?: ""
            }
        }

        etContext.setText(contextText.take(150))
        progressBar.visibility = View.VISIBLE
        btnAutoType.visibility = View.GONE
        tvOption1.text = "Reading chat & crafting charming replies..."
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
                highlightOption(tvOption1, tvOption2, tvOption3)

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

    private fun highlightOption(selected: TextView, other1: TextView, other2: TextView) {
        selected.setBackgroundColor(Color.parseColor("#BE123C"))
        other1.setBackgroundColor(Color.parseColor("#334155"))
        other2.setBackgroundColor(Color.parseColor("#334155"))
    }

    private fun openMenuAndAutoDraft() {
        if (!isMenuOpen) {
            windowManager.addView(menuView, menuParams)
            bubbleView.visibility = View.GONE
            isMenuOpen = true
            // Instant 1-tap read & draft!
            triggerAi(DatingStage.AUTO_DETECT)
        }
    }

    private fun closeMenu() {
        if (isMenuOpen && menuView.isAttachedToWindow) {
            windowManager.removeView(menuView)
            bubbleView.visibility = View.VISIBLE
            isMenuOpen = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
        if (::bubbleView.isInitialized && bubbleView.isAttachedToWindow) {
            windowManager.removeView(bubbleView)
        }
        if (isMenuOpen && ::menuView.isInitialized && menuView.isAttachedToWindow) {
            windowManager.removeView(menuView)
        }
    }
}
