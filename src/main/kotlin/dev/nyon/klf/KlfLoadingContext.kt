package dev.nyon.klf

//? if neoforge {
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModLoadingContext
//?} else {
/*import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.ModLoadingContext
*///?}

val MOD_BUS: IEventBus
    get() {
        return KlfLoadingContext.get().container.modBus
    }

class KlfLoadingContext(internal val container: KotlinModContainer) {

    companion object {
        fun get(): KlfLoadingContext {
            val active = /*? if lp: >2.0 {*/ModLoadingContext.get().activeContainer/*?} else {*/ /*ModLoadingContext.get().activeContainer *//*?}*/
            val kotlinModContainer = active as? KotlinModContainer ?:
                throw IllegalStateException("'${active.modId}' Tried to access KlfLoadingContext as container of type '${active::class.java.name}'.")
            return kotlinModContainer.context
        }
    }
}