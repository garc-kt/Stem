package com.veggiebit.sprout.app.theme

/**
 * User-facing theme preference. `SYSTEM` follows the device's day/night setting; `LIGHT`/`DARK`
 * pin the app regardless of the system setting. Superseded plan.md §3.1's original "strictly
 * light mode" constraint per an explicit user decision to support dark mode.
 */
enum class ThemeMode(val id: String, val label: String) {
    SYSTEM(id = "system", label = "System"),
    LIGHT(id = "light", label = "Light"),
    DARK(id = "dark", label = "Dark");

    companion object {
        fun fromId(id: String): ThemeMode = entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SYSTEM
    }
}
