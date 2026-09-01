package com.stem.core.util

import com.stem.BuildConfig



/**
 * Exposes Structured Bit-Shift / Decimal Mask Version Code & CI Build Counter metadata.
 */
object AppVersion {
    val major: Int get() = BuildConfig.VERSION_MAJOR
    val minor: Int get() = BuildConfig.VERSION_MINOR
    val patch: Int get() = BuildConfig.VERSION_PATCH
    val buildCounter: Int get() = BuildConfig.BUILD_COUNTER
    val versionCode: Int get() = BuildConfig.STRUCTURED_VERSION_CODE

    val displayString: String get() = "Build $versionCode"
}
