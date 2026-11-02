plugins {
    id("coil-multiplatform-library")
}

kotlin {
    sourceSets {
        // Android device tests don't include commonTest, so share the HEIC fixtures with them separately.
        commonTest {
            kotlin.srcDir("src/sharedTest/kotlin")
        }
        getByName("androidDeviceTest") {
            kotlin.srcDir("src/sharedTest/kotlin")
        }
    }
}
