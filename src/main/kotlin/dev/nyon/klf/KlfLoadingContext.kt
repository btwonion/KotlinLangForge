package dev.nyon.klf

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.Marker
import org.apache.logging.log4j.MarkerManager
//? if neoforge {
import net.neoforged.bus.EventBusErrorMessage
import net.neoforged.bus.api.BusBuilder
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModList
import net.neoforged.fml.event.IModBusEvent
//?} else {
/*import net.minecraftforge.eventbus.EventBusErrorMessage
import net.minecraftforge.eventbus.api.BusBuilder
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.ModContainer
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.event.IModBusEvent
*/
//?}

internal val LOGGER: Logger = LogManager.getLogger()
internal val LOADING: Marker = MarkerManager.getMarker("LOADING")

val MOD_BUS: IEventBus = BusBuilder.builder()
    .setExceptionHandler { _, event, listeners, busId, throwable ->
        LOGGER.error(EventBusErrorMessage(event, busId, listeners, throwable))
    }
    .markerType(IModBusEvent::class.java)
    //? if neoforge
    .allowPerPhasePost()
    .build()

object KlfLoadingContext {
    var activeContainer: ModContainer? = null
        get() = if (activeContainer == null) ModList.get().getModContainerById("minecraft").orElseThrow() else activeContainer

    val activeNamespace: String
        get() = if (activeContainer == null) "minecraft" else activeContainer!!.namespace
}