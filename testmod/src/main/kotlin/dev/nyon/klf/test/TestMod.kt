package dev.nyon.klf.test

import dev.nyon.klf.KotlinModContainer

@Mod("klftestwithemptyconstructor")
class EmptyConstructorTestMod {
    init {
        println("Empty constructor test successful.")
    }
}

@Mod("klftestwithfullconstructor")
class FullConstructorTestMod(bus: IEventBus, container: ModContainer, kotlinModContainer: KotlinModContainer, dist: Dist) {
    init {
        println("""
            Full constructor test successful with:
            - $bus
            - $container
            - $kotlinModContainer
            - $dist
        """.trimIndent())
    }
}