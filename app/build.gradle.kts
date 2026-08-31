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
val semVerPreRelease = versionProps.getProperty("VERSION_PRE_RELEASE", "").trim()
val semVerBuildMetadata = versionProps.getProperty("VERSION_BUILD_METADATA", "").trim()

// Semantic Version Code: MAJOR * 10000 + MINOR * 100 + PATCH
val semVerCode = semVerMajor * 10000 + semVerMinor * 100 + semVerPatch
val semVerName = buildString {
    append("$semVerMajor.$semVerMinor.$semVerPatch")
    if (semVerPreRelease.isNotEmpty()) {
        append("-$semVerPreRelease")
    }
    if (semVerBuildMetadata.isNotEmpty()) {
        append("+$semVerBuildMetadata")
    }
}

android {
    namespace = "com.veggiebit.sprout"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.veggiebit.sprout"
        minSdk = 26
        targetSdk = 35
        versionCode = semVerCode
        versionName = semVerName

        buildConfigField("int", "SEMVER_MAJOR", "$semVerMajor")
        buildConfigField("int", "SEMVER_MINOR", "$semVerMinor")
        buildConfigField("int", "SEMVER_PATCH", "$semVerPatch")
        buildConfigField("String", "SEMVER_NAME", "\"$semVerName\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
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
  implementation(libs.okhttp.logging)
  implementation(libs.kotlinx.serialization.json)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
