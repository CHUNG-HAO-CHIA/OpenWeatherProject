import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

android {
    namespace = "com.app.openweather.core.network"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
        buildConfigField("String", "BASE_URL", "\"https://api.openweathermap.org/\"")
        buildConfigField("String", "API_KEY", "\"${localProperties["WEATHER_API_KEY"]}\"")
        buildConfigField("String", "APPLICATION_ID", "\"com.app.openweather\"")
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.android)
}
