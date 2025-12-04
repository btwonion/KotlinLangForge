@file:Suppress("SpellCheckingInspection")

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.architectury.loom)
}

base {
    archivesName.set(rootProject.name + "-kff-compat")
}

repositories {
    mavenCentral()
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
    minecraft("com.mojang:minecraft:1.20.1")
    mappings(loom.officialMojangMappings())

    "forge"("net.minecraftforge:forge:1.20.1-47.4.4")
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