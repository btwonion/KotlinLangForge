package dev.nyon.klf

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.Marker
import org.apache.logging.log4j.MarkerManager
import java.util.function.Supplier
//? if lp: >=3.0 {
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.EventBusErrorMessage
import net.neoforged.bus.api.BusBuilder
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModLoadingException
import net.neoforged.fml.ModLoadingIssue
import net.neoforged.fml.event.IModBusEvent
import net.neoforged.fml.loading.FMLLoader
import net.neoforged.neoforgespi.language.IModInfo
import java.lang.reflect.InvocationTargetException
//?} else {
/*//? if forge {
/^import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.eventbus.EventBusErrorMessage
import net.minecraftforge.eventbus.api.BusBuilder
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.ModContainer
import net.minecraftforge.fml.ModLoadingException
import net.minecraftforge.fml.ModLoadingStage
import net.minecraftforge.fml.event.IModBusEvent
import net.minecraftforge.fml.loading.FMLLoader
import net.minecraftforge.forgespi.language.IModInfo
import java.lang.reflect.InvocationTargetException
^///?} else {
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.EventBusErrorMessage
import net.neoforged.bus.api.BusBuilder
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModLoadingException
import net.neoforged.fml.event.IModBusEvent
import net.neoforged.fml.ModLoadingStage
import net.neoforged.fml.loading.FMLLoader
import net.neoforged.neoforgespi.language.IModInfo
import java.lang.reflect.InvocationTargetException
//?}
*///?}

@Suppress("NO_REFLECTION_IN_CLASS_PATH")
class KotlinModContainer(val info: IModInfo, entrypoints: List<String>, gameLayer: ModuleLayer) : ModContainer(info) {
    companion object {
        private val LOGGER: Logger = LogManager.getLogger()
        private val LOADING: Marker = MarkerManager.getMarker("LOADING")
    }

    private var modClasses: List<Class<*>>
    private var layer: Module
    internal var context: KlfLoadingContext
    internal var modBus: IEventBus

    init {
        LOGGER.debug(LOADING, "Creating KotlinModContainer instance with klf for {}", entrypoints)

        context = KlfLoadingContext(this)
        layer = gameLayer.findModule(info.owningFile.moduleName()).orElseThrow()
        modBus = BusBuilder.builder()
            .setExceptionHandler { _, event, listeners, busId, throwable ->
                LOGGER.error(EventBusErrorMessage(event, busId, listeners, throwable))
            }
            .markerType(IModBusEvent::class.java)
            //? if neoforge
            .allowPerPhasePost()
            .build()

        modClasses = entrypoints.map { entrypoint ->
            tryAndThrowWithModLoadingException(entrypoint) {
                val cls = Class.forName(layer, entrypoint)
                if (cls == null) throw ClassNotFoundException("Class '$entrypoint' could not be found!")
                LOGGER.trace(LOADING, "Loaded modclass {} with {}", cls.name, cls.classLoader)
                cls
            }
        }

        try {
            val contextExtensionField = ModContainer::class.java.getDeclaredField("contextExtension")
            val legacyExtension = Supplier { KlfLoadingContext }
            contextExtensionField.set(this, legacyExtension)
        } catch (_: NoSuchFieldException) {}
    }
    //? if lp: <=2.0 {
    /*init {
        activityMap[ModLoadingStage.CONSTRUCT] = Runnable(::createMod)
    }

    override fun matches(mod: Any?): Boolean {
        return modClasses.firstOrNull() == mod
    }

    override fun getMod(): Any? {
        return modClasses.firstOrNull()
    }
    
    *///?} else {
    override fun constructMod() {
        createMod()
    }
    //?}

    //? if neoforge {
    override fun getEventBus(): IEventBus {
        return modBus
    }
    //?}

    private fun createMod() {
        modClasses.forEach { modClass ->
            try {
                val constructors = modClass.constructors
                if (constructors.size == 0 && modClass.kotlin.objectInstance != null) return@forEach
                if (constructors.size != 1) throw RuntimeException("Mod class $modClass must have exactly 1 public constructor, found ${constructors.size}.")
                val constructor = constructors.first()

                val allowedConstructorArguments = mapOf<Class<*>, Any>(
                    IEventBus::class.java to modBus,
                    ModContainer::class.java to this,
                    KotlinModContainer::class.java to this,
                    Dist::class.java to FMLLoader.getDist()
                )

                val constructorArgs = constructor.parameterTypes.map { type ->
                    allowedConstructorArguments[type] ?: throw RuntimeException("Mod constructor has unsupported argument $type.")
                }
                constructor.newInstance(constructorArgs)

                LOGGER.trace(LOADING, "Loaded mod instance {} of type {}", modId, modClass.name)
            } catch (e: Throwable) {
                LOGGER.error(LOADING, "Failed to create mod instance. ModID: {}, class {}", getModId(), modClass.getName(), if (e is InvocationTargetException) e.cause else e)
                throw /*? if lp: <=2.0 {*/ /*ModLoadingException(info, ModLoadingStage.CONSTRUCT, "fml.modloading.failedtoloadmod", e, modClass)
                    *//*?} else {*/ ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.failedtoloadmod", e).withCause(e).withAffectedMod(modInfo)) /*?}*/
            }
        }
    }

    private fun <T> tryAndThrowWithModLoadingException(entrypoint: String, block: () -> T): T {
        try {
            return block()
        } catch (e: Throwable) {
            LOGGER.error(LOADING, "Failed to load class {}", entrypoint, e)
            throw /*? if lp: <=2.0 {*/ /*ModLoadingException(info, ModLoadingStage.CONSTRUCT, "fml.modloading.failedtoloadmod", e)
            *//*?} else {*/ ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.failedtoloadmod", e)) /*?}*/
        }
    }
}