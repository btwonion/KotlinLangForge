@file:Suppress("SpellCheckingInspection", "UnstableApiUsage", "RedundantNullableReturnType")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import net.fabricmc.loom.util.ModPlatform
import org.apache.tools.zip.ZipOutputStream
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
val mcVersion = property("vers.mcVersion").toString() // Pattern is '2.0.0-beta1-k2.0.20-2.0+forge'
val lPVersion = stonecutter.current.project.split("-")[0]
val kotlinVersion = libs.versions.kotlin.orNull
version = "$majorVersion${if (beta != 0) "-beta$beta" else ""}-k$kotlinVersion-$lPVersion+${loader.name.lowercase()}"

group = property("mod.group").toString()
val githubRepo = property("mod.repo").toString()

base {
    archivesName.set(rootProject.name)
}

stonecutter {
    listOf("forge", "neoforge").map { it to (loader.name.lowercase() == it) }
        .forEach { (name, isCurrent) -> const(name, isCurrent) }
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

val apiAndShadow: Configuration by configurations.creating {
    extendsFrom(configurations.api.get())
    extendsFrom(configurations.shadow.get())

    exclude("org.jetbrains", "annotations")
    exclude("org.intellij", "lang")
}

val inclusions = listOf(
    "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion",
    "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion",
    "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion",
    "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion",
    "org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0",
    "org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0",
    "org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.9.0",
    "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2",
    "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.10.2",
    "org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-0.6.x-compat",
    "org.jetbrains.kotlinx:kotlinx-io-core:0.8.0",
    "org.jetbrains.kotlinx:kotlinx-io-bytestring:0.8.0",
    "org.jetbrains.kotlinx:atomicfu:0.29.0"
)

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.officialMojangMappings())

    if (loader == ModPlatform.FORGE) "forge"("net.minecraftforge:forge:$mcVersion-${property("vers.deps.fml")}")
    else "neoForge"("net.neoforged:neoforge:${property("vers.deps.fml")}")

    inclusions.forEach {
        if (listOf("1.0", "2.0").contains(lPVersion)) apiAndShadow(it)
        else api(include(it)!!)
    }
}

val javaVersion =
    if (stonecutter.eval(mcVersion, ">=1.20.6")) 21 else if (stonecutter.eval(mcVersion, ">1.16.5")) 17 else 8
val modName = property("mod.name").toString()
val modId = property("mod.id").toString()
val modDescription = property("mod.description").toString()
val icon = property("mod.icon").toString()
val slug = property("mod.slug").toString()
val mcVersionRange = property("vers.mcVersionRange").toString()
tasks {
    register("releaseMod") {
        group = "publishing"

        dependsOn("publishMods")
        dependsOn("publish")
    }

    register("processReadMeTemplate") {
        group = "publishing"

        val templateText = rootProject.file("README-template.md").readText()
        val inclusionsReplacement = inclusions.joinToString("\n- ", prefix = "- ")
        rootProject.file("README.md").writeText(templateText.replace("{inclusions}", inclusionsReplacement))
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
            "FMLModType" to if (listOf("1.0", "2.0").contains(lPVersion)) "LANGPROVIDER" else "LIBRARY",
            "Automatic-Module-Name" to modId,
            "Implementation-Version" to majorVersion
        )
    }

    processResources {
        val props: Map<String, String?> = mapOf(
            "id" to modId,
            "name" to modName,
            "description" to modDescription,
            "version" to project.version.toString(),
            "repo" to githubRepo,
            "icon" to icon,
            "slug" to slug,
            "mc" to mcVersionRange
        )

        props.forEach(inputs::property)

        filesMatching(listOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml")) { expand(props) }
        exclude(if (loader == ModPlatform.NEOFORGE) "META-INF/mods.toml" else "META-INF/neoforge.mods.toml")
    }

    withType<ShadowJar> {
        archiveClassifier = "shadow"
        configurations = listOf(apiAndShadow)

        class DontIncludeMcFilesTransformer :
            com.github.jengelman.gradle.plugins.shadow.transformers.ResourceTransformer {
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
            val exceptions = listOf("klf/icon.png")

            override fun canTransformResource(element: FileTreeElement): Boolean {
                val path = element.relativePath.pathString
                if (exceptions.any { path.endsWith(it) }) return false
                return invalidEndings.any { path.endsWith(it) }
            }

            override fun transform(context: TransformerContext) {}

            override fun hasTransformedResource(): Boolean {
                return false
            }

            override fun modifyOutputStream(os: ZipOutputStream, preserveFileTimestamps: Boolean) {}
        }

        transform(DontIncludeMcFilesTransformer())
    }

    remapJar {
        if (listOf("1.0", "2.0").contains(lPVersion)) {
            dependsOn("shadowJar")
            val shadowJar = shadowJar.get()
            inputFile.set(shadowJar.archiveFile)
        }
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

    curseforge {
        projectId = "1244682"
        accessToken = providers.environmentVariable("CURSEFORGE_API_KEY")
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
            url = if (beta != 0) uri("https://repo.nyon.dev/snapshots") else uri("https://repo.nyon.dev/releases")
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