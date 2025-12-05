- add compatibility for KotlinForForge on Forge 1.20.1
    - the mod is now loaded differently and modifies the metadata of KFF to not include dependencies KLF already
      provides
    - on lpVersion == 2.0 the mod now requires [preloading-tricks](https://modrinth.com/mod/preloading-tricks) to work