import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.io.FileInputStream
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
  alias(libs.plugins.hilt)
}

android {
  namespace = "com.quranwords"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.quranwords"
    minSdk = 24
    targetSdk = 36
    versionCode = 3
    versionName = "0.2.1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    val keyPropertiesFile = rootProject.file("key.properties")
    val keyProperties = Properties()
    if (keyPropertiesFile.exists()) {
      FileInputStream(keyPropertiesFile).use { stream ->
        keyProperties.load(stream)
      }
    }

    create("release") {
      val rawKeystorePath: String? = System.getenv("KEYSTORE_PATH")
        ?: keyProperties.getProperty("storeFile")
      val keystoreFile = when {
        rawKeystorePath.isNullOrEmpty() -> file("$rootDir/my-upload-key.jks")
        file(rawKeystorePath).isAbsolute -> file(rawKeystorePath)
        else -> rootProject.file(rawKeystorePath)
      }

      storeFile = keystoreFile
      storePassword = System.getenv("STORE_PASSWORD") ?: keyProperties.getProperty("storePassword")
      keyAlias = System.getenv("KEY_ALIAS") ?: keyProperties.getProperty("keyAlias") ?: "upload"
      keyPassword = System.getenv("KEY_PASSWORD") ?: keyProperties.getProperty("keyPassword")
    }

    val customDebugKeystore = file("$rootDir/debug.keystore")
    if (customDebugKeystore.exists()) {
      create("debugConfig") {
        storeFile = customDebugKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val releaseSigning = signingConfigs.getByName("release")
      signingConfig = if ((releaseSigning.storeFile?.exists() == true) && !releaseSigning.storePassword.isNullOrEmpty()) {
        releaseSigning
      } else {
        signingConfigs.findByName("debugConfig") ?: signingConfigs.getByName("debug")
      }
    }
    debug {
      signingConfigs.findByName("debugConfig")?.let {
        signingConfig = it
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Ensure externalOverride (injected by IDE/AGP) falls back to valid signing config if storeFile is missing
fun fixExternalOverrideSigningConfig() {
  android.signingConfigs.findByName("externalOverride")?.let { externalOverride ->
    val targetFile = externalOverride.storeFile
    val releaseSigning = android.signingConfigs.findByName("release")
    val debugSigning = android.signingConfigs.findByName("debugConfig") ?: android.signingConfigs.findByName("debug")

    val validSigning = if (releaseSigning?.storeFile?.exists() == true && !releaseSigning.storePassword.isNullOrEmpty()) {
      releaseSigning
    } else if (debugSigning?.storeFile?.exists() == true) {
      debugSigning
    } else {
      null
    }

    if (validSigning != null) {
      if (targetFile != null && !targetFile.exists()) {
        try {
          targetFile.parentFile?.mkdirs()
          validSigning.storeFile?.copyTo(targetFile, overwrite = true)
        } catch (_: Exception) {}
      }
      externalOverride.storeFile = validSigning.storeFile
      externalOverride.storePassword = validSigning.storePassword
      externalOverride.keyAlias = validSigning.keyAlias
      externalOverride.keyPassword = validSigning.keyPassword
    }
  }
}

afterEvaluate {
  fixExternalOverrideSigningConfig()
}

gradle.taskGraph.whenReady {
  fixExternalOverrideSigningConfig()
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// The Quran Words app is 100% offline-first. Firebase is used only for Crashlytics.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
