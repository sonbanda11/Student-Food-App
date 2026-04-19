import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.studentfood"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.studentfood"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val props = Properties()
        val lp = rootProject.file("local.properties")
        if (lp.exists()) lp.inputStream().use { props.load(it) }
        
        // OpenWeather API Key - Use hardcoded key directly to avoid local.properties issues
        val owmApiKey = "39fbf9f993aed186271f178e0c8d6099"
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"${owmApiKey}\"")
        
        // Google Maps API Key - Use hardcoded key directly
        val googleMapsApiKey = "AIzaSyB2NXjWIQaxzQfjsrASQQ8fYQ_Exp83HYc"
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"${googleMapsApiKey}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    // Core
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)

    // Lifecycle
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.viewmodel)

    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Splash
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Google Maps + Location
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // API (Retrofit)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")

    // Animation
    implementation("com.airbnb.android:lottie:6.4.1")

    // Image
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // ViewPager
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // Logging cho Retrofit
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Flexbox
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Avatar tròn
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // 🔥 Zoom ảnh (FIX LỖI CHÍNH)
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    // Permission
    implementation("io.github.ParkSangGwon:tedpermission-normal:3.4.2")

    // GridLayout
    implementation("androidx.gridlayout:gridlayout:1.0.0")

    implementation("com.facebook.shimmer:shimmer:0.5.0")

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}