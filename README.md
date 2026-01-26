# KotlinLangForge

> Provides a Kotlin language adapter for Forge and Neoforge

This mod adds a language adapter for Kotlin and provides multiple libraries.

## Developer usage

To add your language adapter to your mod, add the following lines to your
(neoforge.)mods.toml.

**mods.toml**

```toml
modLoader = "klf"
loaderVersion = "[1,)"
```

Now you can init your mod like any other.
Just make sure your `@Mod` class is either an object or a class with a public constructor.
The constructor can take the following four arguments (they should never duplicate):

- IEventBus
- ModContainer
- KotlinModContainer
- Dist

If you want to implement the libraries in your mod, import the following dependency,
matching the language provider version, your loader and the (latest) version of Kotlin.

**Versioning**

The "language provider version" is a version only provided by KotlinLangForge.
This format is not used by Forge or NeoForge and only serves as a simple differentiation variable between the different
language provider implementations over the course of the versions of Minecraft.

| version of Minecraft | language provider version | supported loaders |
|----------------------|---------------------------|-------------------|
| 1.16.5               | 1.0                       | Forge             |
| 1.17.1 - 1.20.4      | 2.0                       | Forge, NeoForge   |
| 1.20.5 - 1.21.8      | 3.0                       | NeoForge          |
| 1.21.9 - 1.21.x      | 3.1                       | NeoForge          |

**build.gradle.kts**

```kotlin
repositories {
    maven("https://repo.nyon.dev/releases")
}

dependencies {
    modImplementation("dev.nyon:KotlinLangForge:2.11.2-2.3.0-$lpVersion+$loader")
}
```

### Events

To use automatic event listener registration the `@EventBusSubscriber` annotation has to be added on the class/file.
Additionally, you can annotate a method with `SubscribeEvent` to adjust the listener's parameters.\
This is not necessary though!
Klf automatically looks for events in every method inside of the class and automatically
determines which event bus to use.

**Note for Forge developers:** Private event listeners cannot be processed on Forge and will result in a crash!

**Mod Bus** The mod bus is available by `dev.nyon.klf.MOD_BUS`.

## Included Libraries

- org.jetbrains.kotlin:kotlin-stdlib:2.3.0
- org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.3.0
- org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.3.0
- org.jetbrains.kotlin:kotlin-reflect:2.3.0
- org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0
- org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0
- org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.9.0
- org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2
- org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.10.2
- org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-0.6.x-compat
- org.jetbrains.kotlinx:kotlinx-io-core:0.8.0
- org.jetbrains.kotlinx:kotlinx-io-bytestring:0.8.0
- org.jetbrains.kotlinx:atomicfu:0.29.0

### Other

If you need help with any of my mods, just join my [discord server](https://nyon.dev/discord).
