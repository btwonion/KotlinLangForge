package dev.nyon.klf

//? if neoforge {
import net.neoforged.fml.common.Mod
import net.neoforged.neoforgespi.language.IModInfo
import net.neoforged.neoforgespi.language.ModFileScanData
//? if lp: >=3.0 {
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModLoadingIssue
import net.neoforged.neoforgespi.IIssueReporting
import net.neoforged.neoforgespi.language.IModLanguageLoader
import net.neoforged.neoforgespi.locating.IModFile
import java.lang.annotation.ElementType
//?} else {
/*import net.neoforged.neoforgespi.language.IModLanguageProvider.IModLanguageLoader
*//*?}*/
//?} else {
/*import net.minecraftforge.fml.common.Mod
import net.minecraftforge.forgespi.language.IModInfo
import net.minecraftforge.forgespi.language.IModLanguageProvider.IModLanguageLoader
import net.minecraftforge.forgespi.language.ModFileScanData
*/
//?}

typealias ModAnnotation = Mod

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
        val modClasses = scanResults.getAnnotatedBy(ModAnnotation::class.java, ElementType.TYPE)
            .filter { data -> data.annotationData["value"] == info.modId }
            .map { data -> data.clazz.className }
            .toList()

        return KotlinModContainer(info, modClasses, layer)
    }

    override fun validate(file: IModFile, loadedContainers: Collection<ModContainer?>, reporter: IIssueReporting) {
        val modIds = file.modInfos.mapNotNull { if (it.loader == this) it.modId else null }

        file.scanResult.getAnnotatedBy(ModAnnotation::class.java, ElementType.TYPE)
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
            .filter { it.annotationType.className == ModAnnotation::class.qualifiedName && it.annotationData["value"] == info.modId }
            .map { it.clazz.className }
        return KotlinModContainer(info, modClasses, layer) as T
    }
    */
    //?}
}
