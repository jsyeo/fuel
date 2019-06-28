pluginManagement {
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven(url = "https://plugins.gradle.org/m2/")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == Android.libPlugin) {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
            if (requested.id.id == KotlinX.Serialization.plugin) {
                useModule("org.jetbrains.kotlin:kotlin-serialization:${requested.version}")
            }
        }
    }
}
val projects = listOf(
    Fuel.name,
    Fuel.Forge.name,
    Fuel.Gson.name,
    Fuel.Jackson.name,
    Fuel.Json.name,
    Fuel.KotlinSerialization.name,
    Fuel.Moshi.name,
    Fuel.Stetho.name,
    Fuel.Test.name,
    Fuel.Sample.name
)

include(*(projects.toTypedArray()))
