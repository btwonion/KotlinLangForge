@file:Suppress("SpellCheckingInspection", "UnstableApiUsage", "RedundantNullableReturnType")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import net.fabricmc.loom.util.ModPlatform
import org.apache.tools.zip.ZipOutputStream
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.architectury.loom)
    alias(libs.plugins.mod.publish)
    alias(libs.plugins.shadow)

    `maven-publish`
}

val loader = loom.platform.get()

val beta: Int = property("mod.beta").toString().toInt()
val majorVersion: String = property("mod.major-version").toString()
val mcVersion = property("vers.mcVersion").toString() // Pattern is '1.0.0-beta1-k2.0.20-1.20.6-pre.2+fabric'
val kotlinVersion = libs.versions.kotlin.orNull
version = "$majorVersion${if (beta != 0) "-beta$beta" else ""}-k$kotlinVersion-$mcVersion+${loader.name.lowercase()}"

group = property("mod.group").toString()
val githubRepo = property("mod.repo").toString()

base {
    archivesName.set(rootProject.name)
}

loom {
    if (stonecutter.current.isActive) {
        runConfigs.all {
            ideConfigGenerated(true)
            runDir("../../run")
        }
    }

    mixin { useLegacyMixinAp = false }
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
    mappings(loom.layered {
        val quiltMappings: String = property("vers.deps.quiltmappings").toString()
        if (quiltMappings.isNotEmpty()) mappings("org.quiltmc:quilt-mappings:$quiltMappings:intermediary-v2")
        officialMojangMappings()
    })

    if (loader == ModPlatform.FORGE) "forge"("net.minecraftforge:forge:$mcVersion-${property("vers.deps.fml")}")
    else "neoForge"("net.neoforged:neoforge:${property("vers.deps.fml")}")

    listOf(
        "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion",
        "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion",
        "org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3",
        "org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3",
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0",
        "org.jetbrains.kotlinx:kotlinx-datetime:0.6.1",
        "org.jetbrains.kotlinx:kotlinx-io-core:0.5.4",
        "org.jetbrains.kotlinx:kotlinx-io-bytestring:0.5.4",
        "org.jetbrains.kotlinx:atomicfu:0.25.0"
    ).forEach {
        if (stonecutter.eval(mcVersion, ">=1.20.6")) include(it)
        else implementation(it)
    }
}

val javaVersion =
    if (stonecutter.eval(mcVersion, ">1.20.4")) 21 else if (stonecutter.eval(mcVersion, ">1.16.5")) 17 else 8
val modName = property("mod.name").toString()
val modId = property("mod.id").toString()
tasks {
    register("releaseMod") {
        group = "publishing"

        dependsOn("publishMods")
        dependsOn("publish")
    }

    withType<JavaCompile> {
        options.release = javaVersion
    }

    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(javaVersion.toString().let { if (it == "8") "1.8" else it })
        }
    }

    withType<Jar> {
        manifest.attributes(
            "Manifest-Version" to "1.0",
            "FMLModType" to if (stonecutter.eval(mcVersion, "<=1.20.4")) "LANGPROVIDER" else "LIBRARY",
            "Automatic-Module-Name" to modId,
            "Implementation-Version" to project.version
        )
    }

    withType<ShadowJar> {
        archiveClassifier = "shadow"

        dependencies {
            include {
                it.moduleGroup == "org.jetbrains.kotlin" || it.moduleGroup == "org.jetbrains.kotlinx"
            }
        }

        class DontIncludeMcFilesTransformer : com.github.jengelman.gradle.plugins.shadow.transformers.Transformer {
            @Input
            @Optional
            val invalidEndings = listOf(
                ".nbt",
                ".json",
                ".mcmeta",
                ".mcassetsroot",
                ".tiny",
                ".bin",
                ".jfc",
                ".png",
                ".fsh",
                ".vsh",
                ".glsl",
                ".txt",
                ".pro"
            )

            @Input
            @Optional
            val validEndings = listOf("klf/icon.png")

            override fun canTransformResource(element: FileTreeElement?): Boolean {
                val path = element?.relativePath?.pathString ?: return false
                if (validEndings.any { path.endsWith(it) }) return false
                return invalidEndings.any { path.endsWith(it) }
            }

            override fun transform(context: TransformerContext?) {}

            override fun hasTransformedResource(): Boolean {
                return false
            }

            override fun modifyOutputStream(
                os: ZipOutputStream?, preserveFileTimestamps: Boolean
            ) {
            }

            override fun getName(): String {
                return "DontIncludeMcFilesTransformer"
            }
        }

        transform(DontIncludeMcFilesTransformer::class.java)
    }

    remapJar {
        if (stonecutter.eval(mcVersion, "<=1.20.4")) {
            dependsOn("shadowJar")
            val shadowJar = shadowJar.get()
            inputFile.set(shadowJar.archiveFile)
        }
    }

    build {
        if (stonecutter.eval(mcVersion, "<=1.20.4")) dependsOn("shadowJar")
    }
}

val changelogText = buildString {
    append("# v${project.version}\n")
    if (beta != 0) appendLine("### As this is still a beta version, this version can contain bugs. Feel free to report ANY misbehaviours and errors!")
    rootDir.resolve("changelog.md").readText().also(::append)
}

val supportedMcVersions: List<String> =
    property("vers.supportedMcVersions")!!.toString().split(',').map(String::trim).filter(String::isNotEmpty)

publishMods {
    displayName = "v${project.version}"
    file = tasks.remapJar.get().archiveFile
    changelog = changelogText
    type = if (beta != 0) BETA else STABLE
    when (loader) {
        ModPlatform.FORGE -> modLoaders.addAll("forge")
        ModPlatform.NEOFORGE -> modLoaders.addAll("neoforge")
        else -> {}
    }

    modrinth {
        projectId = "1vrSzlao"
        accessToken = providers.environmentVariable("MODRINTH_API_KEY")
        minecraftVersions.addAll(supportedMcVersions)
    }

    github {
        repository = githubRepo
        accessToken = providers.environmentVariable("GITHUB_TOKEN")
        commitish = property("mod.main-branch").toString()
    }
}

publishing {
    repositories {
        maven {
            name = "nyon"
            url = uri("https://repo.nyon.dev/releases")
            credentials {
                username = providers.environmentVariable("NYON_USERNAME").orNull
                password = providers.environmentVariable("NYON_PASSWORD").orNull
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "dev.nyon"
            artifactId = modName
            version = project.version.toString()
            from(components["java"])
        }
    }
}

java {
    withSourcesJar()

    val gradleJavaVersion = JavaVersion.toVersion(javaVersion)
    sourceCompatibility = gradleJavaVersion
    targetCompatibility = gradleJavaVersion
}