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

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.ui.graphics)
        }
    }
}
