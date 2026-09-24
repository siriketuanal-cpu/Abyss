plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.abysstimer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aistudio.abysstimer.native.pxvwt"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "1.0.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // For material icons
    implementation("androidx.compose.material:material-icons-extended:1.7.5")

    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
    doLast {
        val srcApk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
        if (srcApk.exists()) {
            copy {
                from(srcApk)
                into(rootDir)
                rename { "AbyssTimer.apk" }
            }
            copy {
                from(srcApk)
                into(file("${rootDir}/.build-outputs"))
            }
        }
    }
}
