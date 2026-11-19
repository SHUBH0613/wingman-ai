# 🔥 Wingman AI: Autonomous Dating Copilot Suite for Android
### Built for Hinge, Bumble, Tinder & Instagram DMs | Powered by Google Gemini Vision & Flash

[![Build Wingman APK](https://github.com/your-username/wingman-ai/actions/workflows/build_apk.yml/badge.svg)](https://github.com/your-username/wingman-ai/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com)
[![Gemini](https://img.shields.io/badge/AI-Google%20Gemini%202.5%20Flash-blue.svg)](https://aistudio.google.com)

**Wingman AI** is a complete, ban-proof dating assistant designed specifically for Android. It eliminates swiping fatigue and writer's block by handling profile creation, photo selection, prompt engineering, and live chat steering all the way to in-person dates.

---

## 🌟 Architecture & Features

```
┌───────────────────────────────────────────────────────────────────────────────────┐
│                               WINGMAN AI ANDROID SUITE                            │
├──────────────────────────┬─────────────────────────────┬──────────────────────────┤
│ 1. PROFILE DOCTOR        │ 2. LIVE CHAT COPILOT        │ 3. CONVERSION ENGINE     │
│ • 6-Metric Photo Auditor │ • Floating Bubble (Overlay) │ • 4-Stage Date Playbook  │
│ • 6-Slot Order Optimizer │ • Screen Accessibility Read │ • Anti-AI Casual Styling │
│ • 60-Sec Hinge Packs     │ • Custom AI Keyboard (IME)  │ • Local Date Spot Logic  │
│ • 280-char Bumble Bios   │ • 1-Tap Direct Text Insert  │ • High Conversation Bait │
└──────────────────────────┴─────────────────────────────┴──────────────────────────┘
```

### 1. 📸 6-Metric Photo Auditor & Sequence Optimizer
Upload up to 10 photos from your gallery. Gemini Vision analyzes each photo across 6 objective dimensions:
* **First Impression & Attractiveness (1–10)**
* **Lighting & Sharpness (1–10)**
* **Eye Contact & Genuine Smile (1–10)**
* **Social Proof & Lifestyle Signals (1–10)**
* **Red Flag Detection** (sunglasses, gym selfies, hats, group confusion)
* **Optimal 6-Photo Sequence:** Ranks your photos into the ideal psychological order:
  $$\text{Slot 1: Headshot} \longrightarrow \text{Slot 2: Full Body} \longrightarrow \text{Slot 3: Activity/Hobby} \longrightarrow \text{Slot 4: Social} \longrightarrow \text{Slot 5: Candid/Travel} \longrightarrow \text{Slot 6: Humor}$$

### 2. ✍️ 60-Second Bio & Hinge Prompt Crafter
Answer 5 quick lifestyle questions (passions, weekend routine, guilty pleasure, pet peeve, humor style):
* Generates **3 complete Hinge prompt triplets** (e.g., *"The way to win me over is..."*, *"Together, we could..."*) engineered with conversation bait.
* Generates **3 Bumble bios** under 280 characters with built-in hooks that encourage matches to message first.

### 3. 🔥 Floating Overlay Bubble (Screen-Reading Copilot)
* Floats discreetly over **Hinge, Bumble, and Instagram DMs** like a Messenger ChatHead.
* **Dual-Capture Technology:** Reads the match's profile or chat text directly from the screen via Android's `AccessibilityService` (with an instant clipboard fallback).
* Tapping the flame opens a 1-click drawer with 3 tailored replies. Tap any reply to copy and minimize.

### 4. ⌨️ Wingman AI Keyboard (IME)
* A custom keyboard that works in any text box.
* Features a persistent AI Action Bar: `[✨ Opener]`, `[🌶️ Tease]`, `[🎯 Date]`.
* **Direct Text Insertion:** Press **Insert into Chat** to type the generated response straight into Hinge/Bumble without copy-pasting.

### 5. 🎯 The 4-Stage Date Closer Playbook
Conversations are guided through an intentional psychological state machine:
* **Stage 1 (Hook Opener):** Observations on specific photo/bio details; strictly bans generic "Hey".
* **Stage 2 (Banter & Tease):** Playful push-pull, matching message length.
* **Stage 3 (Common Ground):** Bridging to mutual interests, food, drinks, and weekend routines.
* **Stage 4 (Date Closer):** Proposes low-pressure drinks/coffee in your city on Thursday/Friday, followed by an IG/phone number bridge.

---

## 🚀 How to Build & Download Your APK

### Option A: Cloud Build with GitHub Actions (Zero Local Tools Needed)
1. Fork or push this repository to GitHub:
   ```bash
   git init
   git add .
   git commit -m "Initial Wingman AI commit"
   git remote add origin https://github.com/<your-username>/wingman-ai.git
   git push -u origin main
   ```
2. Navigate to the **Actions** tab on your GitHub repository.
3. The included workflow (`.github/workflows/build_apk.yml`) compiles the project on a GitHub runner.
4. Once completed, click the run and download **`wingman-ai-debug.apk`** under **Artifacts** straight to your Android phone!

### Option B: Local Build via Android Studio
1. Open **Android Studio** and choose **Open**.
2. Select the `wingman-android` folder:
   `C:\Users\shubh\.gemini\antigravity\scratch\wingman-android`
3. Click **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**.
4. The `.apk` will be output to:
   `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📱 Quickstart on Your Phone

1. Install the APK and open **Wingman AI**.
2. Enter your **Google Gemini API Key** (Free from [Google AI Studio](https://aistudio.google.com/)).
3. Enter your city and 2–3 favorite date spots (e.g. *"Downtown, rooftop cocktails, artisan coffee"*).
4. Run the **6-Metric Photo Auditor** on your pictures to optimize your photo order.
5. Tap **Generate Hinge & Bumble Packs** to copy your new bios and prompts.
6. Tap **Start Floating Overlay Bubble** and enable:
   * **Display over other apps**
   * **Accessibility Service** (*Wingman Dating Assistant*)
   * *(Optional)* **Wingman AI Keyboard**
7. Open Hinge or Bumble and let the AI do the heavy lifting!
