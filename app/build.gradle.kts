import java.util.Properties
import java.io.FileInputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

// Load Semantic Versioning (SemVer) - MAJOR.MINOR.PATCH with optional pre-release/metadata
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        FileInputStream(versionPropsFile).use { load(it) }
    }
}

val semVerMajor = versionProps.getProperty("VERSION_MAJOR", "1").toInt()
val semVerMinor = versionProps.getProperty("VERSION_MINOR", "0").toInt()
val semVerPatch = versionProps.getProperty("VERSION_PATCH", "0").toInt()
val semVerBuild = (System.getenv("GITHUB_RUN_NUMBER") ?: System.getenv("BUILD_NUMBER") ?: versionProps.getProperty("VERSION_BUILD", "1")).toIntOrNull() ?: 1
val semVerPreRelease = versionProps.getProperty("VERSION_PRE_RELEASE", "").trim()
val semVerBuildMetadata = versionProps.getProperty("VERSION_BUILD_METADATA", "").trim()

// Structured Decimal Mask / CI Build Counter:
// MAJOR (0-214) * 1,000,000 + MINOR (0-99) * 10,000 + PATCH (0-99) * 100 + BUILD (0-99)
// Example: 1.0.0 (build 1) -> 1000001; 1.2.3 (build 42) -> 1020342
val structuredVersionCode = (semVerMajor * 1_000_000) + (semVerMinor * 10_000) + (semVerPatch * 100) + (semVerBuild % 100)
val semVerName = buildString {
    append("$semVerMajor.$semVerMinor.$semVerPatch")
    if (semVerPreRelease.isNotEmpty()) {
        append("-$semVerPreRelease")
    }
    if (semVerBuildMetadata.isNotEmpty()) {
        append("+$semVerBuildMetadata")
    }
}

// Release signing: never committed. Populate keystore.properties locally (see
// keystore.properties.example) or inject the same keys as env vars in CI. Falls back to
// debug signing when absent so local `assembleRelease` still works during development —
// a release built that way is NOT suitable for Play Store upload.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        FileInputStream(keystorePropsFile).use { load(it) }
    }
}
fun keystoreProp(key: String): String? =
    keystoreProps.getProperty(key) ?: System.getenv(key)
val hasReleaseSigning = keystoreProp("RELEASE_STORE_FILE") != null

android {
    namespace = "com.veggiebit.sprout"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.veggiebit.sprout"
        minSdk = 26
        targetSdk = 36
        versionCode = structuredVersionCode
        versionName = semVerName

        buildConfigField("int", "SEMVER_MAJOR", "$semVerMajor")
        buildConfigField("int", "SEMVER_MINOR", "$semVerMinor")
        buildConfigField("int", "SEMVER_PATCH", "$semVerPatch")
        buildConfigField("int", "BUILD_COUNTER", "$semVerBuild")
        buildConfigField("String", "SEMVER_NAME", "\"$semVerName\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProp("RELEASE_STORE_FILE")!!)
                storePassword = keystoreProp("RELEASE_STORE_PASSWORD")
                keyAlias = keystoreProp("RELEASE_KEY_ALIAS")
                keyPassword = keystoreProp("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.datastore.preferences)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)

  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Networking & Serialization (for Ollama Local AI integration)
  implementation(libs.okhttp)
  implementation(libs.kotlinx.serialization.json)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
