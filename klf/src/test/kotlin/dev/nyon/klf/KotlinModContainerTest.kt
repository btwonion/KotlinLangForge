package dev.nyon.klf

import dev.nyon.klf.fixtures.InjectedEntrypoint
import dev.nyon.klf.fixtures.PlainEntrypoint
import dev.nyon.klf.fixtures.SingletonEntrypoint
import dev.nyon.klf.mv.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.lang.module.ModuleFinder
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

//? if forge {
/*import net.minecraftforge.fml.event.config.ModConfigEvent.Loading
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.fml.event.config.ModConfigEvent.Reloading
import net.minecraftforge.fml.config.IConfigSpec
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.loading.FMLConfig
import net.minecraftforge.fml.loading.FMLPaths
*///?} else {
import net.neoforged.fml.event.config.ModConfigEvent.Loading
import net.neoforged.bus.api.EventPriority
import net.neoforged.fml.event.config.ModConfigEvent.Reloading
//? if lp: <=3.0 {
/*import net.neoforged.fml.config.IConfigSpec
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.loading.FMLConfig
import net.neoforged.fml.loading.FMLPaths
*///?}
//?}

//? if lp: <=3.0
//import dev.nyon.klf.fixtures.HeadlessBindings

class KotlinModContainerTest {
    companion object {
        private const val MODULE = "klf.test.entrypoints"
        private lateinit var layer: ModuleLayer

        @JvmStatic
        @BeforeAll
        fun prepareGameLayer(@TempDir directory: Path) {
            val jar = directory.resolve("entrypoints.jar")
            val manifest = Manifest().apply {
                mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
                mainAttributes.putValue("Automatic-Module-Name", MODULE)
            }
            JarOutputStream(Files.newOutputStream(jar), manifest).use { output ->
                val fixtures = mutableListOf(PlainEntrypoint::class.java, SingletonEntrypoint::class.java, InjectedEntrypoint::class.java)
                //? if lp: <=3.0
                //fixtures.add(HeadlessBindings::class.java)
                fixtures.forEach { fixture ->
                    val resource = fixture.name.replace('.', '/') + ".class"
                    output.putNextEntry(JarEntry(resource))
                    fixture.classLoader.getResourceAsStream(resource)!!.use { it.copyTo(output) }
                    output.closeEntry()
                }
                //? if lp: <=3.0 {
                /*val service = /*? if forge {*/ "net.minecraftforge.fml.IBindingsProvider" /*?} else {*/ "net.neoforged.fml.IBindingsProvider" /*?}*/
                output.putNextEntry(JarEntry("META-INF/services/$service"))
                output.write(HeadlessBindings::class.java.name.toByteArray())
                output.closeEntry()
                *///?}
            }
            val finder = ModuleFinder.of(jar)
            val configuration = ModuleLayer.boot().configuration().resolve(finder, ModuleFinder.of(), setOf(MODULE))
            layer = ModuleLayer.boot().defineModulesWithOneLoader(configuration, KotlinModContainerTest::class.java.classLoader)

            // Only the loader globals normally initialized before mod construction.
            //? if lp: <=3.0 {
            /*FMLPaths.loadAbsolutePaths(directory)
            FMLConfig.load()
            FMLLoader::class.java.getDeclaredField("dist").apply { isAccessible = true }.set(null, Dist.CLIENT)
            //? if lp: <=2.0 {
            val manager = FMLLoader::class.java.getDeclaredField("moduleLayerManager").apply { isAccessible = true }
            manager.set(null, Proxy.newProxyInstance(manager.type.classLoader, arrayOf(manager.type)) { _, method, _ ->
                check(method.name == "getLayer")
                Optional.of(layer)
            })
            //?} else {
            FMLLoader::class.java.getDeclaredField("gameLayer").apply { isAccessible = true }.set(null, layer)
            //?}
            *///?}
        }
    }

    //? if lp: <=2.0 {
    /*@Test
    fun `class entrypoint returns its constructed instance and matches by identity`() {
        val container = container(PlainEntrypoint::class.java.name)
        assertNull(container.mod)
        assertFalse(container.matches(null))
        construct(container)
        val instance = container.mod!!
        assertEquals(PlainEntrypoint::class.java.name, instance.javaClass.name)
        assertTrue(container.matches(instance))
        assertFalse(container.matches(instance.javaClass))
        assertFalse(container.matches(instance.javaClass.getConstructor().newInstance()))
        assertFalse(container.matches(null))
    }

    @Test
    fun `object entrypoint retains the singleton instance`() {
        val container = container(SingletonEntrypoint::class.java.name)
        construct(container)
        val singletonClass = Class.forName(layer.findModule(MODULE).orElseThrow(), SingletonEntrypoint::class.java.name)
        val singleton = singletonClass.getField("INSTANCE").get(null)
        assertSame(singleton, container.mod)
        assertTrue(container.matches(singleton))
        assertFalse(container.matches(singletonClass))
    }

    @Test
    fun `all entrypoint instances match while getMod returns the first`() {
        val container = container(InjectedEntrypoint::class.java.name, SingletonEntrypoint::class.java.name)
        construct(container)
        val instance = container.mod!!
        assertEquals(InjectedEntrypoint::class.java.name, instance.javaClass.name)
        assertSame(container, instance.javaClass.getMethod("getContainer").invoke(instance))
        assertSame(container, instance.javaClass.getMethod("getKotlinContainer").invoke(instance))
        assertSame(container.modBus, instance.javaClass.getMethod("getBus").invoke(instance))
        val singletonClass = Class.forName(layer.findModule(MODULE).orElseThrow(), SingletonEntrypoint::class.java.name)
        assertTrue(container.matches(singletonClass.getField("INSTANCE").get(null)))
    }

    @Test
    fun `empty container has no instance`() {
        val container = container()
        construct(container)
        assertNull(container.mod)
        assertFalse(container.matches(Any()))
        assertFalse(container.matches(null))
    }

    private fun construct(container: KotlinModContainer) {
        val field = ModContainer::class.java.getDeclaredField("activityMap").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val activities = field.get(container) as Map<ModLoadingStage, Runnable>
        activities.getValue(ModLoadingStage.CONSTRUCT).run()
    }
    *///?}

    //? if lp: <=3.0 {
    /*@Test
    @Suppress("DEPRECATION")
    fun `loading context extension is the context of the active container`() {
        val first = container()
        val second = container()
        val loadingContext = ModLoadingContext.get()
        try {
            loadingContext.setActiveContainer(first)
            assertSame(first.context, loadingContext.extension<KlfLoadingContext>())
            assertSame(first.context, KlfLoadingContext.get())
            loadingContext.setActiveContainer(second)
            assertSame(second.context, loadingContext.extension<KlfLoadingContext>())
            assertSame(second.context, KlfLoadingContext.get())
        } finally {
            loadingContext.setActiveContainer(null)
        }
    }
    *///?}

    @Test
    fun `config load and reload reach only the owning mod bus exactly once`() {
        val owner = container()
        val other = container()
        var loads = 0
        var reloads = 0
        var otherEvents = 0
        // Forge normally generates listener tables while transforming event classes.
        // Initialize those same tables for the untransformed headless classpath.
        //? if forge {
        /*val listenerTable = net.minecraftforge.eventbus.api.EventListenerHelper::class.java
            .getDeclaredMethod("getListenerListInternal", Class::class.java, Boolean::class.javaPrimitiveType)
            .apply { isAccessible = true }
        listOf(Loading::class.java.superclass, Loading::class.java, Reloading::class.java).forEach {
            listenerTable.invoke(null, it, true)
        }
        *///?}
        owner.modBus.addListener(EventPriority.NORMAL, false, Loading::class.java) { loads++ }
        owner.modBus.addListener(EventPriority.NORMAL, false, Reloading::class.java) { reloads++ }
        other.modBus.addListener(EventPriority.NORMAL, false, Loading::class.java) { otherEvents++ }
        other.modBus.addListener(EventPriority.NORMAL, false, Reloading::class.java) { otherEvents++ }

        //? if lp: <=3.0 {
        /*val spec = metadata(IConfigSpec::class.java)
        val config = ModConfig(ModConfig.Type.COMMON, spec, owner, "klf-test-${System.nanoTime()}.toml")
        val loading = Loading(config)
        val reloading = Reloading(config)
        //? if forge {
        owner.dispatchConfigEvent(loading)
        owner.dispatchConfigEvent(Reloading(config))
        owner.dispatchConfigEvent(reloading)
        //?} else {
        loading.post()
        Reloading(config).post()
        reloading.post()
        //?}
        *///?} else {
        owner.acceptEvent(Loading(null))
        owner.acceptEvent(Reloading(null))
        owner.acceptEvent(Reloading(null))
        //?}
        assertEquals(1, loads)
        assertEquals(2, reloads)
        assertEquals(0, otherEvents)
    }

    private fun container(vararg entrypoints: String) =
        KotlinModContainer(metadata(IModInfo::class.java), entrypoints.toList(), layer, ModFileScanData())

    // Metadata and the config spec are test doubles; containers and event buses are real.
    private fun <T> metadata(type: Class<T>): T = type.cast(
        Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ ->
            when (method.name) {
                "getModId", "getDisplayName" -> "klf_container_test"
                "moduleName", "getId" -> MODULE
                "getConfigElement" -> Optional.of("NONE")
                "getFileProperties" -> emptyMap<String, Any>()
                "isEmpty" -> false
                else -> if (method.returnType.isInterface) metadata(method.returnType)
                    else error("Unexpected metadata access: $method")
            }
        }
    )
}
