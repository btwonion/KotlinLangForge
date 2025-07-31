import net.fabricmc.loom.util.ModPlatform
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.architectury.loom)
}

val loader = loom.platform.get()
val mcVersion = property("vers.mcVersion").toString()

loom {
    if (stonecutter.current.isActive) {
        runConfigs.all {
            ideConfigGenerated(true)
            runDir("../../run")
        }
    }

    silentMojangMappingsLicense()
}

repositories {
    mavenCentral()
    maven("https://maven.quiltmc.org/repository/release/")
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.minecraftforge.net/")
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.officialMojangMappings())

    if (loader == ModPlatform.FORGE) "forge"("net.minecraftforge:forge:$mcVersion-${property("vers.deps.fml")}")
    else "neoForge"("net.neoforged:neoforge:${property("vers.deps.fml")}")

    implementation(include(project(":klf:${project.name}"))!!)
}

val javaVersion =
    if (stonecutter.eval(mcVersion, ">=1.20.6")) 21 else if (stonecutter.eval(mcVersion, ">1.16.5")) 17 else 8

tasks {
    processResources {
        exclude(if (loader == ModPlatform.NEOFORGE) "META-INF/mods.toml" else "META-INF/neoforge.mods.toml")
    }

    withType<JavaCompile> {
        options.release = javaVersion
    }

    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(javaVersion.toString().let { if (it == "8") "1.8" else it })
        }
    }
}