package dev.nyon.klf.test

import dev.nyon.klf.KotlinModContainer
import dev.nyon.klf.MOD_BUS
import java.util.IdentityHashMap
import java.nio.file.Files
import java.nio.file.Path

private object LaunchAssertions {
    private val counts = mutableMapOf<String, Int>()
    private val deliveredEvents = IdentityHashMap<Event, MutableSet<String>>()

    @Synchronized
    fun once(name: String) {
        val count = counts.getOrDefault(name, 0) + 1
        counts[name] = count
        check(count == 1) { "$name ran $count times; expected once" }
    }

    @Synchronized
    fun eventOnce(name: String, event: Event) {
        check(deliveredEvents.getOrPut(event) { mutableSetOf() }.add(name)) {
            "$name received the same event twice"
        }
    }

    @Synchronized
    fun verify() {
        for (name in listOf("empty constructor", "injected constructor", "subscriber object", "construct event", "registry event")) {
            check(counts[name] == 1) { "$name ran ${counts[name] ?: 0} times; expected once" }
        }
        val client = /*? if lp: >3.0 {*/FMLLoader.getCurrent().dist/*?} else {*//*FMLEnvironment.dist*//*?}*/ == Dist.CLIENT
        check(counts[if (client) "client setup" else "server setup"] == 1) { "Matching side subscriber did not run once" }
        check(counts[if (client) "server setup" else "client setup"] == null) { "Opposite side subscriber ran" }
        println("KLF_LAUNCH_ASSERTIONS_PASSED")
    }
}

@EventBusSubscriber(modid = "klftestwithautoeventsubscriber", value = [Dist.CLIENT])
object ClientOnlyLaunchSubscriber {
    @SubscribeEvent
    fun setup(event: FMLCommonSetupEvent) {
        check(net.minecraft.client.Minecraft.getInstance() != null) { "Client runtime was not available" }
        LaunchAssertions.once("client setup")
    }
}

@EventBusSubscriber(modid = "klftestwithautoeventsubscriber", value = [Dist.DEDICATED_SERVER])
object ServerOnlyLaunchSubscriber {
    @SubscribeEvent
    fun setup(event: FMLCommonSetupEvent) {
        LaunchAssertions.once("server setup")
    }
}

@Mod("klftestwithemptyconstructor")
class EmptyConstructorTestMod {
    init {
        LaunchAssertions.once("empty constructor")
    }
}

@Mod("klftestwithfullconstructor")
class FullConstructorTestMod(bus: IEventBus, container: ModContainer, kotlinModContainer: KotlinModContainer, dist: Dist) {
    init {
        check(container === kotlinModContainer) { "Injected containers differ" }
        check(bus === MOD_BUS) { "Injected bus differs from the active loading context" }
        check(dist == /*? if lp: >3.0 {*/FMLLoader.getCurrent().dist/*?} else {*//*FMLEnvironment.dist*//*?}*/) {
            "Injected distribution differs from FML"
        }
        LaunchAssertions.once("injected constructor")
    }
}

@Mod("klftestwithautoeventsubscriber")
@EventBusSubscriber
object AutoEventSubscriberTest {
    init {
        LaunchAssertions.once("subscriber object")
    }

    @SubscribeEvent
    /*? if lp: >=3.0 {*/ private /*?}*/ fun constructModEvent(event: FMLConstructModEvent) {
        LaunchAssertions.once("construct event")
    }

    fun newRegistryEvent(event: NewRegistryEvent) {
        LaunchAssertions.once("registry event")
    }

    @SubscribeEvent
    fun playerEvent(event: LoadFromFile) {
        LaunchAssertions.eventOnce("player load event", event)
    }

    @SubscribeEvent
    fun loadComplete(event: FMLLoadCompleteEvent) {
        LaunchAssertions.once("load complete event")
        LaunchAssertions.verify()
    }

    @SubscribeEvent
    fun serverStarted(event: ServerStartedEvent) {
        System.getProperty("klf.smoke.result")?.let { result ->
            LaunchAssertions.once("server started event")
            LaunchAssertions.verify()
            Files.writeString(Path.of(result), "KLF_SERVER_SMOKE_PASSED\n")
            event.server.halt(false)
        }
    }
}
