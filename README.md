# ForgeLangKotlin

> Provides Forge/Neoforge language adapter for Kotlin

This mod adds a language adapter for Kotlin and provides multiple libraries.

## Developer usage
You don't have to implement any dependency to your project. As long as you add the following lines to your 
(neoforge.)mods.toml you'll be good.

**mods.toml**
```toml
modLoader = "flk"
loaderVersion = "[1,)"
```

Now you can init your mod like any other. Just make sure your Mod class is either a Object or a class with a public 
and empty constructor.

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
