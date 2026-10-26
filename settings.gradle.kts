pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "VhsCam"

// Pure Kotlin logic (look presets, audio DSP, orientation math, date stamps). It is a separate
// build so it can be compiled and tested without the Android SDK: ./gradlew -p core test
includeBuild("core")
include(":app")
