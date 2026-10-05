@file:dev.nyon.klf.mv.EventBusSubscriber(modid = "regression")

package dev.nyon.klf.regression.fixtures

import dev.nyon.klf.mv.*
import dev.nyon.klf.regression.*
import net.neoforged.bus.api.EventPriority

class FirstEntrypoint {
    init { ConstructionState.entrypoints += "first" }
}

class QuietSecondEntrypoint {
    init { ConstructionState.entrypoints += "second" }
}

class SecondEntrypoint(bus: IEventBus) {
    init {
        ConstructionState.entrypoints += "second"
        // Automatic listeners must not be registered while entrypoints are still being constructed.
        val event = ModEvent()
        bus.post(event)
        check(event.calls.isEmpty())
    }
}

object ObjectEntrypoint {
    init { ConstructionState.entrypoints += "object" }
}

class FailingEntrypoint {
    init { error("constructor failed") }
}

@EventBusSubscriber(modid = "regression")
object ObjectSubscriber {
    init { ConstructionState.atSubscription = ConstructionState.entrypoints.toList() }

    val label get() = "ordinary property getter"
    fun helper() = "ordinary zero-argument helper"
    @SubscribeEvent fun annotatedHelper() = Unit
    fun nonEvent(value: String) { error("Not an event: $value") }
    fun twoArguments(event: GameEvent, value: String) { error("Two arguments") }

    @SubscribeEvent fun mod(event: ModEvent) { event.calls += "object-mod" }
    fun game(event: GameEvent) { event.calls += "object-game" }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun cancel(event: CancellableGameEvent) {
        event.calls += "cancel"
        event.setCanceled(true)
    }

    @SubscribeEvent
    fun skippedWhenCanceled(event: CancellableGameEvent) { event.calls += "unexpected" }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    fun receiveCanceled(event: CancellableGameEvent) { event.calls += "receive-canceled" }
}

class StaticSubscriber {
    fun instanceHelper() = Unit
    fun ignoredInstanceHandler(event: GameEvent) { event.calls += "unexpected-instance" }

    companion object {
        @JvmStatic fun helper() = Unit
        @JvmStatic @SubscribeEvent fun mod(event: ModEvent) { event.calls += "static-mod" }
        @JvmStatic fun game(event: GameEvent) { event.calls += "static-game" }
    }
}

fun fileHelper() = Unit
val fileLabel get() = "ordinary file getter"
@SubscribeEvent fun fileMod(event: ModEvent) { event.calls += "file-mod" }
fun fileGame(event: GameEvent) { event.calls += "file-game" }