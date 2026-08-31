# 🌱 Sprout — Ambient AI Writing Assistant for Android

<p align="center">
  <b>Instant, frictionless text enhancement directly inside any Android app.</b><br>
  <i>100% On-Device Rules + Local LAN AI via Ollama. Built with Jetpack Compose & Material 3 Expressive.</i>
</p>

---

## ✨ Features

- 🌿 **Floating Suggestion Pill**: 36dp floating pill attached near focused text inputs across Android with organic pulse animation.
- ⚡ **Dual Intelligence Engine**:
  - **On-Device Rules (Instant)**: 100% in-memory dictionary & grammar rules ($0\text{ ms}$ latency, zero network).
  - **Ollama Local AI (PC / LAN)**: Seamlessly connects over Wi-Fi to your PC running Ollama (`llama3.2`, `mistral`, `gemma2`, `qwen2.5`, etc.) with automatic fallback if the PC goes offline.
- 🎯 **4 Transformation Presets**:
  - **Fix & Polish**: Corrects spelling, grammar, punctuation, and casing.
  - **Concise**: Trims filler words and tightens sentences.
  - **Professional**: Formats text into courteous, articulate business prose.
  - **Punchy**: High-energy, active copy.
- 🔍 **Dynamic LCS Diff Viewer**: Word-by-word comparison highlighting additions (emerald green) and deletions (rose strikethrough).
- ⌨️ **Inline Trigger Commands**:
  - `?fix`, `?concise`, `?formal`, `?punchy` — Transform text inline without opening the overlay.
  - `?calc: 25 * 4 + 10` $\rightarrow$ `110` — Inline math evaluation.
  - `?now`, `?date` — Dynamic timestamps.
  - `?undo` — Revert last injection.
- 🚀 **Text Snippets & Expander**:
  - Type `..email` $\rightarrow$ `user@example.com`, `..shrug` $\rightarrow$ `¯\_(ツ)_/¯`.
  - Save on the fly: `..save:addr:123 Main Street`.
- 🛡️ **Zero-Cloud Privacy**: Zero analytics, zero external cloud servers, zero telemetry.
- 📱 **Wide Compatibility**: Fully certified and signed for **Android 10 (API 29) through Android 17 (API 37+)**.

---

## 🛠️ Architecture & Package Structure

Organized **By Function + By Type**:

```
com.veggiebit.sprout/
├── app/                                        # Application lifecycle & Theme
│   ├── SproutApplication.kt
│   └── theme/                                  # Material 3 Expressive light tokens
│
├── core/                                       # Cross-cutting foundational layer
│   ├── utils/                                  # AccessibilityUtils, HapticHelper, PermissionHelper
│   └── version/                                # Semantic Versioning runtime metadata
│
└── features/                                   # Feature modules
    ├── enhancement/                            # Data engines (Local, Ollama, LCS Diff, Undo)
    ├── overlay/                                # Floating WindowManager Compose overlay & tile
    ├── selection/                              # ACTION_PROCESS_TEXT native selection menu
    └── settings/                               # DataStore preferences, PC Ollama setup & Live Sandbox
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug / Meerkat (or newer)
- JDK 17+
- Android SDK 36 (minSdk 26, targetSdk 35)

### Build from Source
```bash
# Clone the repository
git clone https://github.com/Garc2004/Sprout.git
cd Sprout

# Run unit tests
./gradlew testDebugUnitTest

# Assemble signed release APK
./gradlew assembleRelease
```

### Install APK via ADB
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 💻 Connecting to Ollama on your PC

1. On your PC, launch Ollama with LAN access:
   ```bash
   OLLAMA_HOST=0.0.0.0 ollama serve
   ```
2. In Sprout's **Settings > AI Intelligence Engine**, select **Ollama Local AI (PC / LAN)**.
3. Enter your PC's IP address (e.g. `http://192.168.1.50:11434` or `http://10.0.2.2:11434` for emulator).
4. Tap **Test Connection** to discover installed models and select your preferred model.

---

## 📄 License
This project is open-source under the Apache 2.0 License.
