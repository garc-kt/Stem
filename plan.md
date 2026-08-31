# Sprout (`com.veggiebit.sprout`) — Project Plan

---

## 1. Project Overview

* **Application Name:** Sprout
* **Package ID:** `com.veggiebit.sprout`
* **Category:** System Utility / Text Enhancement
* **Tech Stack:** Kotlin, Jetpack Compose, Android Accessibility APIs, WindowManager
* **Design Philosophy:** Material 3, Monet Dynamic Color (Light + Dark), 8dp spatial grid
* **Core Purpose:** Real-time, floating inline overlay that detects active text fields, generates instant suggestions (Fix, Concise, Professional, Punchy, Friendly, Summarize, Bulletize, Expand, Custom), and injects replacements directly into the target app without switching context.

> **v1.2.0 note:** the original design target was Material 3 *Expressive* (`MaterialExpressiveTheme`, `ButtonGroup`, `ToggleButton`, `HorizontalFloatingToolbar`, `LoadingIndicator`, `MaterialShapes`, the `*Increased` shape roles). In this project's resolved `androidx.compose.material3:material3:1.4.0` (pinned by the `androidx.compose:compose-bom:2026.03.01` BOM), that entire Expressive surface — including the `ExperimentalMaterial3ExpressiveApi` opt-in annotation itself — compiles as `internal`, not accessible from app code; this was confirmed with a clean, cache-disabled rebuild, not a transient tooling issue. The UI below is built on stable Material 3 instead (`FilterChip` for selection, plain `TopAppBar`/`MaterialTheme`, a `Row`-based action bar, `CircularProgressIndicator`), with Sprout's own shape/color tokens carrying the expressive intent. Revisit `MaterialExpressiveTheme` once the BOM moves to a `material3` release where it's public.

---

## 2. Technical Architecture

### 2.1 Core Modules & Services

This illustrative tree predates the actual `features/{enhancement,overlay,selection,settings}` package split — see `ARCHITECTURE_AND_PROCESS.md` §3 for the authoritative, current file tree (kept in sync as of v1.2.0: `TransformCache`/`TransformHistory`, the `engine/rules/` language layer, `core/crypto/CryptoBox`, and the `settings/ui/{sections,onboarding,components}` Navigation 3 rebuild).

### 2.2 System Service Hooks

* **`AccessibilityService` (`SproutAccessibilityService`)**
* Listens to `TYPE_VIEW_TEXT_CHANGED`, `TYPE_VIEW_FOCUSED`, and `TYPE_VIEW_CLICKED`.
* Extracts text via `AccessibilityNodeInfoCompat`.
* Injects transformed text back into target nodes via `ACTION_SET_TEXT` or clipboard fallback.
* Calculates screen coordinates using `nodeInfo.getBoundsInScreen(rect)` to anchor the floating pill near the cursor or keyboard top bar.


* **`WindowManager` (`SproutOverlayManager`)**
* Injects a Jetpack Compose `ComposeView` using `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
* Flags: `FLAG_NOT_FOCUSABLE` (allows typing underneath) and dynamically toggles `FLAG_ALT_FOCUSABLE_IM` when modal expanded.


* **Text Processing Pipeline (`TextEngine`)**
* **Phase 1 (MVP):** Lightweight on-device grammar/regex rules + compact local N-gram/transformer runtime via ONNX Runtime / MediaPipe LLM Inference.
* **Modes:**
* `Fix & Polish`: Grammar, typo correction, casing, punctuation.
* `Concise`: Redundancy removal and compression.
* `Professional`: Formal vocabulary and tone adjustments.
* `Punchy`: Conversational, high-impact phrasing.





---

## 3. UI & Design System Specifications

### 3.1 Material 3 Expressive Light Tokens

* **Theme:** Light + Dark, user-selectable (System / Light / Dark — `ThemeMode` in `SproutUserSettings`). `dynamicLightColorScheme()`/`dynamicDarkColorScheme()` on Android 12+; below that, a hand-authored Citrus/Amber (`#8A4B00` light primary / `#FFB876` dark primary) + Sprout Green static scheme — see `Color.kt`. Superseded the original "strictly light mode" constraint per an explicit product decision.
* **Surfaces:**
* Background / Base: `surfaceContainerLow`
* Overlay Capsule: `surfaceContainer` with `1.dp` border (`outlineVariant`)
* Active Selection: `primaryContainer` / `onPrimaryContainer`
* Action Buttons: `primary` / `onPrimary`


* **Typography:**
* Headers: `Roboto` / system sans (`HeadlineSmall`, `TitleMedium`)
* Body/Labels: `Roboto` / system sans (`BodyMedium`, `LabelLarge`)
* Counters / Metadata: `Roboto Mono` (`LabelSmall`)
* Google Sans / Google Sans Text aren't publicly distributable, and the Google-Fonts-downloadable-provider path needs Google Play Services plus provider certificate hashes that couldn't be verified without a device — Roboto is what most Android devices already render as the system sans regardless, so the visual delta is small.



### 3.2 Overlay Geometry & States

* **Collapsed State (Pill):**
* Size: `36dp` height, floating pill (`RoundedCornerShape(18.dp)`).
* Appears alongside active typing cursor or docked above the IME.


* **Expanded State (Control Capsule):**
* Width: Match parent with `16dp` horizontal margins.
* Corners: `RoundedCornerShape(28.dp)`.
* Contains preset selector chips (`32dp` height), diff preview, and one-tap **Replace Inline** button.



---

## 4. Permissions & Privacy Model

### 4.1 Manifest Declarations

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<service
    android:name=".service.SproutAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>

```

### 4.2 Accessibility Configuration (`accessibility_service_config.xml`)

```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeViewTextChanged|typeViewFocused"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagIncludeNotImportantViews|flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100" />

```

### 4.3 Zero-Telemetry Policy

* No first-party analytics, crash reporting, or usage tracking of any kind.
* `INTERNET`/`ACCESS_NETWORK_STATE` are declared and used, but only for the cloud AI engine
  modes (Gemini/OpenAI-compatible/Claude) the user explicitly opts into in Settings — the
  default `LOCAL_RULES` engine and Ollama (LAN-only) never leave the device. When a cloud
  engine is selected, the text being transformed is sent directly to that provider's API;
  this is inherent to choosing a cloud engine, not hidden telemetry. `NetworkSecurityConfig`
  restricts cleartext traffic to the LAN Ollama case.
* All text buffer parsing remains in volatile memory.
* Zero storage persistence of user-typed content. API keys are Keystore-encrypted
  (`CryptoBox`) and excluded from backup/device-transfer (`backup_rules.xml`,
  `data_extraction_rules.xml`).

---

## 5. Development Roadmap & Milestones

### Phase 1: Foundations & Permission Flow

* [x] Initialize Android Studio project (`minSdk = 26`, `targetSdk = 36` as of v1.2.0, was 35).
* [x] Configure build scripts, Kotlin 2.x, Compose BOM, and R8 shrinking rules.
* [x] Build onboarding UI for step-by-step permission granting (`SYSTEM_ALERT_WINDOW` & `AccessibilityService`).

### Phase 2: Accessibility & Overlay Binding

* [x] Implement `SproutAccessibilityService` to listen for editable input nodes (`EditText`, `WebView`, Compose input fields).
* [x] Implement `SproutOverlayManager` to render Compose views via `WindowManager`.
* [x] Build coordinate positioning logic to keep the floating pill adjacent to the cursor or keyboard header.

### Phase 3: Text Processing Engine

* [x] Build `TextEngine` interface and transformation dispatcher.
* [x] Implement the 4 base presets (`Fix`, `Concise`, `Professional`, `Punchy`).
* [x] Implement diff calculation algorithm for visual highlighting of modified text.

### Phase 4: UI & Material 3 Implementation

* [x] Integrate Material 3 theme with dynamic Monet extraction (stable `MaterialTheme`, not `MaterialExpressiveTheme` — see the v1.2.0 note in §1).
* [x] Implement collapsed $\leftrightarrow$ expanded overlay animation transitions.
* [x] Wire **Replace Inline** button to execute `ACTION_SET_TEXT` on the active node.

### Phase 5: Testing, Hardening & Release

* [ ] Compatibility testing across diverse IMEs (Gboard, SwiftKey, Samsung Keyboard) and apps (WhatsApp, Signal, Telegram, Notes, Chrome) — needs a physical/emulator device pass, not yet run for v1.2.0.
* [ ] Profile memory usage (target: $< 25\text{ MB}$ RAM overhead) — not yet measured for v1.2.0.
* [ ] Strip unused resources with ProGuard/R8 to keep APK size $< 8\text{ MB}$ — R8 rules exist but the resulting release APK size hasn't been re-measured since v1.2.0's additions.
* [ ] Build signed standalone APK release — unit tests and `assembleDebug`/`assembleRelease` compile cleanly; no new signed release artifact produced this pass.

### Phase 6: v1.2.0 — Bug Fixes, Redesign, Feature Expansion

* [x] Fixed the per-keystroke engine invocation on both the overlay and Settings sandbox (debounce + `TransformCache`), a `ComposeView` leak on every overlay hide/show, an unbounded LCS diff allocation, punctuation rules mangling emails/URLs/abbreviations, a calculator trigger that silently dropped parentheses and `^`, and a clipboard-fallback injection path that inserted instead of replacing.
* [x] Added dark mode (`ThemeMode`: System/Light/Dark) on top of Monet dynamic color.
* [x] Rebuilt Settings as a `SettingsViewModel` + Navigation 3 graph (`Home`/`Onboarding`/`Engine`/`Appearance`/`AppRules`/`Snippets`/`History`/`Sandbox`) replacing a single 1,150-line composable.
* [x] Added 5 new presets (Friendly, Summarize, Bulletize, Expand, Custom) across the local rule engine and all four AI engines, per-app overlay rules, a draggable/edge-snapping pill, session transformation history, Spanish-language local rules, and Android Keystore encryption for stored API keys.
* [x] Updated the Claude engine default model, request shape, and error handling for the current Claude API.