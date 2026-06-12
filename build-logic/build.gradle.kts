plugins { `kotlin-dsl` }

dependencies {
    implementation(libs.build.spotless)
    implementation("com.android.tools.build:gradle:${libs.versions.agp.get()}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "foundation.e.bliss.android-library"
            implementationClass = "foundation.e.bliss.AndroidLibraryConventionPlugin"
        }
        register("spotless") {
            id = "foundation.e.bliss.spotless"
            implementationClass = "foundation.e.bliss.SpotlessPlugin"
        }
        register("githooks") {
            id = "foundation.e.bliss.githooks"
            implementationClass = "foundation.e.bliss.GitHooksPlugin"
        }
    }
}
