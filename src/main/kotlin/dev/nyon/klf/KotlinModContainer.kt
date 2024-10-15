package dev.nyon.klf

import java.util.function.Supplier
/*? if neoforge && >1.20.4 {*/
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModLoadingException
import net.neoforged.fml.ModLoadingIssue
import net.neoforged.neoforgespi.language.IModInfo
/*?} else {*/
/*/^? if forge {^/
/^import net.minecraftforge.fml.ModContainer
import net.minecraftforge.fml.ModLoadingException
import net.minecraftforge.fml.ModLoadingStage
import net.minecraftforge.forgespi.language.IModInfo
^//^?} else {^/
import net.neoforged.bus.EventBusErrorMessage
import net.neoforged.bus.api.BusBuilder
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModLoadingException
import net.neoforged.fml.ModLoadingStage
import net.neoforged.neoforgespi.language.IModInfo
/^?}^/
*//*?}*/

@Suppress("NO_REFLECTION_IN_CLASS_PATH")
class KotlinModContainer(val info: IModInfo, val modClass: Class<*>) : ModContainer(info) {
    private var instance: Any? = null

    /*? if <=1.20.4 {*/
    /*init {
        activityMap[ModLoadingStage.CONSTRUCT] = Runnable(::createMod)
    }

    override fun matches(mod: Any?): Boolean {
        return mod == instance
    }

    override fun getMod(): Any? {
        return instance
    }
    *//*?} else {*/
    override fun constructMod() {
        createMod()
    }
    /*?}*/

    init {
        try {
            val contextExtensionField = ModContainer::class.java.getDeclaredField("contextExtension")
            val legacyExtension = Supplier { KlfLoadingContext }
            contextExtensionField.set(this, legacyExtension)
        } catch (_: NoSuchFieldException) {}
    }

    /*? if neoforge {*/
    override fun getEventBus(): IEventBus? {
        return MOD_BUS
    }
    /*?}*/

    private fun createMod() {
        println("Loading ${info.modId} with KotlinLangForge.")

        // Construct instance
        try {
            // Use object or class without any constructors to construct
            instance = modClass.kotlin.objectInstance ?: modClass.getDeclaredConstructor().newInstance()
        } catch (e: Throwable) {
            throw /*? if <=1.20.4 {*/ /*ModLoadingException(info, ModLoadingStage.CONSTRUCT, "fml.modloading.failedtoloadmod", e, modClass)
            *//*?} else {*/ ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.failedtoloadmod", e)) /*?}*/
        }
    }
}