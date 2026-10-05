# KotlinLangForge

> Provides a Kotlin language adapter for Forge and NeoForge

KotlinLangForge (KLF) supplies the Kotlin runtime, a language adapter, and libraries for mods written in Kotlin.
Install it only when a mod or modpack requires it; KLF adds no gameplay features on its own.

## Player installation

1. Check **both your Minecraft version and your mod loader (Forge or NeoForge)** in your launcher or server setup.
   Download a matching KLF release from [Modrinth](https://modrinth.com/mod/kotlin-lang-forge/versions),
   [CurseForge](https://www.curseforge.com/minecraft/mc-mods/kotlinlangforge/files), or
   [GitHub releases](https://github.com/btwonion/KotlinLangForge/releases).
   Filter by both Minecraft version and loader, then check the selected file's supported versions and dependencies.
2. **Forge also requires [Preloading Tricks](https://modrinth.com/mod/preloading-tricks)**
   ([CurseForge](https://www.curseforge.com/minecraft/mc-mods/preloading-tricks)).
   Choose its matching Minecraft/Forge file and install it alongside KLF. A launcher may install dependencies
   automatically; check that it has installed this one. This requirement applies to the Forge distribution.
3. Put the downloaded `.jar` files in the **`mods` folder of the launcher instance you actually use**.
   For a dedicated server, use that server's `mods` folder. Keep the jars intact; do not extract them.
4. Follow the mod that requires KLF for **client/server placement**: install KLF on the client for a client mod,
   on the server for a server mod, and on both when the dependent mod requires both. Install required dependencies
   on the same side(s), then restart the game or server.

### Choose a compatible download

The table below is generated from `klf/versions/*/gradle.properties` and lists the **exact Minecraft versions
declared by the variants in this checkout**. It is not a list of all historical releases or a promise that every
branch version is already published. Always confirm availability and requirements on the selected download page.

| Compatibility line | Loader | Declared Minecraft versions | Download name for this checkout |
| --- | --- | --- | --- |
| 2.0 | Forge | 1.17.1, 1.18.2, 1.19.2, 1.19.4, 1.20.1, 1.20.2, 1.20.3, 1.20.4 | `v2.14.1-k2.4.20-2.0+forge` |
| 2.0 | NeoForge | 1.20.2, 1.20.3, 1.20.4 | `v2.14.1-k2.4.20-2.0+neoforge` |
| 3.0 | NeoForge | 1.20.5, 1.20.6, 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8 | `v2.14.1-k2.4.20-3.0+neoforge` |
| 3.1 | NeoForge | 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | `v2.14.1-k2.4.20-3.1+neoforge` |

For example, **Minecraft 1.20.1 uses the Forge 2.0 variant** in this checkout; the NeoForge 2.0 variant lists only
1.20.2, 1.20.3, and 1.20.4. Forge and NeoForge downloads are not interchangeable.
For older Minecraft versions such as 1.16.5, look for a matching **historical release** on the download pages
instead of choosing a current variant by a broad version range.

### Read the download name

A release name such as `v2.14.1-k2.4.20-2.0+forge` contains three different version numbers:

- `2.14.1` is the **KLF release version**. Use a release that meets your dependent mod's KLF requirement.
- `k2.4.20` is the **bundled Kotlin version**, not a Minecraft version. Respect any Kotlin requirement
  stated by the dependent mod.
- `2.0` is the **KLF compatibility line** (also called the language provider version). Match it using the table above.
  It is not the KLF release version or a Forge/NeoForge version.
- `+forge` or `+neoforge` identifies the **mod loader**.

The jar uses the same version suffix. The published Forge player jar is named
`KotlinLangForge-kff-compat-2.14.1-k2.4.20-2.0+forge.jar`; it includes KLF and its compatibility service.
NeoForge player jars use the `KotlinLangForge-` prefix. Choose the main player download rather than a
`-sources`, `-dev`, or `-shadow` development artifact.

If startup reports a missing `klf` language provider, check the instance's `mods` folder, both version/loader
matches, and (on Forge) Preloading Tricks before restarting.

## Developer usage

To add your language adapter to your mod, add the following lines to your
(neoforge.)mods.toml.

**neoforge.mods.toml**

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

The "language provider version" is KLF's compatibility line, as explained in
[Choose a compatible download](#choose-a-compatible-download). It distinguishes language provider implementations
across Minecraft versions and is not a version number supplied by Forge or NeoForge.
Set `lpVersion` to the matching compatibility line and `loader` to `forge` or `neoforge` in the dependency below.

**build.gradle.kts**

```kotlin
repositories {
    maven("https://repo.nyon.dev/releases")
}

dependencies {
    implementation("dev.nyon:KotlinLangForge:2.14.1-k2.4.20-$lpVersion+$loader")
}
```

*For lp: <=3.0, you will have to use `modImplementation`.*

### Events

To use automatic event listener registration, the `@EventBusSubscriber` annotation has to be added on the class/file.
Klf then will automatically find all methods that have events in their parameters and will determine which event bus to
use.
Additionally, you can annotate a method with `@SubscribeEvent` to adjust the listener's parameters.

**Note for Forge developers:** Private event listeners cannot be processed on Forge and will result in a crash!

**Mod Bus:** The mod bus is available via the top-level declaration `dev.nyon.klf.MOD_BUS`.

## Included Libraries

- org.jetbrains.kotlin:kotlin-stdlib:2.4.20
- org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.4.20
- org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.4.20
- org.jetbrains.kotlin:kotlin-reflect:2.4.20
- org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0
- org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0
- org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.11.0
- org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0
- org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.11.0
- org.jetbrains.kotlinx:kotlinx-datetime:0.8.0-0.6.x-compat
- org.jetbrains.kotlinx:kotlinx-io-core:0.9.1
- org.jetbrains.kotlinx:kotlinx-io-bytestring:0.9.1
- org.jetbrains.kotlinx:atomicfu:0.33.0

### Other

If you need help with any of my mods, just join my [discord server](https://nyon.dev/discord).
