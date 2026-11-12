plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.dimitriazzarone.minianimator"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.dimitriazzarone.minianimator.studio"
        minSdk = 26
        targetSdk = 35
        versionCode = 41
        versionName = "4.0.1"
        resValue("string", "app_name", "MiniAnimator Studio")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
}
dependencies {
    implementation("com.squareup:gifencoder:0.10.1")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.webkit:webkit:1.17.0")
}
