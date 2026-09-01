# 🌱 Stem

**Ambient Writing & Inline Text Enhancement Assistant for Android**  
*100% On-Device Privacy + Local PC Ollama AI & Cloud LLMs (Gemini, Claude, OpenAI)*

[![Target SDK 36](https://img.shields.io/badge/Target%20SDK-36-2E7D32.svg?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-2026.03.01-1A73E8.svg?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Local AI Ollama](https://img.shields.io/badge/Local_AI-Ollama_LAN-000000.svg?style=for-the-badge&logo=ollama&logoColor=white)](https://ollama.com)
[![Release v1.0.0](https://img.shields.io/badge/Release-v1.0.0-00838F.svg?style=for-the-badge&logo=github&logoColor=white)](https://github.com/Garc2004/Stem/releases)

---

**Stem** is an ambient, frictionless writing and text enhancement assistant designed with a warm-stone neutral aesthetic, sharp geometry, and zero cloud lock-in. It operates silently with zero popups via the **Native Android Text Selection Menu** (ACTION_PROCESS_TEXT), **Custom Inline Trigger Commands**, **Text Expansion Snippets**, or an optional floating helper.

---

## ✨ Features

- **Warm Stone Neutral Palette & Sharp Geometry**: Custom tokens with 2–4dp radius controls, 10dp bottom sheets/overlays, and reserved diff accent pairing (emerald add / terracotta remove).
- **Multi-AI Intelligence Engine**:
  - **On-Device Rules (Instant)**: 40+ in-memory grammar & tone heuristics (0 ms latency, 100% private, zero network).
  - **Ollama Local AI (PC / LAN)**: Connects over local Wi-Fi to your PC running Ollama (llama3.2, mistral, gemma2, qwen2.5) with automatic offline fallback.
  - **Google Gemini API**: Native Google Gemini 2.0 / 1.5 Flash integration.
  - **Anthropic Claude API**: Claude 3.5 Sonnet / Haiku integration.
  - **OpenAI-Compatible**: Compatible with standard OpenAI API endpoints (GPT-4o, DeepSeek-V3, Groq, Together).
- **9 Transformation Presets**:
  - **Fix**: Corrects spelling, grammar, punctuation, and casing.
  - **Concise**: Trims filler words and tightens sentences.
  - **Formal**: Polite, business-ready formatting.
  - **Punchy**: High-energy, active copy.
  - **Friendly**: Warm, conversational tone.
  - **Summarize**: Distills text down to core insights.
  - **Bulletize**: Formats messy thoughts into scannable bullets.
  - **Expand**: Elaborates and fleshes out shorthand notes.
  - **Custom**: User-defined instructions on the fly.
- **Dynamic LCS Diff Viewer**: Word-by-word visual comparison with line-through deletions and underlined additions.
- **Inline Text Expansion Snippets**:
  - Type ;email $\rightarrow$ user@example.com, ;addr $\rightarrow$ full address.
- **Privacy Guarantee**: Keystrokes are never logged or stored off-device.

---

## 🏗 Architecture & Versioning

- **UI Layer**: 100% Jetpack Compose with custom LocalStemColors tokens and Manrope + Space Mono typography.
- **Versioning Strategy**: Structured Decimal Mask / CI Build Counter:
  \text{versionCode} = (\text{MAJOR} \times 1\,000\,000) + (\text{MINOR} \times 10\,000) + (\text{PATCH} \times 100) + (\text{BUILD} \bmod 100)
  - Strictly monotonic, deterministic, and CI-compatible.

---

## 📄 License

Apache License 2.0 — see [LICENSE](LICENSE) for details.
