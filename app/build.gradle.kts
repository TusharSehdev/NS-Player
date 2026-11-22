import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.genuine.nsplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.genuine.nsplayer"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.jks")
            storePassword = "nsplayer123"
            keyAlias = "nsplayer"
            keyPassword = "nsplayer123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            signingConfig = signingConfigs.getByName("release")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core AndroidX & Lifecycle
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Jetpack Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // Media3 ExoPlayer
    val media3Version = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3Version")
    implementation("androidx.media3:media3-exoplayer-rtsp:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("androidx.media3:media3-datasource-okhttp:$media3Version")

    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-paging:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coil Image Loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // OkHttp Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}


tasks.register("packageDistribution") {
    dependsOn("assembleRelease", "assembleDebug")
    doLast {
        val root = project.rootDir
        val pkgDir = File(root, "packages")
        val distDir = File(root, "dist")
        pkgDir.mkdirs()
        distDir.mkdirs()

        val releaseDir = File(project.layout.buildDirectory.get().asFile, "outputs/apk/release")
        val debugDir = File(project.layout.buildDirectory.get().asFile, "outputs/apk/debug")

        val targets = listOf(
            Triple(File(releaseDir, "app-universal-release.apk"), "NEXORA-universal-release.apk", "Universal (Mobile + TV) Release APK"),
            Triple(File(releaseDir, "app-arm64-v8a-release.apk"), "NEXORA-arm64-v8a-release.apk", "ARM64 64-bit Architecture Release APK"),
            Triple(File(releaseDir, "app-armeabi-v7a-release.apk"), "NEXORA-armeabi-v7a-release.apk", "ARMv7 32-bit Architecture Release APK"),
            Triple(File(releaseDir, "app-x86_64-release.apk"), "NEXORA-x86_64-release.apk", "x86_64 Architecture Release APK"),
            Triple(File(releaseDir, "app-x86-release.apk"), "NEXORA-x86-release.apk", "x86 Architecture Release APK"),
            Triple(File(debugDir, "app-universal-debug.apk"), "NEXORA-universal-debug.apk", "Universal Debug Development APK")
        )

        val shaLines = mutableListOf<String>()
        val manifestItems = mutableListOf<String>()

        targets.forEach { (src, targetName, desc) ->
            if (src.exists()) {
                val destPkg = File(pkgDir, targetName)
                val destDist = File(distDir, targetName)
                src.copyTo(destPkg, overwrite = true)
                src.copyTo(destDist, overwrite = true)

                val sha256 = MessageDigest.getInstance("SHA-256")
                    .digest(destPkg.readBytes())
                    .joinToString("") { b -> "%02x".format(b) }

                shaLines.add("$sha256  $targetName")
                manifestItems.add("""
                    {
                      "filename": "$targetName",
                      "description": "$desc",
                      "sizeBytes": ${destPkg.length()},
                      "sha256": "$sha256"
                    }
                """.trimIndent())

                println("Successfully Packaged: $targetName (${destPkg.length()} bytes)")
            }
        }

        val shaFile = File(pkgDir, "SHA256SUMS.txt")
        shaFile.writeText(shaLines.joinToString("\n") + "\n")
        File(distDir, "SHA256SUMS.txt").writeText(shaLines.joinToString("\n") + "\n")

        val manifestJson = """
        {
          "application": "NEXORA",
          "tagline": "Your Media. Your Way.",
          "version": "1.0.0",
          "versionCode": 1,
          "buildTimestamp": ${System.currentTimeMillis()},
          "packages": [
            ${manifestItems.joinToString(",\n    ")}
          ]
        }
        """.trimIndent()

        File(pkgDir, "package-manifest.json").writeText(manifestJson)
        File(distDir, "package-manifest.json").writeText(manifestJson)
    }
}
