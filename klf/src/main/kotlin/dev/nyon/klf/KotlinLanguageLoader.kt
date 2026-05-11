package dev.nyon.klf

import dev.nyon.klf.mv.IModInfo
import dev.nyon.klf.mv.IModLanguageLoader
import dev.nyon.klf.mv.Mod
import dev.nyon.klf.mv.ModFileScanData

//? if lp: >=3.0 {
import dev.nyon.klf.mv.ModContainer
import dev.nyon.klf.mv.dist
import net.neoforged.fml.ModLoadingIssue
import net.neoforged.neoforgespi.IIssueReporting
import net.neoforged.neoforgespi.locating.IModFile
import java.lang.annotation.ElementType
//?}

class KotlinLanguageLoader : IModLanguageLoader {
    private val versionCompatibilityPipetteValues = mapOf(
        //? if lp: !=3.1
        //"net.neoforged.fml.loading.FMLLoader" to "getCurrent",
        //? if lp: !=3.0
        "net.neoforged.fml.loading.FMLLoader" to "versionInfo",
        //? if lp: !=2.0
        "net.neoforged.fml.Bindings" to "getForgeBus"
    )

    /**
     * Checks for methods in the classpath that are not supposed to be there when running a specific version. This way
     * it can determine whether the language provider version is correct or not. If there are files that correspond to
     * another version, an error will be thrown, terminating the startup of the game.
     */
    private fun verifyCompatibility() {
        val isValid = versionCompatibilityPipetteValues.none { (className, method) ->
            try {
                val clazz = Class.forName(className)
                clazz.getMethod(method).invoke(null)
            } catch (_: Exception) {
                return@none false
            }
            return@none true
        }
        if (!isValid) error("You are using a version of KotlinLangForge that is not compatible with this version of " +
            "Minecraft. Check the description of the mod for correct versions. If you believe this is a mistake, please " +
            "contact the developer.")
    }

    //? if lp: >=3.0 {
    override fun name(): String {
        return "klf"
    }

    override fun version(): String {
        return this.javaClass.`package`.implementationVersion
    }

    override fun loadMod(
        info: IModInfo, scanResults: ModFileScanData, layer: ModuleLayer
    ): ModContainer {
        verifyCompatibility()
        val modClasses = scanResults.getAnnotatedBy(Mod::class.java, ElementType.TYPE)
            .filter { data -> data.annotationData["value"] == info.modId }
            .filter { data -> AutomaticEventSubscriber.getSides(data.annotationData["dist"]).contains(dist) }
            .sorted(Comparator.comparingInt { data -> -AutomaticEventSubscriber.getSides(data.annotationData["dist"]).size })
            .map { data -> data.clazz.className }
            .toList()

        return KotlinModContainer(info, modClasses, layer, scanResults)
    }

    override fun validate(file: IModFile, loadedContainers: Collection<ModContainer?>, reporter: IIssueReporting) {
        val modIds = file.modInfos.mapNotNull { if (it.loader == this) it.modId else null }

        file.scanResult.getAnnotatedBy(Mod::class.java, ElementType.TYPE)
            .filter { !modIds.contains(it.annotationData["value"]) }
            .forEach { data ->
                val modId = data.annotationData["value"]
                val entrypointClass = data.clazz.className
                val issue = ModLoadingIssue.error("fml.modloadingissue.javafml.dangling_entrypoint", modId, entrypointClass, file.filePath).withAffectedModFile(file)
                reporter.addIssue(issue)
            }
    }

    //?} else {
    /*@Suppress("UNCHECKED_CAST")
    override fun <T : Any> loadMod(
        info: IModInfo, scanResults: ModFileScanData, layer: ModuleLayer
    ): T {
        verifyCompatibility()
        val modClasses = scanResults.annotations
            .filter { it.annotationType.className == Mod::class.qualifiedName && it.annotationData["value"] == info.modId }
            .map { it.clazz.className }
        return KotlinModContainer(info, modClasses, layer, scanResults) as T
    }
     *///?}
}
