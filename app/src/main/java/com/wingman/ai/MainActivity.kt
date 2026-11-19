package com.wingman.ai

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    // Step 1: Vitals
    private lateinit var etApiKey: EditText
    private lateinit var etName: EditText
    private lateinit var etAge: EditText
    private lateinit var etHeight: EditText
    private lateinit var etCity: EditText
    private lateinit var etWorkCollege: EditText

    // Step 2: Lifestyle
    private lateinit var etDatingGoal: EditText
    private lateinit var etHabits: EditText

    // Step 3: Interests & Quirks
    private lateinit var etHobbiesSkills: EditText
    private lateinit var etMusicFood: EditText
    private lateinit var btnGenerateProfile: Button
    private lateinit var tvProfileResult: TextView

    // Photo Auditor
    private lateinit var btnSelectPhotos: Button
    private lateinit var tvPhotoAuditResult: TextView

    // Viral Hooks
    private lateinit var tvViralHooks: TextView

    // Activations
    private lateinit var btnToggleOverlay: Button
    private lateinit var btnEnableAccessibility: Button
    private lateinit var btnEnableKeyboard: Button
    private lateinit var btnSaveSettings: Button

    private val pickPhotosLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            auditSelectedPhotos(uris.take(10))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadSettings()
        displayViralHooks()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateOverlayButtonState()
    }

    private fun initViews() {
        etApiKey = findViewById(R.id.etApiKey)
        etName = findViewById(R.id.etName)
        etAge = findViewById(R.id.etAge)
        etHeight = findViewById(R.id.etHeight)
        etCity = findViewById(R.id.etCity)
        etWorkCollege = findViewById(R.id.etWorkCollege)

        etDatingGoal = findViewById(R.id.etDatingGoal)
        etHabits = findViewById(R.id.etHabits)

        etHobbiesSkills = findViewById(R.id.etHobbiesSkills)
        etMusicFood = findViewById(R.id.etMusicFood)
        btnGenerateProfile = findViewById(R.id.btnGenerateProfile)
        tvProfileResult = findViewById(R.id.tvProfileResult)

        btnSelectPhotos = findViewById(R.id.btnSelectPhotos)
        tvPhotoAuditResult = findViewById(R.id.tvPhotoAuditResult)
        tvViralHooks = findViewById(R.id.tvViralHooks)

        btnToggleOverlay = findViewById(R.id.btnToggleOverlay)
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)
        btnEnableKeyboard = findViewById(R.id.btnEnableKeyboard)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
    }

    private fun displayViralHooks() {
        val hooks = ViralHookLibrary.curatedHooks.take(5)
        val sb = StringBuilder()
        for (hook in hooks) {
            sb.append("• [${hook.category} - ${hook.language}]:\n")
            sb.append("\"${hook.hookText}\"\n")
            sb.append("Why it works: ${hook.strategyNote}\n\n")
        }
        tvViralHooks.text = sb.toString().trim()
    }

    private fun setupListeners() {
        btnSaveSettings.setOnClickListener {
            saveSettings()
            Toast.makeText(this, "Profile & API Key saved!", Toast.LENGTH_SHORT).show()
        }

        btnSelectPhotos.setOnClickListener {
            saveSettings()
            pickPhotosLauncher.launch("image/*")
        }

        btnGenerateProfile.setOnClickListener {
            saveSettings()
            runProfileGeneration()
        }

        btnToggleOverlay.setOnClickListener {
            saveSettings()
            toggleOverlayService()
        }

        btnEnableAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Enable 'Wingman Dating Assistant' for Auto-Read & Auto-Type!", Toast.LENGTH_LONG).show()
        }

        btnEnableKeyboard.setOnClickListener {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Enable 'Wingman AI Keyboard'", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateOverlayButtonState() {
        if (FloatingOverlayService.isRunning) {
            btnToggleOverlay.text = "Stop Floating Overlay Bubble"
            btnToggleOverlay.setBackgroundColor(android.graphics.Color.parseColor("#475569"))
        } else {
            btnToggleOverlay.text = "Start Floating Overlay Bubble (⚡ Auto-Type Ready)"
            btnToggleOverlay.setBackgroundColor(android.graphics.Color.parseColor("#E11D48"))
        }
    }

    private fun toggleOverlayService() {
        if (FloatingOverlayService.isRunning) {
            val stopIntent = Intent(this, FloatingOverlayService::class.java).apply {
                action = FloatingOverlayService.ACTION_STOP
            }
            startService(stopIntent)
            Toast.makeText(this, "Floating Overlay Stopped", Toast.LENGTH_SHORT).show()
            btnToggleOverlay.text = "Start Floating Overlay Bubble (⚡ Auto-Type Ready)"
            btnToggleOverlay.setBackgroundColor(android.graphics.Color.parseColor("#E11D48"))
        } else {
            checkOverlayPermissionAndStart()
        }
    }

    private fun runProfileGeneration() {
        val apiKey = etApiKey.text.toString().trim().ifBlank { FullUserProfile.BUNDLED_DEFAULT_API_KEY }

        tvProfileResult.visibility = View.VISIBLE
        tvProfileResult.text = "Crafting high-conversion Hinge & Bumble packs with Gemini..."

        val profile = buildFullProfile()

        lifecycleScope.launch {
            val result = ProfileGenerator.generateProfilePacks(apiKey, profile)
            if (result != null) {
                val sb = StringBuilder("✨ GENERATED PROFILE SUITE:\n\n")
                sb.append("🐝 BUMBLE BIOS (English & Hinglish):\n")
                result.bumbleBios.forEachIndexed { i, bio ->
                    sb.append("${i + 1}. \"$bio\"\n\n")
                }
                sb.append("Bonus Hinglish Bio:\n\"${result.hinglishBonusBio}\"\n\n")

                sb.append("🎯 HINGE PROMPT PACKS:\n")
                result.hingePacks.forEachIndexed { packIdx, pack ->
                    sb.append("--- Pack #${packIdx + 1} ---\n")
                    pack.forEach { prompt ->
                        sb.append("Q: ${prompt.promptTitle}\n")
                        sb.append("A: ${prompt.answer}\n\n")
                    }
                }
                tvProfileResult.text = sb.toString()
            } else {
                tvProfileResult.text = "Generation failed. Verify network connection."
            }
        }
    }

    private fun auditSelectedPhotos(uris: List<Uri>) {
        val apiKey = etApiKey.text.toString().trim().ifBlank { FullUserProfile.BUNDLED_DEFAULT_API_KEY }

        tvPhotoAuditResult.visibility = View.VISIBLE
        tvPhotoAuditResult.text = "Downsampling & auditing ${uris.size} photos with Gemini Vision..."

        lifecycleScope.launch {
            val base64List = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    try {
                        contentResolver.openInputStream(uri)?.use { input ->
                            // Two-pass memory-safe downsampling to prevent OOM
                            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeStream(input, null, options)

                            val targetSize = 640
                            var inSampleSize = 1
                            if (options.outHeight > targetSize || options.outWidth > targetSize) {
                                val halfHeight = options.outHeight / 2
                                val halfWidth = options.outWidth / 2
                                while (halfHeight / inSampleSize >= targetSize && halfWidth / inSampleSize >= targetSize) {
                                    inSampleSize *= 2
                                }
                            }

                            contentResolver.openInputStream(uri)?.use { secondInput ->
                                val decodeOptions = BitmapFactory.Options().apply {
                                    this.inSampleSize = inSampleSize
                                    inPreferredConfig = Bitmap.Config.RGB_565
                                }
                                val bitmap = BitmapFactory.decodeStream(secondInput, null, decodeOptions)
                                    ?: return@use null

                                val scaled = if (bitmap.width > targetSize || bitmap.height > targetSize) {
                                    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                                    val newW = if (ratio >= 1) targetSize else (targetSize * ratio).toInt()
                                    val newH = if (ratio >= 1) (targetSize / ratio).toInt() else targetSize
                                    Bitmap.createScaledBitmap(bitmap, newW.coerceAtLeast(1), newH.coerceAtLeast(1), true)
                                } else {
                                    bitmap
                                }

                                val baos = ByteArrayOutputStream()
                                scaled.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                                val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                                if (scaled != bitmap) scaled.recycle()
                                bitmap.recycle()
                                b64
                            }
                        }
                    } catch (t: Throwable) {
                        null
                    }
                }
            }

            if (base64List.isEmpty()) {
                tvPhotoAuditResult.text = "Could not process selected photos. Please try different images."
                return@launch
            }

            val report = PhotoAuditor.auditPhotos(apiKey, base64List)
            if (report != null) {
                val sb = StringBuilder("📸 AI Photo Audit & Attractiveness Report:\n\n")
                sb.append("Diagnosis: ").append(report.overallSummary).append("\n\n")
                sb.append("Optimal 6-Photo Sequence:\n")
                sb.append(report.optimalSequence.map { "#${it + 1}" }.joinToString(" ➔ ")).append("\n\n")

                for (score in report.photoScores) {
                    sb.append("Photo #${score.photoIndex + 1} (Score: ${score.overallScore}/10)\n")
                    sb.append("• Lighting: ${score.lightingScore}/10 | Smile: ${score.smileAndEyeContact}/10\n")
                    if (score.redFlags.isNotEmpty()) {
                        sb.append("• Red Flags: ${score.redFlags.joinToString(", ")}\n")
                    }
                    sb.append("• Placement Advice: ${score.recommendation}\n\n")
                }
                tvPhotoAuditResult.text = sb.toString()
            } else {
                tvPhotoAuditResult.text = "Audit failed. Check network or verify API key."
            }
        }
    }

    private fun buildFullProfile(): FullUserProfile {
        return FullUserProfile(
            vitals = UserVitals(
                name = etName.text.toString().trim().ifBlank { "User" },
                age = etAge.text.toString().toIntOrNull() ?: 25,
                height = etHeight.text.toString().trim().ifBlank { "5'11" },
                city = etCity.text.toString().trim().ifBlank { "City Center" },
                work = etWorkCollege.text.toString().trim()
            ),
            lifestyle = UserLifestyle(
                datingIntention = etDatingGoal.text.toString().trim().ifBlank { "Looking for connection" },
                workout = etHabits.text.toString().trim()
            ),
            interests = UserInterests(
                hobbies = etHobbiesSkills.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() },
                musicAndFood = etMusicFood.text.toString().trim()
            )
        )
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("wingman_prefs", Context.MODE_PRIVATE)
        val savedKey = prefs.getString("gemini_api_key", "") ?: ""
        etApiKey.setText(if (savedKey.isNotBlank()) savedKey else FullUserProfile.BUNDLED_DEFAULT_API_KEY)

        etName.setText(prefs.getString("user_name", ""))
        etAge.setText(prefs.getString("user_age", ""))
        etHeight.setText(prefs.getString("user_height", ""))
        etCity.setText(prefs.getString("user_city", ""))
        etWorkCollege.setText(prefs.getString("user_work", ""))
        etDatingGoal.setText(prefs.getString("user_intent", ""))
        etHabits.setText(prefs.getString("user_habits", ""))
        etHobbiesSkills.setText(prefs.getString("user_hobbies", ""))
        etMusicFood.setText(prefs.getString("user_music_food", ""))
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("wingman_prefs", Context.MODE_PRIVATE)
        val keyToSave = etApiKey.text.toString().trim().ifBlank { FullUserProfile.BUNDLED_DEFAULT_API_KEY }
        prefs.edit()
            .putString("gemini_api_key", keyToSave)
            .putString("user_name", etName.text.toString().trim())
            .putString("user_age", etAge.text.toString().trim())
            .putString("user_height", etHeight.text.toString().trim())
            .putString("user_city", etCity.text.toString().trim())
            .putString("user_work", etWorkCollege.text.toString().trim())
            .putString("user_intent", etDatingGoal.text.toString().trim())
            .putString("user_habits", etHabits.text.toString().trim())
            .putString("user_hobbies", etHobbiesSkills.text.toString().trim())
            .putString("user_music_food", etMusicFood.text.toString().trim())
            .apply()
    }

    private fun checkOverlayPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please grant 'Display over other apps' permission", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            val intent = Intent(this, FloatingOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "Wingman Floating Bubble Started! (⚡ Anti-Ban Stealth Ready)", Toast.LENGTH_LONG).show()
            btnToggleOverlay.text = "Stop Floating Overlay Bubble"
            btnToggleOverlay.setBackgroundColor(android.graphics.Color.parseColor("#475569"))
        }
    }
}
