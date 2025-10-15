package dev.nyon.klf

import dev.nyon.klf.mv.IModInfo
import dev.nyon.klf.mv.IModLanguageLoader
import dev.nyon.klf.mv.Mod
import dev.nyon.klf.mv.ModContainer
import dev.nyon.klf.mv.ModFileScanData
import dev.nyon.klf.mv.dist

//? if lp: >=3.0 {
import net.neoforged.fml.ModLoadingIssue
import net.neoforged.neoforgespi.IIssueReporting
import net.neoforged.neoforgespi.locating.IModFile
import java.lang.annotation.ElementType
//?}

class KotlinLanguageLoader : IModLanguageLoader {
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
        val modClasses = scanResults.annotations
            .filter { it.annotationType.className == Mod::class.qualifiedName && it.annotationData["value"] == info.modId }
            .map { it.clazz.className }
        return KotlinModContainer(info, modClasses, layer, scanResults) as T
    }
    *///?}
}
