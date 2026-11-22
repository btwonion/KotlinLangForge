
- forge (1.20.1 only): the early compatibility patch now covers every jar that bundles Kotlin packages KLF provides, not only Kotlin for Forge
  - mods and jars without an explicit module-info are patched: they stay loaded and only lose the overlapping packages, so a mod's ID stays available to the mods that depend on it; their own service providers (for example a language provider) are kept
  - libraries with a module-info (never mods, those always have a mods.toml) that only contain packages KLF provides, such as a kotlin-stdlib shipped through jar-in-jar, are removed from the mod list, unless another module requires them
  - the empty kotlin-stdlib-jdk7/-jdk8 libraries are removed together with the kotlin-stdlib they require
  - jars that can't be patched are logged and skipped instead of crashing the game
- forge (1.20.1 only): fix the crash with Essential (`Module kotlin.stdlib not found, required by kotlin.stdlib.jdk7`): empty Kotlin modules whose required module is gone are dropped when the module layer is built
