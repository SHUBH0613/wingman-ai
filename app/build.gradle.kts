plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.wingman.ai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.wingman.ai"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Securely read API key from environment variable (GitHub Secrets) or project property; never stored in git
        val apiKey = (project.findProperty("GEMINI_API_KEY") as? String)
            ?: System.getenv("GEMINI_API_KEY")
            ?: ""
        buildConfigField("String", "DEFAULT_GEMINI_API_KEY", "\"$apiKey\"")
    }

    buildFeatures {
        buildConfig = true
        viewBinding = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.kotlinx.coroutines.android)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.1")
    implementation(libs.okhttp)
    implementation(libs.gson)
    testImplementation("junit:junit:4.13.2")
}
