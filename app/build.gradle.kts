plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.newsagent"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.newsagent"
        minSdk = 24
        targetSdk = 35
        versionCode = 20
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Debug signing config - uses default debug keystore
        getByName("debug") {
            // Android automatically uses the default debug keystore
            // Located at: ~/.android/debug.keystore
            // This is secure for development/testing builds
        }
        
        // Release signing config - uses environment variables for CI/CD
        create("release") {
            // Check if all required signing environment variables are set
            val keystorePath = System.getenv("KEYSTORE_FILE")
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("KEY_ALIAS")
            val keyPassword = System.getenv("KEY_PASSWORD")
            
            if (keystorePath != null && keystorePassword != null && 
                keyAlias != null && keyPassword != null) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
            // If not all variables are set, this config will be incomplete
            // and the fallback logic in buildTypes.release will use debug signing instead
        }
    }

    buildTypes {
        debug {
            // Use debug signing config
            signingConfig = signingConfigs.getByName("debug")
        }
        
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Use release signing if all required variables are available, otherwise fall back to debug
            val hasAllSigningVars = System.getenv("KEYSTORE_FILE") != null &&
                                    System.getenv("KEYSTORE_PASSWORD") != null &&
                                    System.getenv("KEY_ALIAS") != null &&
                                    System.getenv("KEY_PASSWORD") != null
            
            signingConfig = if (hasAllSigningVars) {
                signingConfigs.getByName("release")
            } else {
                // Fall back to debug signing if release keys not fully configured
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    
    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Room database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    
    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
