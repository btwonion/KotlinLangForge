import dev.kikugie.stonecutter.StonecutterSettings

rootProject.name = "KotlinLangForge"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.architectury.dev")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.kikugie.dev/releases")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.4.+"
}

buildscript {
    repositories { mavenCentral() }
    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    }
}

extensions.configure<StonecutterSettings> {
    kotlinController = true
    centralScript = "build.gradle.kts"
    shared {
        vers("1.16.5-forge", "1.16.5")
        vers("1.18.2-forge", "1.18.2")
        vers("1.19.2-forge", "1.19.2")
        vers("1.19.4-forge", "1.19.4")
        vers("1.20.1-forge", "1.20.1")
        vers("1.20.4-neoforge", "1.20.4")
        vers("1.20.6-neoforge", "1.20.6")
        vers("1.21-neoforge", "1.21")
        vers("1.21.3-neoforge", "1.21.3")
        vcsVersion = "1.21-neoforge"
    }
    create(rootProject)
}
