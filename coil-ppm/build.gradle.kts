import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("coil-multiplatform-library")
}

kotlin {
    // Every target except Android renders through Skiko, so share one implementation between them.
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("skiko") {
                withJvm()
                withJs()
                withWasmJs()
                withNative()
            }
        }
    }

    android {
        // Bitmap creation needs a real device or emulator: ./gradlew :coil-ppm:connectedAndroidDeviceTest
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.ui.graphics)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.junit)
        }
    }
}
