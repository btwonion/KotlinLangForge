@file:Suppress("SpellCheckingInspection")

import net.fabricmc.loom.util.ModPlatform

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.architectury.loom)
}

val loader = loom.platform.get()

base {
    archivesName.set(rootProject.name + "-kff-compat-" + project.name)
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.minecraftforge.net/")
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven")
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("vers.mcVersion")}")
    mappings(loom.officialMojangMappings())

    if (loader == ModPlatform.FORGE) "forge"("net.minecraftforge:forge:${property("vers.mcVersion")}-${property("vers.deps.fml")}")
    else "neoForge"("net.neoforged:neoforge:${property("vers.deps.fml")}")

    include(project(":klf:2.0-forge"))
    include(implementation("net.lenni0451:Reflect:1.5.0")!!)
    implementation("maven.modrinth:preloading-tricks:3.3.1")
}

tasks {
    withType<JavaCompile> {
        options.release = 17
    }

    withType<Jar> {
        manifest.attributes(
            "Manifest-Version" to "1.0",
            "FMLModType" to "LIBRARY",
            "Implementation-Version" to "1.0.0"
        )
    }
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}