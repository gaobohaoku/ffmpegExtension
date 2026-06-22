import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.metax.nativead.ffmpegext"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    ndkVersion = libs.versions.ndk.get()

    defaultConfig {

        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
        externalNativeBuild {
            cmake {
                cppFlags("")
            }
        }

        ndk {
            abiFilters += listOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = libs.versions.cmake.get()
        }
    }
}

// Gradle task to setup ffmpeg
val ffmpegSetup by tasks.registering(Exec::class) {
    workingDir = file("../ffmpeg")
    commandLine("bash", "setup.sh")
}

// 建议使用 tasks.named 的方式挂载依赖，防止 preBuild 尚未创建时报错
tasks.named("preBuild") {
    dependsOn(ffmpegSetup)
}

tasks.preBuild.dependsOn(ffmpegSetup)

dependencies {
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.google.errorprone.annotations)
    implementation(libs.androidx.annotation)
    compileOnly(libs.checker.qual)
    compileOnly(libs.kotlin.annotations.jvm)
}
