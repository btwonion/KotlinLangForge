package dev.nyon.klf

import dev.nyon.klf.mv.IEventBus
import dev.nyon.klf.mv.ModLoadingContext

@Suppress("unused")
val MOD_BUS: IEventBus
    get() {
        return KlfLoadingContext.get().container.modBus
    }

class KlfLoadingContext(internal val container: KotlinModContainer) {

    companion object {
        @Suppress("DEPRECATION")
        fun get(): KlfLoadingContext {
            val active = /*? if lp: >2.0 {*/ModLoadingContext.get().activeContainer/*?} else {*/ /*ModLoadingContext.get().activeContainer *//*?}*/
            val kotlinModContainer = active as? KotlinModContainer ?:
                throw IllegalStateException("'${active.modId}' Tried to access KlfLoadingContext as container of type '${active::class.java.name}'.")
            return kotlinModContainer.context
        }
    }
}