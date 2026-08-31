package com.veggiebit.sprout.features.settings.data

/**
 * Per-app override for whether the floating overlay appears. `AUTO` follows the global
 * "Floating Pill Over Other Apps" switch; `ALWAYS`/`NEVER` pin a specific app regardless.
 * Supersedes the old blanket [SproutUserSettings.blacklistedPackages] set (still read for a
 * one-time migration — see [PreferencesRepository]).
 */
enum class AppRuleMode(val id: String, val label: String) {
    AUTO(id = "auto", label = "Auto"),
    ALWAYS(id = "always", label = "Always show"),
    NEVER(id = "never", label = "Never show");

    companion object {
        fun fromId(id: String): AppRuleMode = entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AUTO
    }
}
