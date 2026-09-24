plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.abysstimer.web"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aistudio.abysstimer.web.pxvwt"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "1.0.6"

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

    sourceSets {
        getByName("main") {
            assets.srcDirs("../app/src/main/assets")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
    doLast {
        val srcApk = layout.buildDirectory.file("outputs/apk/debug/webview_app-debug.apk").get().asFile
        if (srcApk.exists()) {
            copy {
                from(srcApk)
                into(rootDir)
                rename { "AbyssTimer_WebView.apk" }
            }
        }
    }
}
