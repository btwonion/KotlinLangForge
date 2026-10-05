# Developing a Kotlin mod with KotlinLangForge

KotlinLangForge (KLF) supplies the `klf` language loader and Kotlin libraries. Your mod still uses Forge or NeoForge's annotations, events, and game APIs. The examples below use **NeoForge 21.11.42 / Minecraft 1.21.11**, KLF **2.14.1**, Kotlin **2.4.20**, and KLF compatibility line **3.1**. A separate Forge build example targets Minecraft **1.20.1**, Forge **47.4.4**, and line **2.0**.

Use the [installation and compatibility guide](README.md) to choose a released artifact for your exact Minecraft version and loader. The `3.1` / `2.0` suffix is KLF's compatibility line; it is neither the KLF release version nor the Forge/NeoForge version. Pin a published coordinate instead of selecting Maven's latest version across all loaders. Release coordinates can be checked in the [Maven metadata](https://repo.nyon.dev/releases/dev/nyon/KotlinLangForge/maven-metadata.xml).

## Gradle: NeoForge with ModDevGradle

This is a minimal `build.gradle.kts` for the NeoForge example. ModDevGradle supplies the `neoForge` extension and adds Minecraft/NeoForge to the source classpath. Its ordinary `implementation` configuration also makes the KLF dependency available to development runs.

```kotlin
plugins {
    kotlin("jvm") version "2.4.20"
    id("net.neoforged.moddev") version "2.0.141"
}

group = "com.example"
version = "1.0.0"

val klfVersion = "2.14.1"
val kotlinVersion = "2.4.20"
val compatibilityLine = "3.1"
val loader = "neoforge"
val klfCoordinate = "dev.nyon:KotlinLangForge:$klfVersion-k$kotlinVersion-$compatibilityLine+$loader"

repositories {
    mavenCentral()
    maven("https://repo.nyon.dev/releases")
}

neoForge {
    version = "21.11.42"
    mods {
        create("examplemod") { sourceSet(sourceSets.main.get()) }
    }
    runs {
        create("client") { client() }
        create("server") { server() }
    }
}

dependencies {
    implementation(klfCoordinate)
}

kotlin { jvmToolchain(21) }
```

Put `rootProject.name = "examplemod"` in `settings.gradle.kts`. Use a Gradle wrapper supported by your build plugin. KLF supplies Kotlin libraries at runtime; avoid bundling a second copy of the Kotlin runtime into your mod. A development dependency alone does **not** embed KLF in your distributed mod JAR. Declare it as a required dependency in metadata and on the platform where you publish your mod.

## Mod metadata

Save this as `src/main/resources/META-INF/neoforge.mods.toml` for the NeoForge example. The `modId` must exactly match `@Mod("examplemod")`. `loaderVersion` constrains the KLF release version advertised by the loader, not its compatibility line. These are Maven version ranges; `[1.21.11]` means exactly that Minecraft version.

```toml
modLoader = "klf"
loaderVersion = "[2,)"
license = "MIT"

[[mods]]
modId = "examplemod"
version = "1.0.0"
displayName = "Example Kotlin Mod"
description = '''A minimal KotlinLangForge mod.'''

[[dependencies.examplemod]]
modId = "klf"
type = "required"
versionRange = "[2.14,)"
ordering = "NONE"
side = "BOTH"

[[dependencies.examplemod]]
modId = "minecraft"
type = "required"
versionRange = "[1.21.11]"
ordering = "NONE"
side = "BOTH"

[[dependencies.examplemod]]
modId = "neoforge"
type = "required"
versionRange = "[21.11.42,)"
ordering = "NONE"
side = "BOTH"
```

These version ranges describe this example's targets; expand them only for versions you have tested. Loader-specific metadata conventions are described in the [NeoForge mod-file documentation](https://docs.neoforged.net/docs/gettingstarted/modfiles/) and [Forge mod-file documentation](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/modfiles/).

## Minimal object mod

Save this as `src/main/kotlin/com/example/ExampleMod.kt`. Kotlin objects need no public constructor. KLF initializes the object during mod construction.

```kotlin
package com.example

import dev.nyon.klf.MOD_BUS
import net.neoforged.bus.api.EventPriority
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent

@Mod("examplemod")
object ExampleMod {
    init {
        MOD_BUS.addListener(EventPriority.NORMAL, false, FMLCommonSetupEvent::class.java, ::onCommonSetup)
    }

    private fun onCommonSetup(event: FMLCommonSetupEvent) {
        event.enqueueWork { println("Example Kotlin Mod is ready") }
    }
}
```

`MOD_BUS` resolves the **currently active KLF mod container** through `KlfLoadingContext.get()`. Read it while your mod is being constructed, such as in the `init` block above. If you need it later, capture the returned bus during construction. Do not resolve `MOD_BUS` from a worker thread, an unrelated mod's initialization, or a game-event callback: the active loading context may be absent or belong to another mod. Explicit constructor injection is useful when you want to store the bus.

## Class constructor alternative

Replace the object example with this class; keep the same metadata. Only one entrypoint is needed for this example.

```kotlin
package com.example

import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent

@Mod("examplemod")
class ExampleMod(private val modBus: IEventBus, container: ModContainer) {
    init {
        println("Constructing ${container.modId}")
        modBus.addListener(EventPriority.NORMAL, false, FMLCommonSetupEvent::class.java, ::onCommonSetup)
    }

    private fun onCommonSetup(event: FMLCommonSetupEvent) {
        event.enqueueWork { println("Example Kotlin Mod is ready") }
    }
}
```

A class must have **exactly one public JVM constructor**. An empty constructor is valid. Parameters can be any subset of the following types, in any order, with each type appearing at most once:

| Type | Injected value |
| --- | --- |
| `net.neoforged.bus.api.IEventBus` | This mod's event bus |
| `net.neoforged.fml.ModContainer` | This mod's container |
| `dev.nyon.klf.KotlinModContainer` | The same container, with KLF's concrete type |
| `net.neoforged.api.distmarker.Dist` | The physical client or dedicated-server side |

Use the corresponding `net.minecraftforge` types on Forge. `ModContainer` and `KotlinModContainer` are different accepted parameter types and may both appear, but both receive the same instance. Arbitrary services, nullable/defaulted placeholders, and duplicate types are not dependency injection. Default arguments and `@JvmOverloads` can create additional public JVM constructors; inspect the generated constructors if validation rejects your class. Exceptions thrown by your constructor are reported using the underlying exception and message.

## Automatic events and physical sides

As an alternative to explicit `addListener` calls, annotate a Kotlin object with the loader's `@EventBusSubscriber`. KLF selects the bus by event type: events implementing `IModBusEvent` go to your mod bus; other events go to the game bus. On older annotations which expose a `bus` setting, KLF still routes by event type. Set `modid` explicitly, especially for JARs containing multiple mods.

```kotlin
package com.example

import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent

@EventBusSubscriber(modid = "examplemod")
object CommonEvents {
    // KLF automatically registers eligible one-Event-parameter methods,
    // including methods without @SubscribeEvent.
    fun onCommonSetup(event: FMLCommonSetupEvent) {
        event.enqueueWork { println("Common setup") }
    }

    // @SubscribeEvent is optional; use it to set priority or receiveCanceled.
    @SubscribeEvent(priority = EventPriority.LOW)
    fun onLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        println("A player logged in")
    }
}
```

Use either automatic registration or explicit registration for a given listener, since using both invokes it twice. Eligible methods must be static JVM methods (including top-level functions on an annotated file) or methods on Kotlin objects, with exactly one parameter extending the loader's `Event` type. An ordinary class instance is not created for a subscriber. Unannotated methods with a non-event parameter or multiple parameters are helpers, not listeners. Explicitly annotated invalid methods fail with their class, method, and expected signature. Use `receiveCanceled = true` only for cancellable event types supported by your loader.

Place client-only event types in a separate subscriber with a physical-side filter:

```kotlin
package com.example

import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ScreenEvent

@EventBusSubscriber(modid = "examplemod", value = [Dist.CLIENT])
object ClientEvents {
    fun onScreenInit(event: ScreenEvent.Init.Post) {
        println("Client screen initialized")
    }
}
```

The filter is checked before KLF initializes the subscriber class. Keep client-only imports, fields, and handlers out of common entrypoints so a dedicated server can load them. `Dist.CLIENT` denotes a physical client, which can also host an integrated server; it is not a logical-side test. NeoForge's newer `@Mod(dist = [Dist.CLIENT])` supports a separate client entrypoint; that API is unavailable on the older Forge/NeoForge branches, so subscriber side filters are the portable choice.

On **Forge**, use `net.minecraftforge.fml.common.Mod.EventBusSubscriber` and `net.minecraftforge.eventbus.api.SubscribeEvent`. Reflection-based automatic listeners must be **public JVM methods in public classes/objects**; use Kotlin `public` visibility for these APIs. Private methods/classes cannot be called by Forge's automatic reflection path. Kotlin object methods need no `@JvmStatic` for KLF registration. A listener passed explicitly as a function reference can be private because KLF does not invoke it reflectively.

## Gradle: Forge with Architectury Loom

For an existing Architectury Loom project targeting Minecraft 1.20.1, use these concrete values. Loom provides `modImplementation` and remaps mod dependencies. It is not a standard Gradle or ModDevGradle configuration; do not use it in those projects. If using ForgeGradle instead, use its dependency/remapping and run configuration instructions for that plugin version.

In `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.architectury.dev/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
    }
}
rootProject.name = "examplemod"
```

In `gradle.properties`:

```properties
loom.platform=forge
kotlin.stdlib.default.dependency=false
```

The Forge 2.0 artifact already contains the Kotlin standard library. Disabling the Kotlin plugin's default stdlib dependency here avoids duplicate stdlib classes during Loom remapping. This setting belongs to this shaded Forge example.

In `build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "2.4.20"
    id("dev.architectury.loom") version "1.17-SNAPSHOT"
}

group = "com.example"
version = "1.0.0"

val minecraftVersion = "1.20.1"
val forgeVersion = "47.4.4"
val klfVersion = "2.14.1"
val kotlinVersion = "2.4.20"
val compatibilityLine = "2.0"
val loader = "forge"

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net/")
    maven("https://repo.nyon.dev/releases")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    "forge"("net.minecraftforge:forge:$minecraftVersion-$forgeVersion")
    modImplementation("dev.nyon:KotlinLangForge:$klfVersion-k$kotlinVersion-$compatibilityLine+$loader")
}

kotlin { jvmToolchain(17) }
```

Use `META-INF/mods.toml` for this Forge target. Its complete minimal metadata is:

```toml
modLoader = "klf"
loaderVersion = "[2,)"
license = "MIT"

[[mods]]
modId = "examplemod"
version = "1.0.0"
displayName = "Example Kotlin Mod"
description = '''A minimal KotlinLangForge mod.'''

[[dependencies.examplemod]]
modId = "klf"
mandatory = true
versionRange = "[2.14,)"
ordering = "NONE"
side = "BOTH"

[[dependencies.examplemod]]
modId = "minecraft"
mandatory = true
versionRange = "[1.20.1]"
ordering = "NONE"
side = "BOTH"

[[dependencies.examplemod]]
modId = "forge"
mandatory = true
versionRange = "[47.4.4,)"
ordering = "NONE"
side = "BOTH"
```

Change the Kotlin source imports to Forge's corresponding packages. A distributed Forge installation also needs **Preloading Tricks** as described in the installation guide; adding a Maven compile dependency does not install that player dependency or the Forge compatibility distribution. Test the released Forge artifact and its required dependencies together before publishing. Configure embedding only if your build plugin and KLF's distribution support it; the snippets above intentionally use a separate required KLF installation.

## Troubleshooting and checks

- A wrong-Minecraft error reports the installed KLF compatibility line, supported versions, full build version, and artifact filename when available. Replace that file with one listed for the running Minecraft version **and** loader. A filtered download page with no matching file means you should choose a supported combination.
- A missing `klf` language provider means the matching KLF provider is absent or not discoverable in the development/runtime environment. Check your dependency and run configuration, metadata filename, loader, and Forge's additional installation requirements.
- A constructor error names the mod class and accepted constructor types. Fix the JVM constructor count/type before debugging event handlers.
- A subscriber error names the annotated method. Give it one event parameter and use a Kotlin object or static method; on Forge also make the method and declaring class public.

Check both a client launch and a dedicated-server launch, including client-only subscribers. Use the game bus for gameplay events and the mod bus for lifecycle/registration events. Follow your loader's threading rules (for example, lifecycle `enqueueWork`) when handling events.

When contributing to KLF itself, the focused diagnostics tests live in `klf/src/test/kotlin/dev/nyon/klf/LoadingDiagnosticsTest.kt`. Run `./gradlew :klf:3.1-neoforge:test` for the active source version, or `./gradlew :klf:2.0-forge:test :klf:2.0-neoforge:test :klf:3.0-neoforge:test :klf:3.1-neoforge:test` for all four branches. Stonecutter generates the appropriate main and test sources for each target. Historical reports [#112](https://github.com/btwonion/KotlinLangForge/issues/112), [#116](https://github.com/btwonion/KotlinLangForge/issues/116), and [#131](https://github.com/btwonion/KotlinLangForge/issues/131) are resolved regression references, not a list of current unresolved failures.
