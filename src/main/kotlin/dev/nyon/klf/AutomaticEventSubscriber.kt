package dev.nyon.klf

import dev.nyon.klf.KotlinModContainer.Companion.LOADING
import dev.nyon.klf.KotlinModContainer.Companion.LOGGER
import dev.nyon.klf.mv.Dist
import dev.nyon.klf.mv.EnumHolder
import dev.nyon.klf.mv.Event
import dev.nyon.klf.mv.EventBusSubscriber
import dev.nyon.klf.mv.FMLEnvironment
import dev.nyon.klf.mv.IModBusEvent
import dev.nyon.klf.mv.Mod
import dev.nyon.klf.mv.ModFileScanData
import dev.nyon.klf.mv.SubscribeEvent
import dev.nyon.klf.mv.gameBus
//? if lp: <=2.0
/*import dev.nyon.klf.mv.getAnnotatedBy*/
import java.lang.annotation.ElementType
import java.lang.reflect.Modifier
import java.util.*

object AutomaticEventSubscriber {

    fun inject(mod: KotlinModContainer, scanData: ModFileScanData, layer: Module) {
        LOGGER.debug(
            LOADING, "Attempting to inject kotlin @EventBusSubscriber classes into the eventbus for ${mod.modId}."
        )
        val targets = scanData.getAnnotatedBy(EventBusSubscriber::class.java, ElementType.TYPE).toList()
        val modIds = scanData.getAnnotatedBy(Mod::class.java, ElementType.TYPE).map { data ->
            data.clazz.className to data.annotationData["value"].toString()
        }.toList().toMap()

        targets.forEach { data ->
            val sides = getSides(data.annotationData["value"])
            val modId = data.annotationData["modid"]?.toString() ?: modIds[data.clazz.className] ?: mod.modId
            if (modId != mod.modId && !sides.contains(FMLEnvironment.dist)) return@forEach
            LOGGER.debug(LOADING, "Scanning class ${data.clazz.className} for @SubscribeEvent-annotated methods.")

            try {
                val clazz = Class.forName(data.clazz.className, true, layer.classLoader)
                processClass(clazz, mod)
            } catch (e: Exception) {
                LOGGER.fatal(LOADING, "Failed to register class ${data.clazz} with @EventBusSubscriber annotation.", e)
                throw RuntimeException(e)
            }
        }
    }

    private fun processClass(clazz: Class<*>, modContainer: KotlinModContainer) {
        clazz.declaredMethods.forEach { method ->
            if (!method.isAnnotationPresent(SubscribeEvent::class.java)) return@forEach
            if (!Modifier.isStatic(method.modifiers)) return@forEach
            if (method.parameterCount != 1 || !Event::class.java.isAssignableFrom(method.parameterTypes[0]))
                throw IllegalStateException("Method $method annotated with @SubscribeEvent must have only one parameter that is an Event subtype.")

            val eventType = method.parameterTypes[0]

            if (IModBusEvent::class.java.isAssignableFrom(eventType)) {
                val modBus = modContainer.modBus
                LOGGER.debug(LOADING, "Subscribing method $method to the event bus of mod ${modContainer.modId}.")
                modBus.register(method)
            } else {
                LOGGER.debug(LOADING, "Subscribing method $method to the game event bus.")
                gameBus.register(method)
            }
        }
    }

    internal fun getSides(data: Any?): EnumSet<Dist> {
        return if (data == null) EnumSet.allOf(Dist::class.java)
        else (data as MutableList<EnumHolder>).map { holder -> Dist.valueOf(holder.value) }
            .toCollection(EnumSet.noneOf(Dist::class.java))
    }
}