package dev.nyon.klf.mv

import java.lang.annotation.ElementType
import java.util.stream.Stream

typealias IEventBus = net.neoforged.bus.api.IEventBus
typealias BusBuilder = net.neoforged.bus.api.BusBuilder
typealias Event = net.neoforged.bus.api.Event
typealias EventBusErrorMessage = net.neoforged.bus.EventBusErrorMessage
typealias SubscribeEvent = net.neoforged.bus.api.SubscribeEvent

interface IModBusEvent
enum class Dist { CLIENT, DEDICATED_SERVER }
var dist = Dist.CLIENT
var gameBus: IEventBus = BusBuilder.builder().build()

@Target(AnnotationTarget.CLASS)
annotation class Mod(val value: String)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
annotation class EventBusSubscriber(val modid: String = "")

class EnumHolder(val value: String)
class ClassType(val className: String)
class IModFileInfo(val moduleName: String)
class IModInfo(val modId: String, val owningFile: IModFileInfo)

data class AnnotationData(
    val annotationType: Class<*>,
    val clazz: ClassType,
    val annotationData: Map<String, Any?> = emptyMap()
)

class ModFileScanData(val annotations: List<AnnotationData>) {
    fun getAnnotatedBy(type: Class<*>, target: ElementType): Stream<AnnotationData> {
        check(target == ElementType.TYPE)
        return annotations.filter { it.annotationType == type }.stream()
    }
}

fun modLoadingException(cause: Throwable, info: IModInfo): RuntimeException =
    RuntimeException("Failed to load ${info.modId}", cause)