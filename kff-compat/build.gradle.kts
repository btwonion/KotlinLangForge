@file:Suppress("SpellCheckingInspection")

import net.fabricmc.loom.util.ModPlatform

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.architectury.loom)
}

val loader = loom.platform.get()

val beta: Int = property("mod.beta").toString().toInt()
val majorVersion: String = property("mod.major-version").toString()
val mcVersion = property("vers.mcVersion").toString() // Pattern is '2.0.0-beta1-k2.0.20-2.0+forge'
val lPVersion = stonecutter.current.project.split("-")[0]
val kotlinVersion = libs.versions.kotlin.orNull
version = "$majorVersion${if (beta != 0) "-beta$beta" else ""}-k$kotlinVersion-$lPVersion+${loader.name.lowercase()}"

base {
    archivesName.set("${rootProject.name}-kff-compat")
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

    include(project(":klf:${project.name.split(":").last()}"))
    include(implementation("net.lenni0451:Reflect:1.5.0")!!)
    implementation("maven.modrinth:preloading-tricks:3.6.0")
}

// These checks have a main entry point and do not require a separate test framework.
val compatibilityChecks = sourceSets.create("compatibilityTest") {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().runtimeClasspath
}

tasks {
    val compatibilityTest = register<JavaExec>("compatibilityTest") {
        group = "verification"
        description = "Checks early KLF/KFF metadata patching and missing Preloading Tricks diagnostics."
        dependsOn(compatibilityChecks.classesTaskName)
        classpath = compatibilityChecks.runtimeClasspath
        workingDir = layout.buildDirectory.get().asFile
        mainClass = "dev.nyon.klf.compat.kff.CompatibilityTest"
        // SecureJar's union filesystem needs the same access supplied by the Forge launcher.
        jvmArgs("--add-opens=java.base/java.lang.invoke=ALL-UNNAMED")
    }

    check { dependsOn(compatibilityTest) }

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
