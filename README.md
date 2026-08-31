<p align="center">
  <img src="art/banner.png" alt="Sprout Banner" width="700" style="border-radius: 16px;" />
</p>

# 🌱 Sprout — Ambient AI Writing Assistant for Android

<p align="center">
  <b>Instant, frictionless text enhancement directly inside any Android app.</b><br>
  <i>100% On-Device Rules + Google Gemini, OpenAI, Claude, DeepSeek & Ollama LAN AI. Google Material You (Monet) Design.</i>
</p>

---

## ✨ Features

- 🌿 **SwiftSlate Unobtrusive Mode**: Runs quietly in the background without noisy popups over other apps. Enhance text seamlessly via the **Native Android Text Selection Menu** (`ACTION_PROCESS_TEXT`) or **Instant Inline Triggers**. An optional floating pill can be enabled whenever desired.
- 🤖 **Multi-AI Intelligence Engine**:
  - **On-Device Rules (Instant)**: 100% in-memory dictionary & grammar rules ($0\text{ ms}$ latency, zero network, zero cloud).
  - **Google Gemini API**: Native Google Gemini 2.0 Flash / 1.5 Flash integration with structured responses and system prompts.
  - **OpenAI / OpenRouter / DeepSeek**: Compatible with standard OpenAI API endpoints (OpenAI GPT-4o, DeepSeek-V3, Groq, OpenRouter).
  - **Anthropic Claude API**: Claude 3.5 Sonnet / Haiku integration.
  - **Ollama Local AI (PC / LAN)**: Connects over local Wi-Fi to your PC running Ollama (`llama3.2`, `mistral`, `gemma2`, `qwen2.5`) with automatic fallback.
- 🎨 **Google Material You & Monet Palette**:
  - Dynamic Wallpaper Color Extraction on Android 12+ (API 31+).
  - Signature Google Pixel Material 3 palette and tonal surface containers on Android 10-11.
  - Assistant-style Bottom Sheet with diff preview and one-tap replacement.
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
- 🛡️ **Zero-Cloud Telemetry & Privacy**: Zero analytics, zero tracking, your API keys remain local and encrypted in DataStore.
- 📱 **Wide Compatibility**: Fully certified and signed for **Android 10 (API 29) through Android 17 (API 37+)**.

---

## 🛠️ Architecture & Package Structure

Organized **By Function + By Type**:

```
com.veggiebit.sprout/
├── app/                                        # Application lifecycle & Theme
│   ├── SproutApplication.kt
│   └── theme/                                  # Material 3 Monet dynamic color tokens
│
├── core/                                       # Cross-cutting foundational layer
│   ├── utils/                                  # AccessibilityUtils, HapticHelper, PermissionHelper
│   └── version/                                # Semantic Versioning runtime metadata
│
└── features/                                   # Feature modules
    ├── enhancement/                            # Data engines (Local, Gemini, OpenAI, Claude, Ollama, Diff, Undo)
    │   ├── data/api/                           # GeminiClient, OpenAIClient, ClaudeClient
    │   └── data/engine/                        # Rule engines with offline fallback & provider
    ├── overlay/                                # Floating WindowManager Compose overlay & tile
    ├── selection/                              # ACTION_PROCESS_TEXT Google-style bottom sheet dialog
    └── settings/                               # DataStore preferences, AI providers setup & Live Sandbox
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

## 🤖 Supported AI Providers

Sprout allows configuring any of the following providers in **Settings > AI Intelligence Engine**:

| Provider | Description | Setup |
| :--- | :--- | :--- |
| **🌿 On-Device Rules** | Zero latency ($0\text{ ms}$), 100% offline, zero network | Default (No setup required) |
| **✨ Google Gemini** | Gemini 2.0 Flash / 1.5 Flash | Enter Gemini API Key |
| **⚡ OpenAI / DeepSeek** | GPT-4o-mini, DeepSeek-V3, Groq | Enter Base URL & API Key |
| **🧠 Anthropic Claude** | Claude 3.5 Sonnet / Haiku | Enter Claude API Key |
| **💻 PC Ollama LAN** | Self-hosted local models | Enter PC IP `http://192.168.1.X:11434` |

---

## 📄 License
This project is open-source under the Apache 2.0 License. Engineered with ❤️ by **VeggieBit Studios**.
