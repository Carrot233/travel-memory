import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.carrot233.travelmemory"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.carrot233.travelmemory"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val amapKey = readBuildSetting("AMAP_KEY")
        val amapSecurityCode = readBuildSetting("AMAP_SECURITY_CODE")
        buildConfigField("String", "AMAP_KEY", "\"$amapKey\"")
        buildConfigField("String", "AMAP_SECURITY_CODE", "\"$amapSecurityCode\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
}

private fun readBuildSetting(name: String): String {
    val fromGradle = project.findProperty(name) as String?
    val fromEnv = System.getenv(name)
    val localFile = rootProject.file("local.properties")
    val fromLocal = if (localFile.isFile) {
        Properties().apply {
            localFile.inputStream().use { load(it) }
        }.getProperty(name)
    } else {
        null
    }
    return (fromGradle ?: fromEnv ?: fromLocal ?: "")
        .trim()
        .filter { it.isLetterOrDigit() || it == '_' || it == '-' }
}
