import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
}

val limanPhotosVersion = "1.0.0"

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "LimanPhotos"
            isStatic = true
        }
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.multiplatform.settings)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.mockk)
            implementation(libs.turbine)
            implementation(libs.multiplatform.settings.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ollama4j)
            implementation(libs.jackson.databind)
            implementation(libs.lucene.core)
            implementation(libs.lucene.queryparser)
            implementation(libs.lucene.analyzers.common)
        }
    }
}

android {
    namespace = "com.limanphotos.limandoc"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.limanphotos.limandoc"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

//compose.desktop {
//    application {
//        mainClass = "org.example.com.limanphotos.limandoc.MainKt"
//
//        nativeDistributions {
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
//            packageName = "LimanPhotos"
//            packageVersion = "1.0.0"
//        }
//    }
//}

compose.desktop {
    application {
        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        if (System.getProperty("os.name").contains("Mac")) {
            println("Opening_apple_events")
            jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")

            jvmArgs("--add-opens", "java.desktop/com.apple.eawt=ALL-UNNAMED")
            jvmArgs("--add-opens=java.desktop/com.apple.eawt.event=ALL-UNNAMED")
            args("-XDignore.symbol.file --add-exports java.desktop/com.apple.eawt.event=ALL-UNNAMED")
        }

        mainClass = "com.limanphotos.limandoc.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "LimanPhotos"
            packageVersion = limanPhotosVersion
            modules("java.sql")

            macOS {
                iconFile.set(project.file("appicon/logo.icns"))
                bundleID = "com.limanphotos.limandoc"
            }
            windows {
                iconFile.set(project.file("appicon/logo.ico"))
                shortcut = true // adds shortcut to Windows desktop
                menu = true
            }
            linux {
                iconFile.set(project.file("appicon/logo.png"))
                modules("jdk.security.auth")
            }
        }
        // https://github.com/JetBrains/compose-multiplatform/blob/master/gradle-plugins/compose/src/test/test-projects/application/proguard/build.gradle
        buildTypes.release.proguard {
            isEnabled.set(false)
        }
        buildTypes.release.proguard {
            optimize.set(true)
        }
        buildTypes.release.proguard {
            obfuscate.set(false)
        }
    }
}