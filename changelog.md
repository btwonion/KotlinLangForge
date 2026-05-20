- Use NeoForge's `net.neoforged.neoforge.internal.NeoForgeBindings` instead of `net.neoforged.fml.Bindings` for lp <=
  2.0
    - The `Bindings` class is no longer shipped with the latest version of 1.21.1 leading to a bug when registering
      events
    - fix [**#116**](https://github.com/btwonion/KotlinLangForge/issues/116)
- Migrate to PreloadingTricks' relocated `LexForgeManager` for 2.0-forge-compat