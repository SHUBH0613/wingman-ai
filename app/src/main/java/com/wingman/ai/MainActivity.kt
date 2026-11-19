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

    private lateinit var etApiKey: EditText
    private lateinit var etCityAndSpots: EditText
    private lateinit var etPassions: EditText
    private lateinit var etWeekend: EditText
    private lateinit var etHumor: EditText

    private lateinit var btnSelectPhotos: Button
    private lateinit var tvPhotoAuditResult: TextView
    private lateinit var btnGenerateProfile: Button
    private lateinit var tvProfileResult: TextView

    private lateinit var btnToggleOverlay: Button
    private lateinit var btnEnableAccessibility: Button
    private lateinit var btnEnableKeyboard: Button
    private lateinit var btnSaveSettings: Button

    // Photo picker launcher (up to 10 photos)
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
        setupListeners()
    }

    private fun initViews() {
        etApiKey = findViewById(R.id.etApiKey)
        etCityAndSpots = findViewById(R.id.etCityAndSpots)
        etPassions = findViewById(R.id.etPassions)
        etWeekend = findViewById(R.id.etWeekend)
        etHumor = findViewById(R.id.etHumor)

        btnSelectPhotos = findViewById(R.id.btnSelectPhotos)
        tvPhotoAuditResult = findViewById(R.id.tvPhotoAuditResult)
        btnGenerateProfile = findViewById(R.id.btnGenerateProfile)
        tvProfileResult = findViewById(R.id.tvProfileResult)

        btnToggleOverlay = findViewById(R.id.btnToggleOverlay)
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)
        btnEnableKeyboard = findViewById(R.id.btnEnableKeyboard)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
    }

    private fun setupListeners() {
        btnSaveSettings.setOnClickListener {
            saveSettings()
            Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show()
        }

        btnSelectPhotos.setOnClickListener {
            saveSettings()
            pickPhotosLauncher.launch("image/*")
        }

        btnGenerateProfile.setOnClickListener {
            saveSettings()
            runProfileQuiz()
        }

        btnToggleOverlay.setOnClickListener {
            saveSettings()
            checkOverlayPermissionAndStart()
        }

        btnEnableAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Enable 'Wingman Dating Assistant'", Toast.LENGTH_LONG).show()
        }

        btnEnableKeyboard.setOnClickListener {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Enable 'Wingman AI Keyboard'", Toast.LENGTH_LONG).show()
        }
    }

    private fun auditSelectedPhotos(uris: List<Uri>) {
        val apiKey = etApiKey.text.toString().trim()
        if (apiKey.isBlank()) {
            Toast.makeText(this, "Please enter your Gemini API Key first!", Toast.LENGTH_SHORT).show()
            return
        }

        tvPhotoAuditResult.visibility = View.VISIBLE
        tvPhotoAuditResult.text = "Compressing & auditing ${uris.size} photos with Gemini Vision..."

        lifecycleScope.launch {
            val base64List = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    try {
                        contentResolver.openInputStream(uri)?.use { input ->
                            val bitmap = BitmapFactory.decodeStream(input)
                            val scaled = Bitmap.createScaledBitmap(bitmap, 640, 640 * bitmap.height / bitmap.width, true)
                            val baos = ByteArrayOutputStream()
                            scaled.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            val report = PhotoAuditor.auditPhotos(apiKey, base64List)
            if (report != null) {
                val sb = StringBuilder("📸 6-Metric Audit Report:\n\n")
                sb.append("Summary: ").append(report.overallSummary).append("\n\n")
                sb.append("Optimal Upload Sequence: ")
                sb.append(report.optimalSequence.map { "#${it + 1}" }.joinToString(" ➔ ")).append("\n\n")

                for (score in report.photoScores) {
                    sb.append("Photo #${score.photoIndex + 1} (Score: ${score.overallScore}/10)\n")
                    sb.append("• Lighting: ${score.lightingScore}/10 | Smile: ${score.smileAndEyeContact}/10\n")
                    if (score.redFlags.isNotEmpty()) {
                        sb.append("• Red Flags: ${score.redFlags.joinToString(", ")}\n")
                    }
                    sb.append("• Advice: ${score.recommendation}\n\n")
                }
                tvPhotoAuditResult.text = sb.toString()
            } else {
                tvPhotoAuditResult.text = "Audit failed. Check network or Gemini API key."
            }
        }
    }

    private fun runProfileQuiz() {
        val apiKey = etApiKey.text.toString().trim()
        if (apiKey.isBlank()) {
            Toast.makeText(this, "Enter Gemini API Key first!", Toast.LENGTH_SHORT).show()
            return
        }

        tvProfileResult.visibility = View.VISIBLE
        tvProfileResult.text = "Generating Hinge prompt packs & Bumble bios..."

        val quiz = ProfileQuizData(
            passions = etPassions.text.toString().trim(),
            weekendVibe = etWeekend.text.toString().trim(),
            guiltyPleasure = "Late night takeout",
            petPeeve = "People walking too slow",
            humorStyle = etHumor.text.toString().trim().ifBlank { "Playfully witty" },
            city = etCityAndSpots.text.toString().trim().ifBlank { "City Center" }
        )

        lifecycleScope.launch {
            val result = ProfileGenerator.generateProfilePacks(apiKey, quiz)
            if (result != null) {
                val sb = StringBuilder("✨ Generated Profile Packs:\n\n")
                sb.append("🐝 BUMBLE BIOS (Pick your favorite):\n")
                result.bumbleBios.forEachIndexed { i, bio ->
                    sb.append("${i + 1}. \"$bio\"\n\n")
                }

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
                tvProfileResult.text = "Generation failed. Verify API Key."
            }
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("wingman_prefs", Context.MODE_PRIVATE)
        etApiKey.setText(prefs.getString("gemini_api_key", ""))
        etCityAndSpots.setText(prefs.getString("user_spots", ""))
        etPassions.setText(prefs.getString("user_passions", ""))
        etWeekend.setText(prefs.getString("user_weekend", ""))
        etHumor.setText(prefs.getString("user_tone", ""))
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("wingman_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("gemini_api_key", etApiKey.text.toString().trim())
            .putString("user_spots", etCityAndSpots.text.toString().trim())
            .putString("user_passions", etPassions.text.toString().trim())
            .putString("user_weekend", etWeekend.text.toString().trim())
            .putString("user_tone", etHumor.text.toString().trim())
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
            Toast.makeText(this, "Wingman Floating Bubble Started! Open Hinge or Bumble.", Toast.LENGTH_LONG).show()
        }
    }
}
