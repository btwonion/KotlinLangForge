package dev.nyon.klf

import dev.nyon.klf.mv.*
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.FileSystem
import java.nio.file.spi.FileSystemProvider
import java.net.URI
import java.util.Properties
import java.util.jar.JarOutputStream
import kotlin.test.*

class LoadingDiagnosticsTest {
    private val allowedTypes = setOf(IEventBus::class.java, ModContainer::class.java, KotlinModContainer::class.java, Dist::class.java)

    @Test
    fun `compatible versions and absent optional metadata retain their behavior`() {
        assertNull(MinecraftVersionCompatibility.findIncompatibility("1.20.6", KlfBuildInfo("3.0", setOf("1.20.6")), null))
        val message = MinecraftVersionCompatibility.findIncompatibility("unknown version", KlfBuildInfo("3.0", setOf("1.20.6")), null)!!.message!!
        assertContains(message, "compatibility line 3.0")
        assertContains(message, "?g=unknown+version")
        assertFalse(message.contains("null"))
        assertFalse(message.contains("Choose compatibility line"))
    }

    @Test
    fun `wrong version explains installed artifact and exact replacement`() {
        val message = MinecraftVersionCompatibility.findIncompatibility(
            "1.20.4", KlfBuildInfo("3.0", setOf("1.20.6", "1.21.1")), "2.14.1-k2.4.20-3.0+neoforge",
            "neoforge", "KotlinLangForge-2.14.1-k2.4.20-3.0+neoforge.jar"
        )!!.message!!
        listOf("2.14.1-k2.4.20-3.0+neoforge", "1.20.6, 1.21.1", "1.20.4 on neoforge",
            "Installed artifact: KotlinLangForge-2.14.1-k2.4.20-3.0+neoforge.jar", "Remove this", "Choose compatibility line 2.0",
            "https://modrinth.com/mod/kotlin-lang-forge/versions?g=1.20.4&l=neoforge").forEach { assertContains(message, it) }
    }

    @Test
    fun `compatibility guidance respects loader and Minecraft version boundaries`() {
        val build = KlfBuildInfo("2.0", setOf("1.20.4"))
        for ((loader, mc, line) in listOf(Triple("forge", "1.16.5", "1.0"), Triple("forge", "1.20.1", "2.0"),
            Triple("neoforge", "1.20.6", "3.0"), Triple("neoforge", "1.21.8", "3.0"),
            Triple("neoforge", "1.21.9", "3.1"), Triple("neoforge", "26.3", "3.1"))) {
            assertContains(MinecraftVersionCompatibility.findIncompatibility(mc, build, null, loader)!!.message!!, "Choose compatibility line $line for $loader")
        }
        assertFalse(MinecraftVersionCompatibility.findIncompatibility("26.3", build, null, "forge")!!.message!!.contains("Choose compatibility line"))
    }

    @Test
    fun `legacy and current FML accessors are both invoked reflectively`() {
        assertEquals("1.20.4", MinecraftVersionCompatibility.findMinecraftVersion(LegacyLoader::class.java))
        assertEquals("1.21.11", MinecraftVersionCompatibility.findMinecraftVersion(CurrentLoader::class.java))
        assertFailsWith<NoSuchMethodException> { MinecraftVersionCompatibility.findMinecraftVersion(Any::class.java) }
    }

    @Test
    fun `artifact filename supports file and installed union filesystem without assuming a jar in development`() {
        val jar = Files.createTempFile("KotlinLangForge test ", ".jar")
        try {
            JarOutputStream(Files.newOutputStream(jar)).use { }
            assertEquals(jar.fileName.toString(), MinecraftVersionCompatibility.findArtifactFilename(jar.toUri()))
            val unionProvider = FileSystemProvider.installedProviders().firstOrNull { it.scheme == "union" }
            if (unionProvider != null) {
                val factory = unionProvider.javaClass.methods.single { method ->
                    method.name == "newFileSystem" && method.parameterCount == 2 && method.parameterTypes[1] == Array<Path>::class.java
                }
                (factory.invoke(unionProvider, null, arrayOf(jar)) as FileSystem).use { fs ->
                    assertEquals(jar.fileName.toString(), MinecraftVersionCompatibility.findArtifactFilename(fs.rootDirectories.first().toUri()))
                }
            }
            assertNull(MinecraftVersionCompatibility.findArtifactFilename(jar.parent.toUri()))
            assertNull(MinecraftVersionCompatibility.findArtifactFilename(URI("https://example.invalid/klf.jar")))
            assertNull(MinecraftVersionCompatibility.findArtifactFilename(null))
        } finally { Files.deleteIfExists(jar) }
    }

    @Test
    fun `replacement guidance covers every version in the current release configurations`() {
        val root = Path.of(System.getProperty("klf.repositoryRoot"))
        val versions = root.resolve("klf/versions")
        Files.list(versions).use { directories ->
            directories.filter { Files.exists(it.resolve("gradle.properties")) }.forEach { directory ->
                val properties = Properties().apply { Files.newInputStream(directory.resolve("gradle.properties")).use(::load) }
                val loader = properties.getProperty("loom.platform")
                val line = directory.fileName.toString().substringBefore('-')
                properties.getProperty("vers.supportedMcVersions").split(',').forEach { mc ->
                    val message = MinecraftVersionCompatibility.findIncompatibility(mc.trim(), KlfBuildInfo("test", emptySet()), null, loader)!!.message!!
                    assertContains(message, "Choose compatibility line $line for $loader")
                }
            }
        }
    }

    @Test
    fun `objects and supported constructors are accepted`() {
        assertNull(findModConstructor(ObjectMod::class.java, allowedTypes))
        assertEquals(0, findModConstructor(EmptyMod::class.java, allowedTypes)!!.parameterCount)
        assertEquals(4, findModConstructor(FullMod::class.java, allowedTypes)!!.parameterCount)
    }

    @Test
    fun `invalid constructors describe class and accepted arguments`() {
        for (clazz in listOf(PrivateMod::class.java, MultipleMod::class.java)) {
            val message = assertFailsWith<IllegalArgumentException> { findModConstructor(clazz, allowedTypes) }.message!!
            assertContains(message, clazz.name)
            assertContains(message, "exactly one public constructor")
            assertContains(message, "found ${clazz.constructors.size}")
        }
        for ((clazz, reason) in listOf(UnsupportedMod::class.java to "unsupported", DuplicateMod::class.java to "duplicate")) {
            val message = assertFailsWith<IllegalArgumentException> { findModConstructor(clazz, allowedTypes) }.message!!
            assertContains(message, clazz.name)
            assertContains(message, reason)
            assertContains(message, "at most once each, in any order")
            allowedTypes.forEach { assertContains(message, it.name) }
        }
    }

    @Test
    fun `annotated subscriber errors include method and accepted signature`() {
        for (name in listOf("noArguments", "twoArguments", "wrongArgument")) {
            val method = SubscriberObject::class.java.declaredMethods.single { it.name == name }
            val message = assertFailsWith<IllegalArgumentException> { validateAnnotatedSubscriber(method, true) }.message!!
            assertContains(message, "${SubscriberObject::class.java.name}.$name")
            assertContains(message, "exactly one parameter extending ${Event::class.java.name}")
        }
        val method = InstanceSubscriber::class.java.getDeclaredMethod("onEvent", TestEvent::class.java)
        assertContains(assertFailsWith<IllegalArgumentException> { validateAnnotatedSubscriber(method, false) }.message!!, "static method or a Kotlin object method")
        validateAnnotatedSubscriber(SubscriberObject::class.java.getDeclaredMethod("onEvent", TestEvent::class.java), true)
        validateAnnotatedSubscriber(StaticSubscriber::class.java.getDeclaredMethod("onEvent", TestEvent::class.java), false)
    }

    @Test
    fun `subscriber visibility follows the loader reflection rules`() {
        val method = SubscriberObject::class.java.getDeclaredMethod("privateOnEvent", TestEvent::class.java)
        val hiddenClassMethod = HiddenSubscriber::class.java.getDeclaredMethod("onEvent", TestEvent::class.java)
        //? if forge {
        /*for (invalid in listOf(method, hiddenClassMethod)) {
            val message = assertFailsWith<IllegalArgumentException> { validateAnnotatedSubscriber(invalid, true) }.message!!
            assertContains(message, invalid.declaringClass.name)
            assertContains(message, invalid.name)
            assertContains(message, "Forge requires a public method in a public class")
        }
        *///?} else {
        validateAnnotatedSubscriber(method, true)
        validateAnnotatedSubscriber(hiddenClassMethod, true)
        //?}
    }

    @Test
    fun `reflection wrappers expose the thrown developer exception in loader issues`() {
        val rootCause = IllegalStateException("Developer configuration is missing")
        assertSame(rootCause, InvocationTargetException(InvocationTargetException(rootCause)).unwrapInvocationTargetException())
        val missingCause = InvocationTargetException(null)
        assertSame(missingCause, missingCause.unwrapInvocationTargetException())
        val fileType = IModFileInfo::class.java.getMethod("getFile").returnType
        val file = Proxy.newProxyInstance(fileType.classLoader, arrayOf(fileType)) { _, method, _ ->
            if (method.name == "getFilePath") Path.of("diagnosticstest.jar") else null
        }
        val fileInfo = Proxy.newProxyInstance(IModFileInfo::class.java.classLoader, arrayOf(IModFileInfo::class.java)) { _, method, _ ->
            if (method.name == "getFile") file else null
        }
        val modInfo = Proxy.newProxyInstance(IModInfo::class.java.classLoader, arrayOf(IModInfo::class.java)) { _, method, _ ->
            when (method.name) { "getModId" -> "diagnosticstest"; "toString" -> "diagnosticstest"; "getOwningFile" -> fileInfo; else -> null }
        } as IModInfo
        val exception = modLoadingException(InvocationTargetException(rootCause), modInfo)
        //? if lp: <=2.0 {
        /*assertSame(rootCause, exception.cause)
        *///?} else {
        assertSame(rootCause, exception.issues.single().cause)
        assertSame(rootCause, exception.issues.single().translationArgs.single())
        //?}
    }

    class VersionInfo(private val mc: String) { fun mcVersion() = mc }
    class LegacyLoader { companion object { @JvmStatic fun versionInfo() = VersionInfo("1.20.4") } }
    class CurrentLoader {
        fun getVersionInfo() = VersionInfo("1.21.11")
        companion object { @JvmStatic fun getCurrent() = CurrentLoader() }
    }
    object ObjectMod
    class EmptyMod
    class FullMod(bus: IEventBus, container: ModContainer, kotlinContainer: KotlinModContainer, dist: Dist)
    class PrivateMod private constructor()
    class MultipleMod { constructor(); constructor(bus: IEventBus) }
    class UnsupportedMod(value: String)
    class DuplicateMod(first: Dist, second: Dist)
    class TestEvent : Event()
    object SubscriberObject {
        @SubscribeEvent fun noArguments() {}
        @SubscribeEvent fun twoArguments(first: TestEvent, second: TestEvent) {}
        @SubscribeEvent fun wrongArgument(value: String) {}
        @SubscribeEvent fun onEvent(event: TestEvent) {}
        @SubscribeEvent private fun privateOnEvent(event: TestEvent) {}
    }
    private object HiddenSubscriber { @SubscribeEvent fun onEvent(event: TestEvent) {} }
    class InstanceSubscriber { @SubscribeEvent fun onEvent(event: TestEvent) {} }
    object StaticSubscriber { @JvmStatic @SubscribeEvent fun onEvent(event: TestEvent) {} }
}
