
- forge: the early compatibility patch now covers every jar that bundles Kotlin packages KLF provides, not only Kotlin for Forge
  - jars without an explicit module-info and normal mods lose only the overlapping packages; their own service providers (for example a language provider) are kept
  - jars with a module-info that only contain packages KLF provides are dropped from the mod list, unless another module requires them
  - jars that can't be patched are logged and skipped instead of crashing the game
