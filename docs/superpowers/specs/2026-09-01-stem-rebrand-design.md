# Stem rebrand — design spec

Status: approved by user, ready for implementation planning
Source design: Claude Design project "Mobile app design for Sprout", file `Stem.dc.html`
(`https://claude.ai/design/p/840d725f-ca01-4818-bf31-e1aae45971ad?file=Stem.dc.html`)

## Summary

Full rebrand of the shipping Android app **Sprout** (`com.veggiebit.sprout`, currently v1.6.1)
into **Stem**: new name, new package id, a fixed warm-stone color palette replacing Material You
dynamic (Monet) color, new typography (Manrope + Space Mono replacing Roboto), a sharper/flatter
shape language, custom geometric glyphs replacing Material Icons, and restyled onboarding, home,
snippets, history, and settings screens plus the floating overlay pill and text-selection sheet.

This is a **re-skin of working code**, not new features. Every screen in the Stem mockup already
has a functioning counterpart in Sprout (confirmed by codebase research): providers ↔
`EngineScreen`, presets ↔ `TransformPreset`/`PresetChipsRow`, diff view ↔ `DiffViewer`, history ↔
`HistoryScreen`/`TransformHistory`, snippets ↔ `SnippetsScreen`, overlay pill ↔
`SproutOverlayManager`/`SproutFloatingOverlay`/`SproutPill`, selection sheet ↔
`ProcessTextActivity`.

## Decisions already made (do not re-litigate)

1. **Rename scope**: full rename, including `applicationId`/`namespace`
   (`com.veggiebit.sprout` → `com.veggiebit.stem`). This breaks the update path for existing
   Sprout installs — accepted trade-off.
2. **Color system**: fully replace Monet dynamic color with Stem's fixed warm-stone palette.
   `dynamicColor`/`dynamicLightColorScheme`/`dynamicDarkColorScheme` branches are removed from
   `Theme.kt` entirely — no per-device wallpaper-derived color, no "classic" fallback toggle.
3. **Rollout**: implement everything in one pass (not phased).
4. **IA gap**: the Stem mockup's bottom-tab bar only shows Home / Snippets / History / Settings.
   Sandbox (manual test harness) and AppRules (per-app allow/deny) are real screens today with
   no equivalent in the mockup. They are **nested under Settings** as additional rows that push
   to their existing full-screen implementations, styled to match Stem.

## 1. Rename mechanics

| What | From | To |
|---|---|---|
| `applicationId` / `namespace` (`app/build.gradle.kts`) | `com.veggiebit.sprout` | `com.veggiebit.stem` |
| Java package dir | `app/src/main/java/com/veggiebit/sprout/` | `app/src/main/java/com/veggiebit/stem/` |
| Every `package`/`import com.veggiebit.sprout...` line | — | 63 files, mechanical find/replace after the directory move |
| `rootProject.name` (`settings.gradle.kts`) | `"Sprout"` | `"Stem"` (Gradle/IDE display name only) |
| Theme style name (`values/themes.xml`, `values-night/themes.xml`) | `Theme.Sprout` | `Theme.Stem` |
| `AndroidManifest.xml` | `android:theme="@style/Theme.Sprout"`, `android:name=".app.SproutApplication"` refs, activity labels `"Sprout Assistant"` (×2) | `Theme.Stem`, `.app.StemApplication`, `"Stem Assistant"` |
| `strings.xml` (3 strings) | `app_name = "Sprout"`, `accessibility_service_label = "Sprout Text Assistant"`, `accessibility_service_description` (mentions Sprout) | `"Stem"`, `"Stem Text Assistant"`, updated description text |
| DataStore file (`PreferencesRepository.kt`) | `preferencesDataStore(name = "sprout_settings")` | `"stem_settings"` — safe, since the applicationId change already means no data carries forward |
| Backup-exclusion XML (`backup_rules.xml`, `data_extraction_rules.xml`) | `sprout_settings.preferences_pb` references | `stem_settings.preferences_pb`, kept in lockstep with the above |
| QS tile drawable file | `drawable/ic_tile_sprout.xml` | rename file + reference in manifest/`StemTileService`, redraw path to the new mark (see §4) |

### Symbol rename (for consistency, not strictly required by the package move)

Every `Sprout*`-prefixed class/object/composable is renamed to `Stem*`. Non-exhaustive list from
the research pass — the implementation plan should treat this as "rename every `Sprout` prefix
found," not limited to this list:

`SproutApplication`, `SproutTheme`, `SproutTypography`, `SproutShapes`, `SproutPillShape`,
`SproutCapsuleShape`, `SproutChipShape`, `SproutLargeIncreasedShape`, `SproutExtendedColors`,
`LocalSproutExtendedColors`, `SproutLight*`/`SproutDark*` color vals, `SproutAccessibilityService`,
`SproutOverlayManager`, `SproutTileService`, `SproutFloatingOverlay`, `SproutPill`,
`SproutNavDisplay`, `SproutRoutes`, `SproutSubScreen`, `SproutSegmentedGroup`,
`SproutThinkingIndicator`/`SproutThinkingCard`/`SproutThinkingBadge`.

### Launcher / QS tile icon

Redraw to a geometric plant-stem mark matching the wordmark glyph used throughout the design (a
vertical stem stroke + one diagonal leaf stroke at ~38°, per the repeated `<div>` pairs in
`Stem.dc.html`'s brand block and onboarding step 0/1/2 icons). Deliverables:

- `drawable/ic_launcher_background.xml` — flat fill using theme `bg` (`#FBF9F6` light /
  `#110F0D` dark equivalent — adaptive icons only support one static background, so use the
  light `bg` value as today's file does)
- `drawable/ic_launcher_foreground.xml` — convert from the current raster PNG to a vector
  (`pathData`) drawing the stem+leaf mark in `ink` (`#151311`), matching the two-stroke
  construction shown in the design (`width:40px;height:40px` brand mark: vertical bar
  `left:18px;top:4px;width:4px;height:32px` + rotated bar `left:7px;top:11px;width:22px;height:4px;
  transform:rotate(38deg)`, scaled to the 108×108dp adaptive-icon canvas)
- `drawable/ic_launcher_monochrome.xml` — same path, alpha-only, for Android 13+ themed icons
- Regenerate the 10 legacy `mipmap-*/ic_launcher*.webp` composites from the new vector (standard
  Android Studio "Image Asset" export, or manual per-density raster export)
- `drawable/ic_tile_stem.xml` — same stem+leaf mark simplified to a 24×24dp single/double-path
  vector (tile system re-tints at render time, so exact fill color is irrelevant, matching
  today's `ic_tile_sprout.xml` convention)

## 2. Theme foundation

### Color.kt

Replace entirely. The design's OKLCH tokens are converted to sRGB hex once (converted via
Björn Ottosson's OKLab matrices — script-verified, not hand math) and hardcoded, matching the
existing file's convention of literal hex values:

**Light**
| token | oklch (source) | hex |
|---|---|---|
| bg | `98.2% 0.004 75` | `#FBF9F6` |
| surface | `99.4% 0.002 75` | `#FEFDFC` |
| surface2 | `95.5% 0.005 75` | `#F2F0EC` |
| surface3 | `91.5% 0.006 75` | `#E5E2DF` |
| border | `86% 0.007 75` | `#D4D0CC` |
| ink | `19% 0.006 75` | `#151311` |
| inkMuted | `46% 0.007 75` | `#5A5754` |
| inkFaint | `64% 0.007 75` | `#8F8C88` |
| onInk | (= bg) | `#FBF9F6` |
| add (diff) | `52% 0.07 145` | `#4F7450` |
| remove (diff) | `52% 0.08 35` | `#915849` |

**Dark**
| token | oklch (source) | hex |
|---|---|---|
| bg | `17% 0.006 75` | `#110F0D` |
| surface | `21% 0.007 75` | `#1A1815` |
| surface2 | `25% 0.008 75` | `#24211D` |
| surface3 | `30% 0.009 75` | `#312D29` |
| border | `34% 0.01 75` | `#3B3732` |
| ink | `95% 0.004 75` | `#F0EEEB` |
| inkMuted | `72% 0.007 75` | `#A7A4A0` |
| inkFaint | `54% 0.007 75` | `#716E6A` |
| onInk | (= bg) | `#110F0D` |
| add (diff) | `75% 0.08 145` | `#8EBC8F` |
| remove (diff) | `78% 0.08 35` | `#E6A595` |

`shadow` tokens (`oklch(20% 0.01 75 / 0.14)` light, `oklch(0% 0 0 / 0.45)` dark) are **not**
converted precisely — Compose/Material elevation shadows are already OS-native; use
`Color.Black.copy(alpha = 0.14f)` / `0.45f` for any explicit shadow-color needs (overlay pill,
selection sheet).

Map onto Compose `ColorScheme` roles used today (`primary`→`ink`, `onPrimary`→`onInk`,
`background`→`bg`, `surface`→`surface`, `surfaceVariant`→`surface2`, `surfaceContainer*`→
`surface2`/`surface3` graduated, `outline`/`outlineVariant`→`border`, `onSurface`→`ink`,
`onSurfaceVariant`→`inkMuted`) — the implementation plan should produce an explicit mapping
table per `ColorScheme` field since Stem's palette (11 tokens) is much flatter than Material's
(~26 roles); several Material roles will collapse onto the same Stem token.

`StemExtendedColors` (renamed from `SproutExtendedColors`) drops `diffAddedBackground` /
`diffDeletedBackground` — the design renders diff tokens as colored text + strikethrough
(removed) / underline (added), no background fill (see §3, `DiffViewer.kt`).

### Theme.kt

Remove `dynamicColor` parameter and both `dynamic*ColorScheme(...)` branches. `StemTheme(...)`
takes only `themeMode: ThemeMode`, resolves light/dark, and applies the fixed
`StemLightColorScheme`/`StemDarkColorScheme`.

### Type.kt

Bundle Manrope (weights 400/500/600/700/800) and Space Mono (400/700) as OFL font files in a new
`app/src/main/res/font/` directory (fetched from the Google Fonts GitHub repo — both OFL
licensed, matching the design's `@import` of the same families). `StemTypography` uses Manrope
for all UI text roles and Space Mono for the label/metadata role (`labelSmall`, and any new
uppercase-tracked "eyebrow" text style the screens need — the design uses Space Mono for preset
tags, timestamps, and section eyebrows throughout).

### Shape.kt

Replace Material's 4–28dp rounded-corner scale with Stem's sharp geometry: the design uses
3px and 4px border-radius almost everywhere (buttons, chips, cards), with a few larger radii for
the selection sheet (10px) and the overlay pill's expand button (4px). `StemShapes` /
`StemPillShape` / `StemCapsuleShape` / `StemChipShape` updated to these literal values — implementation
plan should extract the exact radius used per component from `Stem.dc.html`'s inline styles
rather than inventing new ones.

### Custom glyphs

The design's preset/provider icons (`squareOutline`, `bar`, `squareFilled`, `triangle`,
`circleOutline`, `lines`, `dots`, `diamond`, `plus`) and the repeated stem+leaf brand mark are
plain CSS shapes (bordered/filled divs, one rotated bar), not an icon font. Reproduce as small
Compose composables (`Box` with `border`/`background`/`clip`/`rotate` modifiers, or `Canvas` for
the stem mark) rather than sourcing/replacing Material Icon assets. These replace the current
`Icons.Rounded.*` usage listed in the research pass (Spa, TouchApp, Layers, CheckCircle, Apps,
History, Palette, Psychology, Science, Style, ChevronRight, AutoAwesome, Add, DeleteOutline,
KeyboardCommandKey, TextFields, Search, Check, Clear, ContentCopy, DeleteSweep, Code, Computer,
ErrorOutline, Refresh, Visibility/VisibilityOff, ArrowBack/ArrowForward) — implementation plan
should decide per-icon whether a direct geometric equivalent exists in the design or whether a
reasonable new glyph must be authored in the same visual language (the design doesn't cover
every icon this app currently uses, e.g. Search, Copy, Delete-sweep).

## 3. Screen-by-screen mapping

| Screen (file) | Change |
|---|---|
| `OnboardingScreen.kt` | Restructure from today's 3-step (welcome / accessibility / overlay) to the design's 4-step flow: step 0 intro (brand mark + tagline, no separate welcome screen), step 1 accessibility access, step 2 overlay permission, step 3 "You're set" done screen. Progress indicator becomes 4 segments (today: 3). Skip button hidden on the last step (today: always visible). CTA label changes per step (`Get started` / `Continue` / `Continue` / `Open Stem`), disabled until the current step's permission is granted (today: always enabled, advances regardless). |
| `HomeScreen.kt` | Restructured to the design's layout: service-enabled card with toggle switch, horizontally-scrolling quick-preset chips, a "Try it" demo card with a sample string + "Simulate text selection" affordance, a "Recent" list (3 most-recent history entries) with a "See all" link to History. Bottom tab bar (Home / Snippets / History / Settings) replaces today's single scrollable settings list — Settings tab hosts the nested Sandbox/AppRules rows (§ IA gap decision). |
| `SnippetsScreen.kt` | Restyle in place: trigger/expansion input pair + add button, list of existing snippets with delete (×) affordance. Existing custom-AI-command section (`?trigger`) and built-in-triggers reference are real features not shown in the mockup — keep them, styled to match, placed below/around the mockup's snippet list rather than removed. |
| `HistoryScreen.kt` | Restyle in place: tappable rows expand to show word-level diff (strikethrough/underline tokens) instead of today's plain before/after text. Keep search-filter and clear-history dialog (not shown in mockup but real features). |
| Settings (new composable, or `EngineScreen.kt` + `AppearanceScreen.kt` merged under one route) | Combine: AI provider list (from `EngineScreen`, restyled radio rows with the provider glyphs), Appearance light/dark toggle (from `AppearanceScreen`), a static Privacy note (new, copy from the design), and — per the IA decision — additional rows linking to Sandbox and AppRules. Provider credential fields keep today's behavior (per-provider endpoint + model text fields), restyled. |
| `SproutSubScreen.kt` → `StemSubScreen.kt` | Restyle shared scaffold (top bar, back button) to match; no structural change. |
| `SproutOverlayManager`/`SproutFloatingOverlay`/`SproutPill` | Restyle collapsed pill (design: solid `ink`-colored 40×40 4px-radius square with the stem glyph, vs. today's colored circle with Material `Spa` icon) and expanded panel (design: 10px-radius card, preset chip row, diff-token or before/after view depending on preset, Dismiss/Replace actions) to match `Stem.dc.html`'s overlay screen. Drag/snap/debounce behavior unchanged. |
| `ProcessTextActivity.kt` | Restyle the selection dialog to match the design's "Selection sheet" (bottom sheet with scrim, drag handle, preset chips, diff/before-after view, Copy/Replace-inline actions) rather than today's centered rounded card. |
| `DiffViewer.kt` | Switch from background-highlight tokens to colored-text + strikethrough(removed)/underline(added) tokens, using `StemExtendedColors.diffAdded`/`diffDeleted` directly as text color (no background fields — see §2). |
| `SproutThinkingIndicator.kt` → `StemThinkingIndicator.kt` | Restyle colors/shape only — the design has no explicit "thinking" state to crib from; keep current motion/behavior, reskin to the new palette and remove the `AutoAwesome` Material icon in favor of a Stem-styled equivalent (simple pulsing dot/mark). |
| `SandboxScreen.kt`, `AppRulesScreen.kt` | Restyle only (colors/type/shape/shared components) — no structural change, now reached via Settings per the IA decision. |

## 4. Out of scope

- No changes to transform engines, API clients, diff calculation logic, snippet/command parsing,
  or any non-UI business logic.
- No changes to permissions requested or the accessibility-service/overlay-window mechanics
  themselves (only their visual presentation).
- Play Store listing, versioning/release process, and CI are untouched by this spec.
