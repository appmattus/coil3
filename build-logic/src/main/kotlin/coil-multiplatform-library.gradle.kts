import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Mirrors the target set published by io.coil-kt.coil3:coil-compose:3.6.3:
// android, iosArm64, iosSimulatorArm64, js (browser + node), jvm (11), macosArm64, wasmJs (browser + node)

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

// coil-core's JS and Wasm code imports Skiko's runtime (js-reexport-symbols.mjs, skiko.mjs, skiko.wasm), which
// isn't copied into test bundles automatically. Unpack it so it can be added to the test resources below.
val skikoWasmRuntime = configurations.create("skikoWasmRuntime") {
    isCanBeConsumed = false
    isTransitive = false
}
dependencies {
    skikoWasmRuntime(libs.findLibrary("skiko-wasm-runtime").get())
}
val unpackSkikoWasmRuntime = tasks.register<UnpackSkikoWasmRuntime>("unpackSkikoWasmRuntime") {
    runtimeJars.from(skikoWasmRuntime)
    outputDirectory.set(layout.buildDirectory.dir("skiko-wasm-runtime"))
}

kotlin {
    explicitApi()

    // Every target except Android renders through Skiko, so share one implementation between them in skikoMain.
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
        namespace = "com.appmattus.coil3." + project.name.removePrefix("coil-").replace('-', '.')
        compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
        minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        withHostTest {}
        // Bitmap creation needs a real device or emulator: ./gradlew connectedAndroidDeviceTest
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // Skiko's runtime needs a browser (it writes to `window`), so tests run in headless Chrome rather than Node.
    js {
        browser {
            testTask { useKarma { useChromeHeadless() } }
        }
        nodejs {
            testTask { enabled = false }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask { useKarma { useChromeHeadless() } }
        }
        nodejs {
            testTask { enabled = false }
        }
    }

    iosArm64()
    iosSimulatorArm64()
    macosArm64()

    sourceSets {
        all {
            languageSettings.optIn("com.appmattus.coil3.core.InternalCoilDecoderApi")
        }
        commonMain.dependencies {
            api(libs.findLibrary("coil-core").get())
            implementation(libs.findLibrary("compose-ui-graphics").get())
            // Every decoder module shares the raster and bitmap code in :coil-core.
            if (project.path != ":coil-core") {
                implementation(project(":coil-core"))
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        getByName("androidDeviceTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.findLibrary("androidx-test-runner").get())
            implementation(libs.findLibrary("androidx-test-junit").get())
        }
        jsTest {
            resources.srcDir(unpackSkikoWasmRuntime)
        }
        wasmJsTest {
            resources.srcDir(unpackSkikoWasmRuntime)
        }
    }
}
