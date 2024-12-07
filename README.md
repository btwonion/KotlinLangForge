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
Just make sure your `@Mod` class is either an object or a class with a public
and empty constructor.

If you want to implement the libraries in your mod, import the following dependency,
matching the language loader version of your version of Minecraft, your loader and the (latest) version of Kotlin.

**Versioning**

The "language provider version" is a version only provided by KotlinLangForge.
This format is not used by Forge or NeoForge and only serves as a simple differentiation variable between the different
language provider implementations over the course of the versions of Minecraft.

| version of Minecraft | language provider version |
|----------------------|---------------------------|
| 1.16.5               | 1.0                       |
| 1.17.1 - 1.20.4      | 2.0                       |
| 1.20.5 - 1.21.x      | 3.0                       |

**build.gradle.kts**

```kotlin
repositories {
    maven("https://repo.nyon.dev/releases")
}

dependencies {
    modImplementation("dev.nyon:KotlinLangForge:$version-$kotlinVersion-$lpVersion+$loader")
}
```

### Mod Bus

To use the mod bus, just implement `dev.nyon.klf.MOD_BUS`.

## Libraries

- org.jetbrains.kotlin:kotlin-stdlib
- org.jetbrains.kotlin:kotlin-stdlib-jdk8
- org.jetbrains.kotlin:kotlin-stdlib-jdk7
- org.jetbrains.kotlin:kotlin-reflect
- org.jetbrains.kotlinx:kotlinx-serialization-core
- org.jetbrains.kotlinx:kotlinx-serialization-json
- org.jetbrains.kotlinx:kotlinx-serialization-cbor
- org.jetbrains.kotlinx:kotlinx-coroutines-core
- org.jetbrains.kotlinx:kotlinx-coroutines-jdk8
- org.jetbrains.kotlinx:kotlinx-datetime
- org.jetbrains.kotlinx:kotlinx-io-core
- org.jetbrains.kotlinx:kotlinx-io-bytestring
- org.jetbrains.kotlinx:atomicfu

### Other

If you need help with any of my mods, just join my [discord server](https://nyon.dev/discord).
