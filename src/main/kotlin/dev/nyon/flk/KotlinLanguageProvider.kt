package dev.nyon.flk

/*? if <=1.20.4 {*/
/*? if forge {*/
/*import net.minecraftforge.forgespi.language.ILifecycleEvent
import net.minecraftforge.forgespi.language.IModLanguageProvider
import net.minecraftforge.forgespi.language.ModFileScanData
import java.util.function.Supplier
*//*?} else {*/
import net.neoforged.neoforgespi.language.IModLanguageProvider
import net.neoforged.neoforgespi.language.ModFileScanData
/*?}*/
import java.util.function.Consumer

class KotlinLanguageProvider : IModLanguageProvider {
    override fun name(): String {
        return "flk"
    }

    override fun getFileVisitor(): Consumer<ModFileScanData> {
        return Consumer { scanData ->
            val filtered = scanData.annotations.filter { it.annotationClassName == ModAnnotation::class.qualifiedName }
            val loaders = filtered.associate { annotationData ->
                val id = annotationData.annotationData["value"] as String

                id to KotlinLanguageLoader()
            }

            scanData.addLanguageLoader(loaders)
        }
    }

    //? if forge
    /*override fun <R : ILifecycleEvent<R>> consumeLifecycleEvent(consumeEvent: Supplier<R>) {}*/
}/*?}*/