package dev.nyon.klf

import dev.nyon.klf.KotlinModContainer.Companion.LOADING
import dev.nyon.klf.KotlinModContainer.Companion.LOGGER
import dev.nyon.klf.mv.*
import java.lang.annotation.ElementType
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*
//? if neoforge
import net.neoforged.bus.SubscribeEventListener

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
            if (modId != mod.modId || !sides.contains(dist)) return@forEach
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

    @Suppress("UNCHECKED_CAST")
    private fun processClass(clazz: Class<*>, modContainer: KotlinModContainer) {
        clazz.declaredMethods.forEach { method ->
            val subscribeEventAnnotation = runCatching { method.getDeclaredAnnotation(SubscribeEvent::class.java) }.getOrNull()
            val isObject = clazz.kotlin.objectInstance != null
            if (!Modifier.isStatic(method.modifiers) && !isObject) return@forEach
            val eventType = method.parameterTypes[0]
            if (method.parameterCount != 1 || !Event::class.java.isAssignableFrom(eventType)) return@forEach

            eventType as? Class<Event>
                ?: throw IllegalStateException("Argument of method $method annotated with @SubscribeEvent was not a subtype of Event.")

            if (IModBusEvent::class.java.isAssignableFrom(eventType)) {
                val modBus = modContainer.modBus
                LOGGER.debug(LOADING, "Subscribing method $method to the event bus of mod ${modContainer.modId}.")
                modBus.addListenerWithoutJvmStatic(subscribeEventAnnotation, method, clazz, eventType)
            } else {
                LOGGER.debug(LOADING, "Subscribing method $method to the game event bus.")
                gameBus.addListenerWithoutJvmStatic(subscribeEventAnnotation, method, clazz, eventType)
            }
        }
    }

    private fun IEventBus.addListenerWithoutJvmStatic(
        annotationData: SubscribeEvent?, method: Method, parentClass: Class<*>, eventType: Class<Event>
    ) {
        //? if neoforge {
        val listener = SubscribeEventListener(parentClass.kotlin.objectInstance ?: parentClass, method/*? if lp: <=2.0 {*//*, false*//*?}*/).withoutCheck
        if (annotationData == null) addListener(eventType, listener::invoke)
        else addListener(annotationData.priority, annotationData.receiveCanceled, eventType, listener::invoke)
        
        //?} else {
        /*addListener(annotationData?.priority ?: net.minecraftforge.eventbus.api.EventPriority.NORMAL, annotationData?.receiveCanceled ?: false, eventType) {
            method.invoke(parentClass.kotlin.objectInstance ?: parentClass, it)
        }
        *///?}
    }

    @Suppress("UNCHECKED_CAST")
    internal fun getSides(data: Any?): EnumSet<Dist> {
        return if (data == null) EnumSet.allOf(Dist::class.java)
        else (data as MutableList<EnumHolder>).map { holder -> Dist.valueOf(holder.value) }
            .toCollection(EnumSet.noneOf(Dist::class.java))
    }
}