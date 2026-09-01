# 🌱 Stem

Ambient writing and inline text enhancement assistant for Android.

Stem enhances your text anywhere across Android—without popups, lock-in, or telemetry. Use instant on-device heuristics, your local Ollama instance over LAN, or BYO-key cloud LLMs.

---

## ✨ Highlights

- **Ambient & Seamless**: Trigger from the native Android text selection menu (`ACTION_PROCESS_TEXT`), inline trigger commands, or quick snippet expansions.
- **Privacy First**: 100% private. Keystrokes are never tracked or uploaded without your explicit configuration.
- **Multi-Engine Support**:
  - **On-Device (0ms)**: 40+ instant grammar, syntax, and tone heuristics.
  - **Local LAN (Ollama)**: Connect to local AI models (`llama3.2`, `gemma2`, `mistral`, `qwen2.5`) over Wi-Fi.
  - **Cloud APIs**: Direct BYO-key integration for Google Gemini, Anthropic Claude, and OpenAI-compatible endpoints.
- **Visual Diff**: Word-level diff viewer highlights changes before replacing text.
- **9 Core Presets**: Fix, Concise, Formal, Punchy, Friendly, Summarize, Bulletize, Expand, and Custom Prompts.

---

## 🚀 Getting Started

### Installation
Download the latest APK from [Releases](https://github.com/garc-kt/Stem/releases).

### Usage
1. **Selection Menu**: Highlight text in any app $\rightarrow$ tap **Stem** $\rightarrow$ choose a preset.
2. **Inline Triggers**: Configure custom prefixes (e.g., `;;fix <text>`) for instant in-place transformations.
3. **Snippets**: Set up expandable text shortcuts (e.g., `;email` $\rightarrow$ `user@example.com`).

---

## 🛠 Tech Stack

- **UI**: Jetpack Compose & Material 3
- **Navigation**: Navigation 3
- **Persistence**: DataStore Preferences & Encrypted Storage
- **Requirements**: Android 8.0+ (API 26+) · Target SDK 36

---

## 📄 License

Apache License 2.0. See [LICENSE](LICENSE) for details.
