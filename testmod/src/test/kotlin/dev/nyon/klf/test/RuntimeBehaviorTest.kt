package dev.nyon.klf.test

import dev.nyon.klf.KlfLoadingContext
import dev.nyon.klf.KotlinLanguageLoader
import dev.nyon.klf.KotlinModContainer
import dev.nyon.klf.MOD_BUS
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.objectweb.asm.Type
import java.lang.annotation.ElementType
import java.lang.module.ModuleFinder
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.nio.file.Path
import java.util.Optional

/** Real KLF containers, named fixture modules and real FML/event-bus classes; no Minecraft bootstrap. */
class RuntimeBehaviorTest {
    private lateinit var layer: ModuleLayer
    private var modernLoader: AutoCloseable? = null
    private val fixturePackage = "dev.nyon.klf.test.runtime."
    private val testDist: Dist get() = Dist.valueOf(System.getProperty("klf.test.dist"))

    @BeforeEach
    fun prepareLoaderBoundary() {
        val finder = ModuleFinder.of(Path.of(System.getProperty("klf.fixtureJar")))
        val configuration = ModuleLayer.boot().configuration().resolve(finder, ModuleFinder.of(), setOf("klf.smoke.fixture"))
        layer = ModuleLayer.boot().defineModulesWithOneLoader(configuration, javaClass.classLoader)
        setEnvironment(testDist, System.getProperty("klf.mcVersion"))
    }

    @AfterEach
    fun clearLoaderBoundary() {
        ModLoadingContext.get().setActiveContainer(null)
        modernLoader?.close()
        modernLoader = null
    }

    @Test
    fun `empty and singleton entrypoints construct once`() {
        val container = container(listOf("EmptyEntrypoint", "SingletonEntrypoint"))
        construct(container)
        assertEquals(1, count("empty"))
        assertEquals(1, count("singleton"))
    }

    @Test
    fun `constructor receives the active bus container and distribution`() {
        val container = container(listOf("InjectedEntrypoint"))
        construct(container)
        assertEquals(1, count("injected"))
        assertSame(container, KlfLoadingContext.get().let { context ->
            val field = context.javaClass.getDeclaredField("container").apply { isAccessible = true }
            field.get(context)
        })
        assertSame(bus(container), MOD_BUS)
    }

    @Test
    fun `invalid constructor forms fail with a loading exception`() {
        for (entrypoint in listOf("UnsupportedConstructor", "MultipleConstructors", "NoPublicConstructor")) {
            val failure = assertThrows(ModLoadingException::class.java) { construct(container(listOf(entrypoint))) }
            assertNotNull(loadingCause(failure), entrypoint)
        }
    }

    @Test
    fun `annotated and unannotated mod listeners each run once per post`() {
        val container = container(listOf("EmptyEntrypoint"), scan("OrdinarySubscriber"))
        construct(container)
        repeat(2) { post(container) }
        assertEquals(2, count("annotated"))
        assertEquals(2, count("unannotated"))
    }

    @Test
    fun `public static class listener receives events`() {
        val container = container(listOf("EmptyEntrypoint"), scan("StaticSubscriber"))
        construct(container)
        post(container)
        assertEquals(1, count("static"))
    }

    @Test
    fun `game listener is routed to the game bus rather than the mod bus`() {
        val container = container(listOf("EmptyEntrypoint"), scan("GameSubscriber"))
        construct(container)
        val gameBus = Class.forName("dev.nyon.klf.mv.ExtensionsKt").getDeclaredMethod("getGameBus").invoke(null) as IEventBus
        assertNotSame(bus(container), gameBus)
        repeat(2) { gameBus.post(fixture("SmokeGameEvent").getConstructor().newInstance() as Event) }
        assertEquals(2, count("game"))
    }

    //? if neoforge {
    @Test
    fun `NeoForge private subscriber receives events`() {
        val container = container(listOf("EmptyEntrypoint"), scan("PrivateSubscriber"))
        construct(container)
        post(container)
        assertEquals(1, count("privateCalls"))
    }

    @Test
    fun `NeoForge container event bus receives loading and reloading config events`() {
        val container = container(listOf("EmptyEntrypoint"))
        construct(container)
        var loading = 0
        var reloading = 0
        bus(container).addListener(net.neoforged.fml.event.config.ModConfigEvent.Loading::class.java) { loading++ }
        bus(container).addListener(net.neoforged.fml.event.config.ModConfigEvent.Reloading::class.java) { reloading++ }
        dispatch(container, net.neoforged.fml.event.config.ModConfigEvent.Loading(null))
        dispatch(container, net.neoforged.fml.event.config.ModConfigEvent.Reloading(null))
        assertEquals(1, loading)
        assertEquals(1, reloading)
    }
    //?}

    @Test
    fun `subscriber on opposite distribution is skipped before class loading`() {
        val opposite = if (testDist == Dist.CLIENT) Dist.DEDICATED_SERVER else Dist.CLIENT
        val data = scan("MissingOppositeSideClass", mapOf("value" to sides(opposite)))
        construct(container(listOf("EmptyEntrypoint"), data))
        assertEquals(1, count("empty"))
    }

    @Test
    fun `subscriber on matching distribution receives events`() {
        val container = container(listOf("EmptyEntrypoint"), scan("OrdinarySubscriber", mapOf("value" to sides(testDist))))
        construct(container)
        post(container)
        assertEquals(1, count("annotated"))
    }

    @Test
    fun `subscriber belonging to another mod is skipped before class loading`() {
        construct(container(listOf("EmptyEntrypoint"), scan("MissingOtherModClass", mapOf("modid" to "anothermod"))))
        assertEquals(1, count("empty"))
    }

    @Test
    fun `subscriber mod id is inferred from its Mod annotation`() {
        val data = scan("MissingOtherModClass")
        data.annotations.add(annotation(Mod::class.java, "MissingOtherModClass", mapOf("value" to "anothermod")))
        construct(container(listOf("EmptyEntrypoint"), data))
        assertEquals(1, count("empty"))
    }

    //? if lp: >=3.0 {
    @Test
    fun `modern loader filters entrypoints by mod id and physical distribution`() {
        val data = ModFileScanData()
        data.annotations.add(annotation(Mod::class.java, "EmptyEntrypoint", mapOf("value" to "smoke")))
        val opposite = if (testDist == Dist.CLIENT) Dist.DEDICATED_SERVER else Dist.CLIENT
        data.annotations.add(annotation(Mod::class.java, "MissingOppositeSideEntrypoint", mapOf("value" to "smoke", "dist" to sides(opposite))))
        data.annotations.add(annotation(Mod::class.java, "MissingOtherModEntrypoint", mapOf("value" to "anothermod")))
        val loaded = KotlinLanguageLoader().loadMod(info(), data, layer) as KotlinModContainer
        construct(loaded)
        assertEquals(1, count("empty"))
    }
    //?}

    @Test
    fun `wrong Minecraft version is rejected before entrypoint loading with useful diagnostic`() {
        setEnvironment(testDist, "0.0-smoke-incompatible")
        val failure = assertThrows(ModLoadingException::class.java) {
            KotlinLanguageLoader().loadMod/*? if lp: <=2.0 {*//*<ModContainer>*//*?}*/(info(), ModFileScanData(), layer)
        }
        val messages = causes(failure).mapNotNull { it.message }.joinToString("\n")
        assertTrue(messages.contains("0.0-smoke-incompatible"), messages)
        assertTrue(messages.contains("KotlinLangForge"), messages)
        assertTrue(messages.contains(System.getProperty("klf.mcVersion")), messages)
        assertTrue(messages.lowercase().contains("install"), messages)
    }

    @Test
    @Tag("event-regression")
    fun `subscriber helper getter and wrong signatures are ignored safely`() {
        val container = container(listOf("EmptyEntrypoint"), scan("HelpersSubscriber"))
        construct(container)
        post(container)
        assertEquals(1, count("annotated"))
    }

    @Test
    @Tag("event-regression")
    fun `multiple entrypoints register subscribers once`() {
        val container = container(listOf("EmptyEntrypoint", "SingletonEntrypoint"), scan("OrdinarySubscriber"))
        construct(container)
        post(container)
        assertEquals(1, count("annotated"))
        assertEquals(1, count("unannotated"))
    }

    @Test
    fun `JvmStatic object listener is registered once`() {
        val container = container(listOf("EmptyEntrypoint"), scan("JvmStaticSubscriber"))
        construct(container)
        post(container)
        assertEquals(1, count("static"))
    }

    @Test
    @Tag("diagnostics-regression")
    fun `constructor failure preserves the original cause directly`() {
        val failure = assertThrows(ModLoadingException::class.java) { construct(container(listOf("ThrowingConstructor"))) }
        assertInstanceOf(IllegalStateException::class.java, loadingCause(failure))
        assertEquals("fixture-constructor-cause", loadingCause(failure)!!.message)
    }

    //? if lp: <=2.0 {
    /*@Test
    @Tag("forge-regression")
    fun `legacy container exposes constructed instances and supports primary object lookup`() {
        val container = container(listOf("EmptyEntrypoint"))
        construct(container)
        val instance = container.mod
        assertNotNull(instance)
        assertEquals(fixturePackage + "EmptyEntrypoint", instance!!::class.java.name)
        assertTrue(container.matches(instance))
        assertFalse(container.matches(instance.javaClass))
        val list = ModList.of(emptyList(), emptyList())
        ModList::class.java.getDeclaredMethod("setLoadedMods", List::class.java)
            .apply { isAccessible = true }.invoke(list, listOf(container))
        assertSame(container, list.getModContainerByObject(instance).orElseThrow())
    }

    *///?}

    //? if forge {
    /*@Test
    @Tag("forge-regression")
    fun `Forge dispatches loading and reloading config events to mod listeners`() {
        val container = container(listOf("EmptyEntrypoint"))
        construct(container)
        var loading = 0
        var reloading = 0
        val loadingEvent = net.minecraftforge.fml.event.config.ModConfigEvent.Loading(null)
        val reloadingEvent = net.minecraftforge.fml.event.config.ModConfigEvent.Reloading(null)
        // Without ModLauncher transforms these events have no no-arg constructor. The instance API resolves
        // their real listener lists before class-based registration would try to instantiate them itself.
        net.minecraftforge.fml.event.config.ModConfigEvent::class.java
            .getDeclaredConstructor(net.minecraftforge.fml.config.ModConfig::class.java)
            .apply { isAccessible = true }.newInstance(null).listenerList
        loadingEvent.listenerList
        reloadingEvent.listenerList
        bus(container).addListener(net.minecraftforge.eventbus.api.EventPriority.NORMAL, false, net.minecraftforge.fml.event.config.ModConfigEvent.Loading::class.java) { loading++ }
        bus(container).addListener(net.minecraftforge.eventbus.api.EventPriority.NORMAL, false, net.minecraftforge.fml.event.config.ModConfigEvent.Reloading::class.java) { reloading++ }
        container.dispatchConfigEvent(loadingEvent)
        container.dispatchConfigEvent(reloadingEvent)
        assertEquals(1, loading)
        assertEquals(1, reloading)
    }
    *///?}

    private fun fixture(name: String): Class<*> = Class.forName(layer.findModule("klf.smoke.fixture").orElseThrow(), fixturePackage + name)
        ?: error("Missing fixture $name")

    private fun count(name: String): Int = fixture("FixtureState").getField(name).getInt(null)

    private fun bus(container: KotlinModContainer): IEventBus = KotlinModContainer::class.java.getDeclaredField("modBus")
        .apply { isAccessible = true }.get(container) as IEventBus

    private fun post(container: KotlinModContainer) {
        dispatch(container, fixture("SmokeModEvent").getConstructor().newInstance() as Event)
    }

    private fun dispatch(container: KotlinModContainer, event: Event) {
        try {
            ModContainer::class.java.getDeclaredMethod("acceptEvent", Event::class.java)
                .apply { isAccessible = true }.invoke(container, event)
        } catch (exception: InvocationTargetException) {
            throw exception.targetException
        }
    }

    private fun container(entrypoints: List<String>, data: ModFileScanData = ModFileScanData()) =
        KotlinModContainer(info(), entrypoints.map { fixturePackage + it }, layer, data)

    private fun construct(container: KotlinModContainer) {
        ModLoadingContext.get().setActiveContainer(container)
        try {
            // The legacy API exposes construction through the activity map; the modern API has constructMod().
            //? if lp: <=2.0 {
            /*val activities = ModContainer::class.java.getDeclaredField("activityMap").apply { isAccessible = true }
                .get(container) as Map<*, *>
            val construct = activities.entries.single { (it.key as Enum<*>).name == "CONSTRUCT" }.value as Runnable
            construct.run()
            *///?} else {
            val method = KotlinModContainer::class.java.getDeclaredMethod("constructMod")
            method.isAccessible = true
            method.invoke(container)
            //?}
        } catch (exception: InvocationTargetException) {
            throw exception.targetException
        }
    }

    private fun scan(subscriber: String, values: Map<String, Any> = emptyMap()) = ModFileScanData().apply {
        annotations.add(annotation(EventBusSubscriber::class.java, subscriber, values))
    }

    private fun annotation(type: Class<*>, name: String, values: Map<String, Any>) = AnnotationData(
        Type.getType(type), ElementType.TYPE, Type.getObjectType((fixturePackage + name).replace('.', '/')), "", values
    )

    private fun sides(dist: Dist) = arrayListOf(EnumHolder(Type.getDescriptor(Dist::class.java), dist.name))

    // Only FML discovery metadata is doubled. Containers, loading context, scan records and buses are real.
    private fun info(): IModInfo = metadata(IModInfo::class.java) as IModInfo

    private fun metadata(type: Class<*>): Any = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(type)) { proxy, method, args ->
        when (method.name) {
            "getModId", "getNamespace" -> "smoke"
            "getDisplayName" -> "KLF runtime smoke fixture"
            "moduleName", "getId" -> "klf.smoke.fixture"
            "getConfigElement" -> Optional.empty<Any>()
            "getConfig", "getOwningFile", "getFile" -> metadata(method.returnType)
            "getFilePath" -> Path.of(System.getProperty("klf.fixtureJar"))
            "getVersion" -> org.apache.maven.artifact.versioning.DefaultArtifactVersion("1.0")
            "getDependencies", "getModInfos" -> emptyList<Any>()
            "getFileProperties" -> emptyMap<String, Any>()
            "toString" -> "SmokeMetadata(${type.simpleName})"
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args?.get(0)
            else -> error("Unexpected discovery metadata call: $method")
        }
    }

    private fun setEnvironment(dist: Dist, minecraftVersion: String) {
        //? if lp: >3.0 {
        modernLoader?.close()
        val constructor = FMLLoader::class.java.getDeclaredConstructor(
            ClassLoader::class.java, Array<String>::class.java, Dist::class.java, Boolean::class.javaPrimitiveType, Path::class.java
        ).apply { isAccessible = true }
        modernLoader = constructor.newInstance(javaClass.classLoader,
            arrayOf("--fml.mcVersion", minecraftVersion, "--fml.neoForgeVersion", "smoke", "--fml.neoFormVersion", "smoke"),
            dist, false, Path.of(System.getProperty("java.io.tmpdir"))) as AutoCloseable
        FMLLoader::class.java.getDeclaredField("currentClassLoader").apply { isAccessible = true }
            .set(modernLoader, layer.findLoader("klf.smoke.fixture"))
        FMLLoader::class.java.getDeclaredField("gameLayer").apply { isAccessible = true }.set(modernLoader, layer)
        //?} else {
        /*// FMLEnvironment captures this field in a static final; each physical side gets a separate test worker.
        FMLLoader::class.java.getDeclaredField("dist").apply { isAccessible = true }.set(null, dist)
        val field = FMLLoader::class.java.getDeclaredField("versionInfo").apply { isAccessible = true }
        val components = field.type.recordComponents
        val values = components.map { if (it.name == "mcVersion") minecraftVersion else "1.0" }.toTypedArray()
        field.set(null, field.type.getDeclaredConstructor(*components.map { it.type }.toTypedArray()).newInstance(*values))
        //? if lp: <=2.0 {
        val manager = FMLLoader::class.java.getDeclaredField("moduleLayerManager").apply { isAccessible = true }
        manager.set(null, Proxy.newProxyInstance(javaClass.classLoader, arrayOf(manager.type)) { _, method, _ ->
            check(method.name == "getLayer") { "Unexpected module layer manager call: $method" }
            Optional.of(layer)
        })
        //?} else {
        FMLLoader::class.java.getDeclaredField("gameLayer").apply { isAccessible = true }.set(null, layer)
        //?}
        *///?}
    }

    private fun loadingCause(failure: ModLoadingException): Throwable? =
        /*? if lp: >=3.0 {*/failure.issues.single().cause/*?} else {*//*failure.cause*//*?}*/

    private fun causes(failure: ModLoadingException): List<Throwable> =
        // Legacy FML's outer message formatter needs the full game layer/I18N binding. Inspect KLF's cause here.
        generateSequence(loadingCause(failure)) { it.cause }.toList()
}
