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

    // 这里只获取 Provider 引用，不要调用 .get() 或 .orNull
    val sdkDirProvider = androidComponents.sdkComponents.sdkDirectory
    val ndkDirProvider = androidComponents.sdkComponents.ndkDirectory
    val cmakeProvider = libs.versions.cmake

    doFirst {
        // 1. 安全获取 SDK
        val sdkPath = try {
            sdkDirProvider.orNull?.asFile?.absolutePath
        } catch (e: Exception) { null }
            ?: throw GradleException("❌ 找不到 Android SDK 路径，请检查环境！")

        // 2. 安全获取 NDK (强力拦截 AGP 的原生异常)
        val ndkPath = try {
            ndkDirProvider.orNull?.asFile?.absolutePath
        } catch (e: Exception) { null }
            ?: throw GradleException("❌ 找不到 Android NDK 路径！\n【必做】：请务必在 android {} 闭包中添加 ndkVersion = \"你的版本号\"，并确保已在 SDK Manager 中下载该 NDK！")

        // 3. 安全获取 CMake
        val cmakeVersion = try {
            cmakeProvider.orNull
        } catch (e: Exception) { null }
            ?: throw GradleException("❌ 找不到 CMake 版本！请确保 libs.versions.toml 中配置了 cmake = \"版本号\"")

        // 注入环境变量
        environment("ANDROID_SDK_HOME", sdkPath)
        environment("ANDROID_NDK_HOME", ndkPath)
        environment("ANDROID_CMAKE_VERSION", cmakeVersion)

        println("FFmpeg Setup 环境准备完毕：NDK = $ndkPath")
    }

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
