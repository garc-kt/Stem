import java.util.Properties
import java.io.FileInputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

// Structured Bit-Shift / Decimal Mask Version Code & CI Build Counter
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        FileInputStream(versionPropsFile).use { load(it) }
    }
}

val versionMajor = versionProps.getProperty("VERSION_MAJOR", "1").toInt()
val versionMinor = versionProps.getProperty("VERSION_MINOR", "0").toInt()
val versionPatch = versionProps.getProperty("VERSION_PATCH", "0").toInt()
val versionPreRelease = versionProps.getProperty("VERSION_PRE_RELEASE", "").trim()
val buildCounter = (System.getenv("GITHUB_RUN_NUMBER") ?: System.getenv("BUILD_NUMBER") ?: versionProps.getProperty("VERSION_BUILD", "1")).toIntOrNull() ?: 1

// Structured Decimal Mask / CI Build Counter:
// MAJOR (0-214) * 1,000,000 + MINOR (0-99) * 10,000 + PATCH (0-99) * 100 + BUILD (0-99)
val structuredVersionCode = (versionMajor * 1_000_000) + (versionMinor * 10_000) + (versionPatch * 100) + (buildCounter % 100)
val semanticVersionName = if (versionPreRelease.isNotEmpty()) "$versionMajor.$versionMinor.$versionPatch-$versionPreRelease" else "$versionMajor.$versionMinor.$versionPatch"

// Release signing
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        FileInputStream(keystorePropsFile).use { load(it) }
    }
}
fun keystoreProp(key: String): String? =
    keystoreProps.getProperty(key) ?: System.getenv(key)
val releaseStoreFile = keystoreProp("RELEASE_STORE_FILE")?.let { 
    val f = file(it)
    if (f.exists()) f else rootProject.file(it)
}
val hasReleaseSigning = releaseStoreFile != null && releaseStoreFile.exists()

android {
    namespace = "com.stem"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.stem"
        minSdk = 26
        targetSdk = 36
        versionCode = structuredVersionCode
        versionName = semanticVersionName

        buildConfigField("int", "VERSION_MAJOR", versionMajor.toString())
        buildConfigField("int", "VERSION_MINOR", versionMinor.toString())
        buildConfigField("int", "VERSION_PATCH", versionPatch.toString())
        buildConfigField("int", "BUILD_COUNTER", buildCounter.toString())
        buildConfigField("int", "STRUCTURED_VERSION_CODE", structuredVersionCode.toString())
        buildConfigField("String", "VERSION_NAME", "\"$semanticVersionName\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = keystoreProp("RELEASE_STORE_PASSWORD")
                keyAlias = keystoreProp("RELEASE_KEY_ALIAS")
                keyPassword = keystoreProp("RELEASE_KEY_PASSWORD")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // KotlinX Serialization JSON
    implementation(libs.kotlinx.serialization.json)

    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)

    // OkHttp Engine
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
