plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.hectorscraper.app"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.hectorscraper.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"https://hscrapper1.free.beeceptor.com/\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "BASE_URL", "\"https://hscrapper1.free.beeceptor.com/\"")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.intuit.ssp:ssp-android:1.1.1")
    implementation("com.intuit.sdp:sdp-android:1.1.1")

    implementation("org.apache.poi:poi-ooxml:5.2.3")

    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.6.1")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.2")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.2")
    implementation("com.squareup.retrofit2:converter-scalars:2.6.1")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.7.1")
    implementation("com.orhanobut:logger:2.2.0")
    implementation("com.google.dagger:hilt-android:2.33-beta")
    implementation("androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    //  Todo LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    //  Todo OkHttp3
    implementation("com.github.bumptech.glide:okhttp3-integration:4.8.0")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.2")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}