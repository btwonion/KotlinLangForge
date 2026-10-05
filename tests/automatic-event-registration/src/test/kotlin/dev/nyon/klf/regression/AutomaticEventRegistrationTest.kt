package dev.nyon.klf.regression

import dev.nyon.klf.KotlinModContainer
import dev.nyon.klf.mv.*
import net.neoforged.bus.api.ICancellableEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.module.ModuleFinder
import java.nio.file.Path

object ConstructionState {
    val entrypoints = mutableListOf<String>()
    var atSubscription: List<String>? = null
}

class ModEvent : Event(), IModBusEvent { val calls = mutableListOf<String>() }
class GameEvent : Event() { val calls = mutableListOf<String>() }
class CancellableGameEvent : Event(), ICancellableEvent { val calls = mutableListOf<String>() }

class AutomaticEventRegistrationTest {
    private val fixturePackage = "dev.nyon.klf.regression.fixtures."

    @BeforeEach
    fun reset() {
        ConstructionState.entrypoints.clear()
        ConstructionState.atSubscription = null
        gameBus = BusBuilder.builder().build()
        dist = Dist.CLIENT
    }

    @Test
    fun `object helpers and getters do not prevent one registration per handler`() {
        val container = container(listOf("FirstEntrypoint"))
        container.construct()
        assertOneInvocationPerHandler(container)
    }

    @Test
    fun `two entrypoints register each handler once`() {
        val container = container(listOf("FirstEntrypoint", "QuietSecondEntrypoint"))
        container.construct()
        assertEquals(listOf("first", "second"), ConstructionState.entrypoints)
        assertOneInvocationPerHandler(container)
    }

    @Test
    fun `all entrypoints are constructed before subscribers are injected once`() {
        val container = container(listOf("FirstEntrypoint", "SecondEntrypoint", "ObjectEntrypoint"))
        container.construct()
        assertEquals(listOf("first", "second", "object"), ConstructionState.entrypoints)
        assertEquals(ConstructionState.entrypoints, ConstructionState.atSubscription)
        assertOneInvocationPerHandler(container)
    }

    @Test
    fun `an empty side-filtered entrypoint list still registers matching subscribers once`() {
        val container = container(emptyList())
        container.construct()
        assertEquals(emptyList<String>(), ConstructionState.atSubscription)
        assertOneInvocationPerHandler(container)
    }

    @Test
    fun `annotation priority and receiveCanceled are preserved`() {
        container(emptyList()).construct()
        val event = CancellableGameEvent()
        gameBus.post(event)
        assertEquals(listOf("cancel", "receive-canceled"), event.calls)
    }

    @Test
    fun `subscriber sides and mod ids are filtered before loading classes`() {
        val invalidTargets = listOf(
            subscriber("does.not.Exist", mapOf("modid" to "another_mod")),
            subscriber(
                "also.does.not.Exist", mapOf("value" to mutableListOf(EnumHolder("DEDICATED_SERVER")))
            )
        )
        val container = container(emptyList(), invalidTargets)
        container.construct()
        assertOneInvocationPerHandler(container)
    }

    @Test
    fun `a subscriber on the other side is skipped`() {
        dist = Dist.DEDICATED_SERVER
        container(emptyList()).construct()
        val event = GameEvent()
        gameBus.post(event)
        assertEquals(setOf("static-game", "file-game"), event.calls.toSet())
        assertEquals(2, event.calls.size)
        assertNull(ConstructionState.atSubscription)
    }

    @Test
    fun `failed entrypoint construction does not inject subscribers`() {
        val container = container(listOf("FirstEntrypoint", "FailingEntrypoint"))
        val error = assertThrows(RuntimeException::class.java) { container.construct() }
        assertTrue(generateSequence(error as Throwable) { it.cause }.any { it.message == "constructor failed" })
        val event = GameEvent()
        gameBus.post(event)
        assertTrue(event.calls.isEmpty())
        assertNull(ConstructionState.atSubscription)
    }

    private fun assertOneInvocationPerHandler(container: KotlinModContainer) {
        repeat(2) {
            val modEvent = ModEvent()
            container.modBus.post(modEvent)
            assertEquals(
                mapOf("object-mod" to 1, "static-mod" to 1, "file-mod" to 1),
                modEvent.calls.groupingBy { it }.eachCount()
            )
            val gameEvent = GameEvent()
            gameBus.post(gameEvent)
            assertEquals(
                mapOf("object-game" to 1, "static-game" to 1, "file-game" to 1),
                gameEvent.calls.groupingBy { it }.eachCount()
            )
        }
    }

    private fun subscriber(name: String, data: Map<String, Any?> = emptyMap()) =
        AnnotationData(EventBusSubscriber::class.java, ClassType(name), data)

    private fun container(
        entrypoints: List<String>, extraTargets: List<AnnotationData> = emptyList()
    ): KotlinModContainer {
        val moduleName = "klf.regression.fixtures"
        val finder = ModuleFinder.of(Path.of(System.getProperty("regression.fixtureJar")))
        val parent = ModuleLayer.boot()
        val configuration = parent.configuration().resolve(finder, ModuleFinder.of(), setOf(moduleName))
        val layer = parent.defineModulesWithOneLoader(configuration, javaClass.classLoader)
        val scan = ModFileScanData(
            listOf(
                // Omitted modid falls back to the class's @Mod annotation.
                AnnotationData(
                    Mod::class.java, ClassType(fixturePackage + "ObjectSubscriber"), mapOf("value" to "regression")
                ),
                subscriber(fixturePackage + "ObjectSubscriber", mapOf("value" to mutableListOf(EnumHolder("CLIENT")))),
                subscriber(fixturePackage + "StaticSubscriber", mapOf("modid" to "regression")),
                subscriber(fixturePackage + "SubscribersKt")
            ) + extraTargets
        )
        return KotlinModContainer(
            IModInfo("regression", IModFileInfo(moduleName)),
            entrypoints.map { fixturePackage + it }, layer, scan
        )
    }
}