package com.veggiebit.sprout.core.version

import com.veggiebit.sprout.BuildConfig

/**
 * Exposes Semantic Versioning (SemVer) metadata for the application.
 */
object AppVersion {
    val major: Int get() = BuildConfig.SEMVER_MAJOR
    val minor: Int get() = BuildConfig.SEMVER_MINOR
    val patch: Int get() = BuildConfig.SEMVER_PATCH
    val versionName: String get() = BuildConfig.SEMVER_NAME

    val displayString: String get() = "v$versionName"
}
