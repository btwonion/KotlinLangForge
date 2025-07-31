package dev.nyon.klf.test

import dev.nyon.klf.KotlinModContainer

@Mod("klftestwithemptyconstructor")
class EmptyConstructorTestMod {
    init {
        println("1 -------------- SUCCESSFUL Empty constructor test successful.")
    }
}

@Mod("klftestwithfullconstructor")
class FullConstructorTestMod(bus: IEventBus, container: ModContainer, kotlinModContainer: KotlinModContainer, dist: Dist) {
    init {
        println("""
            2 -------------- SUCCESSFUL
            Full constructor test successful with:
            - $bus
            - $container
            - $kotlinModContainer
            - $dist
        """.trimIndent())
    }
}

@Mod("klftestwithautoeventsubscriber")
@EventBusSubscriber
object AutoEventSubscriberTest {
    init {
        println("3 -------------- SUCCESSFUL EventSubscriberTest initialized")
    }

    @SubscribeEvent
    /*? if lp: >=3.0 {*/ private /*?}*/ fun constructModEvent(event: FMLConstructModEvent) {
        println("4 -------------- SUCCESSFUL Construct event fired with private modifier!")
    }

    fun newRegistryEvent(event: NewRegistryEvent) {
        println("5 -------------- SUCCESSFUL Registry event fired without annotation!")
    }

    @SubscribeEvent
    fun playerEvent(event: LoadFromFile) {
        println("6 -------------- SUCCESSFUL Player event fired!")
    }
}