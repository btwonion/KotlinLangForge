# KotlinLangForge

> Provides Forge/Neoforge language adapter for Kotlin

This mod adds a language adapter for Kotlin and provides multiple libraries.

## Developer usage
To add your language adapter to your mod, add the following lines to your 
(neoforge.)mods.toml.

**mods.toml**
```toml
modLoader = "klf"
loaderVersion = "[1,)"
```

Now you can init your mod like any other. Just make sure your @Mod class is either a object or a class with a public 
and empty constructor.

If you want to implement the libraries in your mod, just import the following dependency, 
matching your version of Minecraft, your loader and the (latest) version of Kotlin.

**build.gradle.kts**
```kotlin
repositories {
	maven("https://repo.nyon.dev/releases")
}

dependencies {
	implementation("dev.nyon:kotlin-lang-forge:$version-$kotlinVersion-$mcVersion+$loader")
}
```

## Libraries
- org.jetbrains.kotlin:kotlin-stdlib
- org.jetbrains.kotlin:kotlin-stdlib-jdk8
- org.jetbrains.kotlin:kotlin-stdlib-jdk7
- org.jetbrains.kotlin:kotlin-reflect
- org.jetbrains.kotlinx:kotlinx-serialization-core
- org.jetbrains.kotlinx:kotlinx-serialization-json
- org.jetbrains.kotlinx:kotlinx-coroutines-core
- org.jetbrains.kotlinx:kotlinx-datetime
- org.jetbrains.kotlinx:kotlinx-io-core
- org.jetbrains.kotlinx:atomicfu
- org.jetbrains.kotlinx:kotlinx-datetime

### Other

If you need help with any of my mods just join my [discord server](https://nyon.dev/discord).
