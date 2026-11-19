package com.wingman.ai

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WingmanKeyboardService : InputMethodService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var currentSuggestion: String = ""

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.layout_keyboard, null)

        val btnOpener = view.findViewById<Button>(R.id.kbBtnOpener)
        val btnTease = view.findViewById<Button>(R.id.kbBtnTease)
        val btnDate = view.findViewById<Button>(R.id.kbBtnDate)
        val btnCasual = view.findViewById<Button>(R.id.kbBtnCasual)
        val btnSwitch = view.findViewById<Button>(R.id.kbBtnSwitchIme)
        val tvSuggestion = view.findViewById<TextView>(R.id.kbTvSuggestion)
        val btnInsert = view.findViewById<Button>(R.id.kbBtnInsert)

        fun fetchAi(stage: DatingStage, flavor: HumorFlavor = HumorFlavor.WITTY_TEASE) {
            val prefs = getSharedPreferences("wingman_prefs", Context.MODE_PRIVATE)
            val apiKey = prefs.getString("gemini_api_key", "") ?: ""

            val profile = FullUserProfile.fromPreferences(prefs)

            // Extract context from preceding text or clipboard
            val textBefore = currentInputConnection?.getTextBeforeCursor(120, 0)?.toString() ?: ""
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clipText = if (clipboard.hasPrimaryClip() && clipboard.primaryClip != null && clipboard.primaryClip!!.itemCount > 0) {
                clipboard.primaryClip!!.getItemAt(0)?.text?.toString() ?: ""
            } else {
                ""
            }

            val contextText = if (textBefore.isNotBlank()) textBefore else clipText

            tvSuggestion.text = "Generating response..."
            btnInsert.visibility = View.GONE

            serviceScope.launch {
                val bundle = GeminiDatingEngine.generateReplies(
                    apiKey = apiKey,
                    contextText = contextText,
                    stage = stage,
                    profile = profile,
                    language = LanguageMode.ENGLISH,
                    flavor = flavor
                )
                val replies = bundle.options
                if (replies.isNotEmpty()) {
                    currentSuggestion = replies[0]
                    tvSuggestion.text = "💡 $currentSuggestion"
                    btnInsert.visibility = View.VISIBLE
                }
            }
        }

        btnOpener.setOnClickListener { fetchAi(DatingStage.STAGE_1_HOOK_OPENER) }
        btnTease.setOnClickListener { fetchAi(DatingStage.STAGE_2_BANTER_TEASE) }
        btnDate.setOnClickListener { fetchAi(DatingStage.STAGE_4_DATE_CLOSER) }
        btnCasual.setOnClickListener { fetchAi(DatingStage.STAGE_2_BANTER_TEASE, HumorFlavor.DRY_SARCASM) }

        btnInsert.setOnClickListener {
            if (currentSuggestion.isNotBlank()) {
                currentInputConnection?.commitText(currentSuggestion, 1)
                tvSuggestion.text = "Inserted! Tap another AI action when ready."
                btnInsert.visibility = View.GONE
            }
        }

        btnSwitch.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchToNextInputMethod(false)
            } else {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
        }

        return view
    }
}
