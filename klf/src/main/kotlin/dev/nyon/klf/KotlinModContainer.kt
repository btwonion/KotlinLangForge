package dev.nyon.klf

import dev.nyon.klf.mv.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.Marker
import org.apache.logging.log4j.MarkerManager
import java.lang.reflect.InvocationTargetException
import java.util.function.Supplier

//? lp: <=2.0
/*import dev.nyon.klf.mv.ModLoadingStage*/

@Suppress("NO_REFLECTION_IN_CLASS_PATH")
class KotlinModContainer(val info: IModInfo, entrypoints: List<String>, gameLayer: ModuleLayer, val scanResults: ModFileScanData) : ModContainer(info) {
    companion object {
        internal val LOGGER: Logger = LogManager.getLogger()
        internal val LOADING: Marker = MarkerManager.getMarker("LOADING")
    }

    private val modClasses: List<Class<*>>
    private val layer: Module
    internal val context: KlfLoadingContext
    internal val modBus: IEventBus

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
            tryAndThrowWithModLoadingException("Failed to load class {} $entrypoint.") {
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

    //? if forge {
    /*override fun <T> acceptEvent(e: T?) where T : Event?, T : net.minecraftforge.fml.event.IModBusEvent? {
        tryAndThrowWithModLoadingException("Caught exception during event $e dispatch for mod $modId.") {
            LOGGER.trace(LOADING, "Firing event $e for mod $modId.")
            modBus.post(e)
            LOGGER.trace(LOADING, "Fired event $e for mod $modId.")
        }
    }
    *///?}

    private fun createMod() {
        modClasses.forEach { modClass ->
            initModClass(modClass)
            injectAutomaticEventSubscriber()
        }
    }

    private fun initModClass(modClass: Class<*>) {
        try {
            val constructors = modClass.constructors
            if (constructors.size == 0 && modClass.kotlin.objectInstance != null) return
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
            constructor.newInstance(*constructorArgs.toTypedArray())

            LOGGER.trace(LOADING, "Loaded mod instance {} of type {}", modId, modClass.name)
        } catch (e: Throwable) {
            LOGGER.error(LOADING, "Failed to create mod instance. ModID: {}, class {}", getModId(), modClass.getName(), if (e is InvocationTargetException) e.cause else e)
            throw modLoadingException(e, modInfo)
        }
    }

    private fun injectAutomaticEventSubscriber() {
        tryAndThrowWithModLoadingException("Failed to register automatic event subscribers of mod $modId.") {
            LOGGER.trace(LOADING, "Injecting automatic event subscribers for $modId.")
            AutomaticEventSubscriber.inject(this, scanResults, layer)
            LOGGER.trace(LOADING, "Completed injecting automatic event subscribers for $modId.")
        }
    }

    private fun <T> tryAndThrowWithModLoadingException(errorMessage: String, block: () -> T): T {
        try {
            return block()
        } catch (e: Throwable) {
            LOGGER.error(LOADING, errorMessage, e)
            throw modLoadingException(e, modInfo)
        }
    }
}