package dev.nyon.klf

//? if lp: <=2.0 {
/*import dev.nyon.klf.mv.Mod
import dev.nyon.klf.mv.ModFileScanData
import java.util.function.Supplier
import java.util.function.Consumer

typealias IModLanguageProvider = /^? if forge {^/ /^net.minecraftforge.forgespi.language.IModLanguageProvider ^//^?} else {^/ net.neoforged.neoforgespi.language.IModLanguageProvider /^?}^/

class KotlinLanguageProvider : IModLanguageProvider {
    override fun name(): String {
        return "klf"
    }

    override fun getFileVisitor(): Consumer<ModFileScanData> {
        return Consumer { scanData ->
            val filtered = scanData.annotations.filter { it.annotationType.className == Mod::class.qualifiedName }
            val loaders = filtered.associate { annotationData ->
                val id = annotationData.annotationData["value"] as String

                id to KotlinLanguageLoader()
            }

            scanData.addLanguageLoader(loaders)
        }
    }

    //? if forge
    /^override fun <R : net.minecraftforge.forgespi.language.ILifecycleEvent<R>> consumeLifecycleEvent(consumeEvent: Supplier<R>) {}^/
}
*///?}