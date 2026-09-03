<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="assets/icon-dark.png">
    <source media="(prefers-color-scheme: light)" srcset="assets/icon-light.png">
    <img alt="Stem Logo" src="assets/icon-light.png" width="128">
  </picture>
</p>

<h1 align="center">Stem</h1>

<p align="center">
  <strong>Ambient writing and inline text enhancement assistant for Android.</strong><br>
  <em>Instant on-device heuristics, local Ollama over Wi-Fi, and cloud LLMs—without popups, lock-in, or telemetry.</em>
</p>

<p align="center">
  <a href="https://github.com/garc-kt/Stem/releases"><img src="https://img.shields.io/github/v/release/garc-kt/Stem?style=for-the-badge&color=2E7D32&logo=android&logoColor=white" alt="Latest Release" /></a>
  <a href="https://github.com/sponsors/garc-kt"><img src="https://img.shields.io/badge/Sponsor-GitHub%20Sponsors-EA4AAA?style=for-the-badge&logo=githubsponsors&logoColor=white" alt="GitHub Sponsors" /></a>
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Android-8.0+_to_16-1A73E8?style=for-the-badge&logo=android&logoColor=white" alt="Android 8.0+" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin 2.3" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg?style=for-the-badge" alt="Apache 2.0" /></a>
</p>

<p align="center">
  <a href="https://github.com/garc-kt/Stem/releases/latest"><b>⬇️ Download APK</b></a> •
  <a href="#-why-stem">Why Stem?</a> •
  <a href="#-how-it-works">How It Works</a> •
  <a href="#-presets--transformations">Presets</a> •
  <a href="#-architecture">Architecture</a> •
  <a href="#-permissions--security">Security</a> •
  <a href="#-building-from-source">Build</a> •
  <a href="SPONSORS.md">Sponsor</a>
</p>

---

## 💡 Why Stem?

Most grammar and AI writing tools require you to swap your primary keyboard for a proprietary one, surrender your keystrokes to remote servers, or pay subscription fees. 

**Stem takes a fundamentally different approach:**

| Capability | Traditional Writing Apps & Keyboards | Stem |
|---|---|---|
| **Keyboard Freedom** | Replaces your keyboard with a proprietary IME | Works ambiently with **any** keyboard (Gboard, SwiftKey, FlorisBoard) |
| **Data Privacy** | Streams keystrokes & clipboard to third-party clouds | **100% Local First**. Zero trackers, zero analytics, zero telemetry |
| **Execution Latency** | 500ms – 2500ms network roundtrips | **0ms instant** on-device heuristic engine |
| **AI Freedom** | Locked to a single cloud provider | **Hybrid**: On-device rules, Local Ollama (LAN Wi-Fi), or BYO-key Cloud |
| **Verification** | Blindly auto-replaces text | **Visual Diff**: Inspect token-level additions and deletions before accepting |
| **Cost & Freedom** | Monthly subscription paywalls | **Free & Open Source** under Apache 2.0 |

---

## 🎬 How It Works

Stem integrates directly into the Android framework without disrupting your typing flow:

### 1. Context Menu Action (`ACTION_PROCESS_TEXT`)
Select any text block in any application (WhatsApp, Gmail, Chrome, Slack, Notes). Tap the three-dot overflow menu and select **Stem** to review suggested improvements in an adaptive bottom sheet.

### 2. Ambient Inline Triggers (`;;shortcut`)
Type shorthand triggers directly inside any editable text box—such as `;;fix`, `;;formal`, or `;;concise`. Stem's lightweight accessibility monitor detects the command, applies the transformation, and injects the improved text in real time.

### 3. Visual Token Diffing
Never guess what an AI model changed. Stem computes a token-level Longest Common Subsequence (LCS) diff, highlighting inserted words in green and removed words in red before you confirm.

---

## ✍️ Presets & Transformations

Stem includes carefully tuned writing transforms designed for daily communication:

| Preset | Shorthand | Typical Use Case | Example Transformation |
|---|---|---|---|
| **Fix** | `;;fix` | Clean typos, punctuation & grammar | *"i went to store yestarday"* ➔ *"I went to the store yesterday."* |
| **Concise** | `;;concise` | Eliminate wordiness and fluff | *"Due to the fact that we are ready"* ➔ *"Because we are ready"* |
| **Formal** | `;;formal` | Workplace emails & client messages | *"hey whats up can u send that"* ➔ *"Hello, could you please provide that?"* |
| **Punchy** | `;;punchy` | Social media, copy & pitch text | *"Our software helps people do things faster"* ➔ *"Ship faster with zero friction."* |
| **Friendly** | `;;friendly` | Soften direct or blunt text | *"Send the file now"* ➔ *"Whenever you have a moment, could you share the file?"* |
| **Bulletize** | `;;bullets` | Convert dense text to summary points | Paragraphs ➔ Clean, actionable bullet list |
| **Custom** | `;;custom` | User-defined system prompts | Tailored to your specific domain or translation tasks |

---

## 🏛️ Architecture

Stem is engineered following modern Android architecture standards with clear separation of concerns:

```
┌──────────────────────────────────────────────────────────────┐
│                       ANDROID OS LAYER                       │
│    Native Context Menu (ACTION_PROCESS_TEXT)  │  Inline (;;)  │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                         STEM CORE                            │
│  ┌────────────────────────┐      ┌────────────────────────┐  │
│  │  0ms Local Heuristics  │      │   Local Ollama (LAN)   │  │
│  │  (40+ Grammar / Rules) │      │   (Zero Internet Req.) │  │
│  └────────────────────────┘      └────────────────────────┘  │
│  ┌────────────────────────────────────────────────────────┐  │
│  │   Cloud LLM Connectors (BYO-Key: Gemini / Claude / GPT)│  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                    VISUAL DIFF PREVIEW                       │
│         Token-level LCS Diff Sheet & Tactile Feedback        │
└──────────────────────────────────────────────────────────────┘
```

- **Kotlin Coroutines & Flow**: Reactive, non-blocking asynchronous state pipelines.
- **Jetpack Compose & Material 3**: Fluid transitions, Monet dynamic theming, and responsive bottom sheets.
- **AndroidX DataStore**: Encrypted local persistence for preferences and optional API credentials.
- **Strict Separation**: Network connectors are fully decoupled from core text processing.

---

## 🔒 Permissions & Security

We believe tools should be transparent about what permissions they request and why:

| Permission | Necessity | Purpose |
|---|---|---|
| `BIND_ACCESSIBILITY_SERVICE` | *Optional* | Enables inline `;;` triggers and direct text injection. Text is analyzed strictly in transient memory on-device and is never logged or saved. |
| `INTERNET` | *Conditional* | Required **only** when querying a local Ollama server over your Wi-Fi network or external cloud LLM providers. On-device heuristic rules operate with zero network access. |
| `VIBRATE` | *Optional* | Provides gentle haptic confirmation when text is replaced. |

> [!NOTE]
> Stem contains **zero third-party tracking libraries**, crash reporters, or advertising SDKs. Your writing remains exclusively on your device.

---

## 🚀 Building from Source

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or newer
- JDK 17
- Android SDK Platform 36 (minSdk 26)

### Build Commands

```bash
# Clone repository
git clone https://github.com/garc-kt/Stem.git
cd Stem

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew testDebugUnitTest
```

Compiled APKs will be located in:  
`app/build/outputs/apk/release/Stem-v1.0.3.apk`

---

## ☕ Support & Sponsorship

Stem is independently crafted and maintained under **VeggieBit Studios**. If Stem saves you time and enhances your Android experience, please consider supporting ongoing development:

<p align="center">
  <a href="https://github.com/sponsors/garc-kt" target="_blank">
    <img src="https://img.shields.io/badge/Sponsor%20on%20GitHub-EA4AAA?style=for-the-badge&logo=githubsponsors&logoColor=white" height="42" alt="Sponsor on GitHub" />
  </a>
  &nbsp;&nbsp;&nbsp;&nbsp;
  <a href="https://ko-fi.com/X5R825DY4X" target="_blank">
    <img src="https://storage.ko-fi.com/cdn/kofi2.png?v=3" height="42" style="border:0px;height:42px;" alt="Buy Me a Coffee at ko-fi.com" />
  </a>
</p>

Learn more about sponsor tiers, perks, and how funds are used in [**SPONSORS.md**](SPONSORS.md).

---

## 📄 License

Distributed under the **Apache License 2.0**. See [`LICENSE`](LICENSE) for terms.


