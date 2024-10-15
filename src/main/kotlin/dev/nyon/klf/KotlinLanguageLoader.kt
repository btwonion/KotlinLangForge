package dev.nyon.klf

/*? if neoforge {*/
import net.neoforged.fml.common.Mod
import net.neoforged.neoforgespi.language.IModInfo
import net.neoforged.neoforgespi.language.ModFileScanData
/*? if >1.20.4 {*/
import net.neoforged.fml.ModContainer
import net.neoforged.neoforgespi.language.IModLanguageLoader
/*?} else {*/
/*import net.neoforged.neoforgespi.language.IModLanguageProvider.IModLanguageLoader
*//*?}*/
/*?} else {*/
/*import net.minecraftforge.fml.common.Mod
import net.minecraftforge.forgespi.language.IModInfo
import net.minecraftforge.forgespi.language.IModLanguageProvider.IModLanguageLoader
import net.minecraftforge.forgespi.language.ModFileScanData
*//*?}*/

typealias ModAnnotation = Mod

class KotlinLanguageLoader : IModLanguageLoader {
    /*? if neoforge && >1.20.4 {*/
    override fun name(): String {
        return "klf"
    }

    override fun version(): String {
        return this.javaClass.`package`.implementationVersion
    }

    override fun loadMod(
        info: IModInfo, scanResults: ModFileScanData, layer: ModuleLayer
    ): ModContainer {
        val modClassName = scanResults.annotations.find { it.annotationType.className == ModAnnotation::class.qualifiedName && it.annotationData["value"] == info.modId }?.annotatedClassName
        val modClass = Class.forName(layer.findModule(info.owningFile.moduleName()).orElseThrow(), modClassName)
        return KotlinModContainer(info, modClass)
    }
    /*?} else {*/
    /*@Suppress("UNCHECKED_CAST")
    override fun <T : Any> loadMod(
        info: IModInfo /^? if <=1.16.5 {^//^, modClassLoader: ClassLoader^//^?}^/, scanResults: ModFileScanData /^? if >1.16.5 {^/, layer: ModuleLayer/^?}^/
    ): T {
        val modClassName = scanResults.annotations.find { it.annotationType.className == ModAnnotation::class.qualifiedName && it.annotationData["value"] == info.modId }?.annotatedClassName
        val modClass = Class.forName(modClassName)
        return KotlinModContainer(info, modClass) as T
    }
*//*?}*/
}

val ModFileScanData.AnnotationData.annotatedClassName
    get() = /*? if >1.16.5 {*/ clazz.className /*?} else {*/ /*classType.className *//*?}*/