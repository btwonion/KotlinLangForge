
- forge: the early compatibility patch now covers every jar that bundles Kotlin packages KLF provides, not only Kotlin for Forge
  - mods and jars without an explicit module-info are patched: they stay loaded and only lose the overlapping packages, so a mod's ID stays available to the mods that depend on it; their own service providers (for example a language provider) are kept
  - libraries with a module-info (never mods, those always have a mods.toml) that only contain packages KLF provides, such as a kotlin-stdlib shipped through jar-in-jar, are removed from the mod list, unless another module requires them
  - jars that can't be patched are logged and skipped instead of crashing the game
