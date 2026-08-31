package com.veggiebit.sprout.features.settings.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Navigation 3 route keys for the settings app graph. */
sealed interface SproutRoute : NavKey

@Serializable
data object Onboarding : SproutRoute

@Serializable
data object Home : SproutRoute

@Serializable
data object Engine : SproutRoute

@Serializable
data object Appearance : SproutRoute

@Serializable
data object AppRules : SproutRoute

@Serializable
data object Snippets : SproutRoute

@Serializable
data object History : SproutRoute

@Serializable
data object Sandbox : SproutRoute
