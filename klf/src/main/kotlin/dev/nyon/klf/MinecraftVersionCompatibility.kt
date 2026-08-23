package dev.nyon.klf

import dev.nyon.klf.mv.FMLLoader
import org.apache.logging.log4j.LogManager
import java.lang.reflect.Modifier
import java.util.Properties

internal data class KlfBuildInfo(
    val compatibilityLine: String,
    val supportedMinecraftVersions: Set<String>
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

        return findIncompatibility(minecraftVersion, buildInfo, KotlinLanguageLoader::class.java.`package`.implementationVersion)
    }

    /**
     * This check intentionally runs on incompatible FML versions. Therefore neither version-info accessor can be
     * referenced directly: FML may resolve that reference while linking this class and throw a NoSuchMethodError
     * before Kotlin's try/catch is entered.
     */
    private fun findMinecraftVersion(): String {
        val loaderClass = FMLLoader::class.java
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
        klfVersion: String?
    ): IllegalStateException? {
        if (minecraftVersion in buildInfo.supportedMinecraftVersions) return null

        val displayedKlfVersion = klfVersion?.let { "$it (${buildInfo.compatibilityLine})" }
            ?: buildInfo.compatibilityLine
        val supportedVersions = buildInfo.supportedMinecraftVersions.joinToString(", ")
        return IllegalStateException(
            "KotlinLangForge $displayedKlfVersion supports Minecraft $supportedVersions, but Minecraft " +
                "$minecraftVersion is running. Install a compatible KotlinLangForge artifact."
        )
    }

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
        return KlfBuildInfo(compatibilityLine, supportedMinecraftVersions)
    }
}
