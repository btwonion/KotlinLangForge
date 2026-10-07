package dev.nyon.klf

import dev.nyon.klf.mv.FMLLoader
import org.apache.logging.log4j.LogManager
import java.lang.reflect.Modifier
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Properties

internal data class KlfBuildInfo(
    val compatibilityLine: String,
    val supportedMinecraftVersions: Set<String>,
    val artifactVersion: String? = null
)

internal object MinecraftVersionCompatibility {
    private const val BUILD_INFO_RESOURCE = "/META-INF/klf-build.properties"

    private val logger = LogManager.getLogger()
    private val buildInfo by lazy(::loadBuildInfo)

    fun findIncompatibility(): IllegalStateException? {
        val minecraftVersion = try {
            findMinecraftVersion()
        } catch (exception: Exception) {
            logger.warn("Could not determine the current Minecraft version; skipping the KotlinLangForge compatibility check.", exception)
            return null
        } catch (exception: LinkageError) {
            logger.warn("Could not access FML version information; skipping the KotlinLangForge compatibility check.", exception)
            return null
        }

        val loader = when {
            FMLLoader::class.java.name.startsWith("net.neoforged.") -> "neoforge"
            FMLLoader::class.java.name.startsWith("net.minecraftforge.") -> "forge"
            else -> null
        }
        val artifactFilename = runCatching {
            findArtifactFilename(KotlinLanguageLoader::class.java.protectionDomain.codeSource?.location?.toURI())
        }.getOrNull()
        return findIncompatibility(
            minecraftVersion, buildInfo,
            buildInfo.artifactVersion ?: KotlinLanguageLoader::class.java.`package`.implementationVersion,
            loader, artifactFilename
        )
    }

    internal fun findArtifactFilename(location: URI?): String? = runCatching {
        if (location == null) return@runCatching null
        val sourcePath = Path.of(location)
        val artifactPath = if (location.scheme == "file") sourcePath else {
            // SecureJarHandler uses a union filesystem. Query its backing path without linking to its API,
            // whose package/version may differ when this early compatibility check runs.
            sourcePath.fileSystem.javaClass.getMethod("getPrimaryPath").invoke(sourcePath.fileSystem) as? Path
        }
        artifactPath?.fileName?.toString()?.takeIf { it.endsWith(".jar") }
    }.getOrNull()

    /**
     * This check intentionally runs on incompatible FML versions. Therefore neither version-info accessor can be
     * referenced directly: FML may resolve that reference while linking this class and throw a NoSuchMethodError
     * before Kotlin's try/catch is entered.
     */
    internal fun findMinecraftVersion(loaderClass: Class<*> = FMLLoader::class.java): String {
        val legacyVersionInfoMethod = loaderClass.methods.firstOrNull { method ->
            method.name == "versionInfo" && method.parameterCount == 0 && Modifier.isStatic(method.modifiers)
        }

        val versionInfo = if (legacyVersionInfoMethod != null) {
            legacyVersionInfoMethod.invoke(null)
        } else {
            val loader = loaderClass.getMethod("getCurrent").invoke(null)
            loader.javaClass.getMethod("getVersionInfo").invoke(loader)
        } ?: error("FML returned no version information")

        return versionInfo.javaClass.getMethod("mcVersion").invoke(versionInfo) as? String
            ?: error("FML returned no Minecraft version")
    }

    internal fun findIncompatibility(
        minecraftVersion: String,
        buildInfo: KlfBuildInfo,
        klfVersion: String?,
        loader: String? = null,
        artifactFilename: String? = null
    ): IllegalStateException? {
        if (minecraftVersion in buildInfo.supportedMinecraftVersions) return null

        val displayedKlfVersion = klfVersion?.let { " $it" }.orEmpty()
        val installedArtifact = artifactFilename?.let { " Installed artifact: $it." }.orEmpty()
        val displayedLoader = loader?.let { " on $it" }.orEmpty()
        val supportedVersions = buildInfo.supportedMinecraftVersions.joinToString(", ")
        val requiredLine = when (loader) {
            "forge" -> when (minecraftVersion) {
                "1.16.5" -> "1.0"
                in FORGE_2_MINECRAFT_VERSIONS -> "2.0"
                else -> null
            }
            "neoforge" -> when (minecraftVersion) {
                in NEOFORGE_2_MINECRAFT_VERSIONS -> "2.0"
                in NEOFORGE_3_MINECRAFT_VERSIONS -> "3.0"
                in NEOFORGE_31_MINECRAFT_VERSIONS -> "3.1"
                else -> null
            }
            else -> null
        }
        val lineGuidance = requiredLine?.let { " Choose compatibility line $it for $loader." }.orEmpty()
        val encodedMinecraftVersion = URLEncoder.encode(minecraftVersion, StandardCharsets.UTF_8)
        val downloadLink = "https://modrinth.com/mod/kotlin-lang-forge/versions?g=$encodedMinecraftVersion" +
            (loader?.let { "&l=${URLEncoder.encode(it, StandardCharsets.UTF_8)}" } ?: "")
        return IllegalStateException(
            "KotlinLangForge$displayedKlfVersion (compatibility line ${buildInfo.compatibilityLine}) supports " +
                "Minecraft $supportedVersions, but Minecraft $minecraftVersion$displayedLoader is running." +
                installedArtifact + " Remove this KotlinLangForge artifact from the mods folder and install one " +
                "listed for Minecraft $minecraftVersion${loader?.let { " and $it" }.orEmpty()}." +
                lineGuidance + " Downloads: $downloadLink . If no matching file is listed, this Minecraft/loader " +
                "combination is not supported; use a listed combination."
        )
    }

    // These are published compatibility lines, not numeric Minecraft-version ranges (26.x is not 1.x).
    private val FORGE_2_MINECRAFT_VERSIONS = setOf("1.17.1", "1.18.2", "1.19.2", "1.19.4", "1.20.1", "1.20.2", "1.20.3", "1.20.4")
    private val NEOFORGE_2_MINECRAFT_VERSIONS = setOf("1.20.2", "1.20.3", "1.20.4")
    private val NEOFORGE_3_MINECRAFT_VERSIONS = setOf("1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8")
    private val NEOFORGE_31_MINECRAFT_VERSIONS = setOf("1.21.9", "1.21.10", "1.21.11", "26.1", "26.1.1", "26.1.2", "26.2", "26.3")

    private fun loadBuildInfo(): KlfBuildInfo {
        val properties = Properties()
        val resource = MinecraftVersionCompatibility::class.java.getResourceAsStream(BUILD_INFO_RESOURCE)
            ?: error("KotlinLangForge build information is missing")
        resource.use(properties::load)

        val compatibilityLine = properties.getProperty("compatibilityLine")
            ?.takeIf(String::isNotBlank)
            ?: error("KotlinLangForge compatibility line is missing")
        val supportedMinecraftVersions = properties.getProperty("supportedMinecraftVersions")
            ?.split(',')
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            ?.toSet()
            .orEmpty()
        check(supportedMinecraftVersions.isNotEmpty()) { "KotlinLangForge supported Minecraft versions are missing" }
        return KlfBuildInfo(compatibilityLine, supportedMinecraftVersions, properties.getProperty("artifactVersion")?.takeIf(String::isNotBlank))
    }
}
