rootProject.name = "KotlinLangForge"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.architectury.dev")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.kikugie.dev/snapshots")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7.11"
}

buildscript {
    repositories { mavenCentral() }
    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    }
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    listOf("klf", "testmod").forEach { namespace ->
        create(namespace) {
            version("2.0-forge", "1.18.2")
            version("2.0-neoforge", "1.20.4")
            version("3.0-neoforge", "1.20.6")
            version("3.1-neoforge", "1.21.9")

            vcsVersion = "3.0-neoforge"
        }
    }

    create("kff-compat") {
        version("2.0-forge", "1.20.1")
        version("2.0-neoforge", "1.20.1")

        vcsVersion = "2.0-neoforge"
    }
}
