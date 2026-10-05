package dev.nyon.klf.test.runtime

import dev.nyon.klf.KotlinModContainer
import dev.nyon.klf.MOD_BUS
import dev.nyon.klf.test.*

// Deliberately unannotated: the JVM suite supplies scan records, including failing regression cases.
// A normal Minecraft launch must not accidentally enable those cases before their fixes are merged.
object FixtureState {
    @JvmField var empty = 0
    @JvmField var injected = 0
    @JvmField var singleton = 0
    @JvmField var annotated = 0
    @JvmField var unannotated = 0
    @JvmField var static = 0
    @JvmField var privateCalls = 0
    @JvmField var game = 0
}

class EmptyEntrypoint {
    init { FixtureState.empty++ }
}

class InjectedEntrypoint(bus: IEventBus, container: ModContainer, kotlinContainer: KotlinModContainer, dist: Dist) {
    init {
        check(container === kotlinContainer) { "Container arguments must be the same instance" }
        check(bus === MOD_BUS) { "Constructor bus must match the active loading context" }
        check(dist.name == System.getProperty("klf.test.dist")) { "Incorrect constructor distribution" }
        FixtureState.injected++
    }
}

object SingletonEntrypoint {
    init { FixtureState.singleton++ }
}

class UnsupportedConstructor(val unsupported: String)
class ThrowingConstructor {
    init { error("fixture-constructor-cause") }
}
class MultipleConstructors {
    constructor()
    constructor(unused: IEventBus)
}
class NoPublicConstructor private constructor()

class SmokeModEvent : Event(), IModBusEvent
class SmokeGameEvent : Event()

object GameSubscriber {
    @SubscribeEvent
    fun onEvent(event: SmokeGameEvent) { FixtureState.game++ }
}

object OrdinarySubscriber {
    @SubscribeEvent
    fun annotated(event: SmokeModEvent) { FixtureState.annotated++ }

    fun unannotated(event: SmokeModEvent) { FixtureState.unannotated++ }
}

object HelpersSubscriber {
    val description: String get() = "ordinary property getter"
    fun helper() = "ordinary zero argument helper"
    fun unrelated(text: String) = text
    fun tooManyArguments(event: SmokeModEvent, text: String) = text

    @SubscribeEvent
    fun onEvent(event: SmokeModEvent) { FixtureState.annotated++ }
}

object JvmStaticSubscriber {
    @JvmStatic
    @SubscribeEvent
    fun onEvent(event: SmokeModEvent) { FixtureState.static++ }
}

// NeoForge supports non-public subscribers; Forge's reflection path needs public methods.
object PrivateSubscriber {
    @SubscribeEvent
    private fun onEvent(event: SmokeModEvent) { FixtureState.privateCalls++ }
}

class StaticSubscriber {
    companion object {
        @JvmStatic
        @SubscribeEvent
        fun onEvent(event: SmokeModEvent) { FixtureState.static++ }
    }
}
