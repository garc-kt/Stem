<p align="center">
  <img src="art/banner.png" alt="Sprout Banner" width="100%" style="border-radius: 16px; max-width: 900px;" />
</p>

<h1 align="center">🌿 Sprout</h1>

<p align="center">
  <strong>Ambient AI & Rule-Based Writing Assistant for Android</strong><br>
  <em>100% On-Device Privacy + Local PC Ollama AI & Cloud LLMs (Gemini, Claude, OpenAI)</em>
</p>

<p align="center">
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Target%20SDK-36-2E7D32.svg?style=for-the-badge&logo=android&logoColor=white" alt="Target SDK 36" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack_Compose-2026.03.01-1A73E8.svg?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" /></a>
  <a href="https://ollama.com"><img src="https://img.shields.io/badge/Local_AI-Ollama_LAN-000000.svg?style=for-the-badge&logo=ollama&logoColor=white" alt="Ollama Local AI" /></a>
  <a href="https://github.com/Garc2004/Sprout/releases"><img src="https://img.shields.io/badge/Release-v1.2.0-00838F.svg?style=for-the-badge&logo=github&logoColor=white" alt="Release v1.2.0" /></a>
</p>

---

**Sprout** (`com.veggiebit.sprout`) is an ambient, frictionless text enhancement engine that lives right inside your existing Android keyboard and text selection workflow. It operates silently with zero popups via the **Native Android Text Selection Menu** (`ACTION_PROCESS_TEXT`), **Inline Trigger Commands**, or an optional floating pill.

---

## 📦 Direct APK Downloads

<p align="center">
  <a href="https://github.com/Garc2004/Sprout/releases/download/v1.2.0/app-release.apk">
    <img src="https://img.shields.io/badge/Download-Release_APK_(v1.2.0)-2E7D32?style=for-the-badge&logo=android&logoColor=white" alt="Download Release APK" />
  </a>
</p>

---

## ✨ Features

- 🌿 **SwiftSlate Unobtrusive Mode**: Runs quietly in the background without obstructing apps. Enhance text seamlessly via the **Native Android Text Selection Menu** (`ACTION_PROCESS_TEXT`) or **Instant Inline Triggers**. An optional floating pill can be toggled on demand.
- 🤖 **Multi-AI Intelligence Engine**:
  - **On-Device Rules (Instant)**: 100% in-memory dictionary & grammar heuristics ($0\text{ ms}$ latency, zero network, 100% private).
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

---

## 🤖 Supported AI Providers Matrix

| Provider | Description | Latency | Privacy |
| :--- | :--- | :--- | :--- |
| **🌿 On-Device Rules** | Zero latency ($0\text{ ms}$), 100% offline, dictionary based | $\approx 0\text{ ms}$ | 100% On-Device |
| **💻 PC Ollama LAN** | Self-hosted local models over Wi-Fi (`http://192.168.1.X:11434`) | $\approx 200\text{--}600\text{ ms}$ | 100% Private LAN |
| **✨ Google Gemini** | Gemini 2.0 Flash / 1.5 Flash | $\approx 300\text{--}800\text{ ms}$ | Direct API Key |
| **⚡ OpenAI / DeepSeek** | GPT-4o-mini, DeepSeek-V3, Groq | $\approx 300\text{--}900\text{ ms}$ | Direct API Key |
| **🧠 Anthropic Claude** | Claude 3.5 Sonnet / Haiku | $\approx 400\text{--}900\text{ ms}$ | Direct API Key |

---

## 🏗️ Architecture & Structure

Organized **By Function & By Type**:

```
com.veggiebit.sprout/
├── app/                                        # Application root & Theme
│   ├── SproutApplication.kt
│   └── theme/                                  # Material 3 Monet dynamic color tokens
│
├── core/                                       # Cross-cutting foundational layer
│   ├── utils/                                  # AccessibilityUtils, HapticHelper, PermissionHelper
│   └── version/                                # Semantic Versioning runtime metadata
│
└── features/                                   # Feature modules
    ├── enhancement/                            # Data engines (Local, Gemini, OpenAI, Claude, Ollama, Diff)
    │   ├── data/api/                           # GeminiClient, OpenAIClient, ClaudeClient
    │   └── data/engine/                        # Rule engines with offline fallback & provider
    ├── overlay/                                # Floating WindowManager Compose overlay & tile
    ├── selection/                              # ACTION_PROCESS_TEXT Google-style bottom sheet dialog
    └── settings/                               # DataStore preferences, AI providers setup & Live Sandbox
```

---

## 🏢 VeggieBit Studios Ecosystem

Sprout is engineered alongside **[RadishTop](https://github.com/Garc2004/RadishTop)** under **VeggieBit Studios**:

- **🌱 [RadishTop](https://github.com/Garc2004/RadishTop)**: Adaptive Hardware Notch & Dynamic Cutout Morphing Overlay for Android.
- **🌿 [Sprout](https://github.com/Garc2004/Sprout)**: Ambient AI & Rule-Based Writing Assistant for Android.

---

## 📄 License

```
Copyright (c) 2026 VeggieBit Studios
Licensed under the Apache License, Version 2.0
```
