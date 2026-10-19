plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.appmca"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.appmca"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Thư viện tải ảnh Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Thư viện Retrofit để gọi API Supabase
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp để tạo HTTP client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Thư viện Google ML Kit Nhận diện khuôn mặt (Face Detection)
    implementation("com.google.mlkit:face-detection:16.1.6")

    // Thư viện TensorFlow Lite chạy AI Offline Nhận diện khuôn mặt (Face Recognition)
    implementation("org.tensorflow:tensorflow-lite:2.16.1")

    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}