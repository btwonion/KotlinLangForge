package dev.nyon.klf

/*? if neoforge {*/
import net.neoforged.bus.EventBusErrorMessage
import net.neoforged.bus.api.BusBuilder
import net.neoforged.fml.event.IModBusEvent
/*?} else {*/
/*import net.minecraftforge.eventbus.EventBusErrorMessage
import net.minecraftforge.eventbus.api.BusBuilder
//? if lp: >1.0
import net.minecraftforge.fml.event.IModBusEvent
*//*?}*/

val MOD_BUS = BusBuilder.builder()
    .setExceptionHandler { _, event, listeners, busId, throwable ->
        println(
            EventBusErrorMessage(
                event,
                busId,
                listeners,
                throwable
            )
        )
    }
    //? if lp: >1.0
    .markerType(IModBusEvent::class.java)
    //? if neoforge
    .allowPerPhasePost()
    .build()


object KlfLoadingContext