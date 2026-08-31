# Sprout (`com.veggiebit.sprout`) — Project Plan

---

## 1. Project Overview

* **Application Name:** Sprout
* **Package ID:** `com.veggiebit.sprout`
* **Category:** System Utility / Text Enhancement
* **Tech Stack:** Kotlin, Jetpack Compose, Android Accessibility APIs, WindowManager
* **Design Philosophy:** Material 3 Expressive, Monet Dynamic Color (Light Mode Only), 8dp spatial grid
* **Core Purpose:** Real-time, floating inline overlay that detects active text fields, generates instant suggestions (Fix, Concise, Professional, Punchy), and injects replacements directly into the target app without switching context.

---

## 2. Technical Architecture

### 2.1 Core Modules & Services

```
com.veggiebit.sprout/
├── data/
│   ├── datastore/          # Preferences (active presets, trigger behavior)
│   ├── engine/             # Text processing engines (Local on-device / LLM adapter)
│   └── models/             # Data classes (TextPayload, TransformResult, Preset)
├── service/
│   ├── SproutAccessibilityService.kt   # Monitors IME focus, reads/writes text buffers
│   └── SproutOverlayManager.kt         # WindowManager anchor, coordinates Compose overlay
├── ui/
│   ├── components/         # Pill triggers, preset selectors, diff viewers
│   ├── overlay/            # ComposeView injected into WindowManager
│   ├── settings/           # Configuration activity & onboarding flow
│   └── theme/              # M3 Expressive, Monet ColorScheme tokens (Light mode)
└── utils/
    ├── AccessibilityUtils.kt # Node info traversal, bounds calculations
    └── PermissionHelper.kt   # Overlay and accessibility permission verification

```

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

* **Theme Constraint:** Strictly Light Mode (`dynamicLightColorScheme()` fallback to Citrus/Amber seed `#FB8C00`).
* **Surfaces:**
* Background / Base: `surfaceContainerLow`
* Overlay Capsule: `surfaceContainer` with `1.dp` border (`outlineVariant`)
* Active Selection: `primaryContainer` / `onPrimaryContainer`
* Action Buttons: `primary` / `onPrimary`


* **Typography:**
* Headers: `Google Sans` (`HeadlineSmall`, `TitleMedium`)
* Body/Labels: `Google Sans Text` (`BodyMedium`, `LabelLarge`)
* Counters / Metadata: `Roboto Mono` (`LabelSmall`)



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

* No internet permissions (`android.permission.INTERNET` omitted or strictly isolated).
* All text buffer parsing remains in volatile memory.
* Zero storage persistence of user-typed content.

---

## 5. Development Roadmap & Milestones

### Phase 1: Foundations & Permission Flow

* [ ] Initialize Android Studio project (`minSdk = 26`, `targetSdk = 35`).
* [ ] Configure build scripts, Kotlin 2.x, Compose BOM, and R8 shrinking rules.
* [ ] Build onboarding UI for step-by-step permission granting (`SYSTEM_ALERT_WINDOW` & `AccessibilityService`).

### Phase 2: Accessibility & Overlay Binding

* [ ] Implement `SproutAccessibilityService` to listen for editable input nodes (`EditText`, `WebView`, Compose input fields).
* [ ] Implement `SproutOverlayManager` to render Compose views via `WindowManager`.
* [ ] Build coordinate positioning logic to keep the floating pill adjacent to the cursor or keyboard header.

### Phase 3: Text Processing Engine

* [ ] Build `TextEngine` interface and transformation dispatcher.
* [ ] Implement the 4 base presets (`Fix`, `Concise`, `Professional`, `Punchy`).
* [ ] Implement diff calculation algorithm for visual highlighting of modified text.

### Phase 4: UI & M3 Expressive Implementation

* [ ] Integrate Google Material 3 Expressive theme with dynamic Monet extraction.
* [ ] Implement collapsed $\leftrightarrow$ expanded overlay animation transitions.
* [ ] Wire **Replace Inline** button to execute `ACTION_SET_TEXT` on the active node.

### Phase 5: Testing, Hardening & Release

* [ ] Compatibility testing across diverse IMEs (Gboard, SwiftKey, Samsung Keyboard) and apps (WhatsApp, Signal, Telegram, Notes, Chrome).
* [ ] Profile memory usage (target: $< 25\text{ MB}$ RAM overhead).
* [ ] Strip unused resources with ProGuard/R8 to keep APK size $< 8\text{ MB}$.
* [ ] Build signed standalone APK release.