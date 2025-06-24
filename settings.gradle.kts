pluginManagement {
    includeBuild("build-logic")

    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://gitlab.e.foundation/api/v4/projects/1391/packages/maven")
        maven(url = "https://gitlab.e.foundation/api/v4/projects/1272/packages/maven")
    }
}

include(":animationlib")
project(":animationlib").projectDir = File(rootDir, "libs_systemui/animationlib")

include(":IconLoader")
project(":IconLoader").projectDir = File(rootDir, "libs_systemui/iconloaderlib")

include(":SearchUiLib")
project(":SearchUiLib").projectDir = File(rootDir, "libs_systemui/searchuilib")

include(":msdllib")
project(":msdllib").projectDir = File(rootDir, "libs_systemui/msdllib")

include(":contextualeducationlib")
project(":contextualeducationlib").projectDir = File(rootDir, "libs_systemui/contextualeducationlib")

rootProject.name = "BlissLauncher3"
