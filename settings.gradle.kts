pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "coil3-decoders"

include(
    ":coil-ppm",
    ":coil-pgm",
    ":coil-pam",
    ":coil-heif",
)
