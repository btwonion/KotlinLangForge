package dev.nyon.klf.fixtures

import dev.nyon.klf.KotlinModContainer
import dev.nyon.klf.mv.IEventBus
import dev.nyon.klf.mv.ModContainer

class PlainEntrypoint {
    // Deliberately equal to other objects: loader lookup must compare identity.
    override fun equals(other: Any?) = other != null
    override fun hashCode() = 0
}

object SingletonEntrypoint

class InjectedEntrypoint(val bus: IEventBus, val container: ModContainer, val kotlinContainer: KotlinModContainer)
