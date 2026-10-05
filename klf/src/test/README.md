# Container regression tests

Run the headless suite for every configured loader:

```sh
./gradlew :klf:2.0-forge:test :klf:2.0-neoforge:test :klf:3.0-neoforge:test :klf:3.1-neoforge:test --max-workers=1
```

The legacy suites check constructed class instances, Kotlin object instances,
identity matching (including an entrypoint with permissive `equals`), multiple
entrypoints, constructor injection, and empty containers. `getMod()` returns the
first constructed entrypoint; `matches()` recognizes every retained instance.
The loaders with `contextExtension` also check `ModLoadingContext.extension()`
and `KlfLoadingContext.get()` while switching active containers.

The config test dispatches one loading event and two distinct reloading events
and expects exactly one load, two reloads, and no delivery to a second container.
Forge uses `dispatchConfigEvent()`. NeoForge 2.0 and 3.0 use the real config
event's `post()` method. NeoForge 3.1 exercises `ModContainer.acceptEvent()`,
the entry point used by its config system.

Fixtures run in a named automatic module so entrypoint loading goes through the
same `ModuleLayer` and `Class.forName(module, name)` path as production. Metadata
and config specifications are lightweight proxies. A test binding provider
supplies the loader's actual config event factories, and FML configuration is
initialized in a temporary directory. Forge's listener tables are initialized
explicitly because the headless JVM does not run its event-class transformer.
These tests do not launch Minecraft or exercise its file watcher.

## API evidence

The cached build dependencies were inspected for Forge 1.20.1-47.4.4 and
NeoForge's FML 2.0.17, 3.0.45 and 10.0.14. The official Forge source branches
[1.17.x](https://github.com/MinecraftForge/MinecraftForge/blob/1.17.x/javafmllanguage/src/main/java/net/minecraftforge/fml/javafmlmod/FMLModContainer.java),
[1.18.x](https://github.com/MinecraftForge/MinecraftForge/blob/1.18.x/javafmllanguage/src/main/java/net/minecraftforge/fml/javafmlmod/FMLModContainer.java),
[1.19.2](https://github.com/MinecraftForge/MinecraftForge/blob/1.19.2/javafmllanguage/src/main/java/net/minecraftforge/fml/javafmlmod/FMLModContainer.java),
[1.19.x](https://github.com/MinecraftForge/MinecraftForge/blob/1.19.x/javafmllanguage/src/main/java/net/minecraftforge/fml/javafmlmod/FMLModContainer.java),
[1.20.1](https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/javafmllanguage/src/main/java/net/minecraftforge/fml/javafmlmod/FMLModContainer.java),
[1.20.x](https://github.com/MinecraftForge/MinecraftForge/blob/1.20.x/javafmllanguage/src/main/java/net/minecraftforge/fml/javafmlmod/FMLModContainer.java)
and [1.20.4](https://github.com/MinecraftForge/MinecraftForge/blob/1.20.4/javafmllanguage/src/main/java/net/minecraftforge/fml/javafmlmod/FMLModContainer.java)
all retain the constructed object, match by identity, initialize the config
handler to post `event.self()`, and supply a context instance. Their base
containers leave the config handler empty.

Legacy NeoForge retains the constructed object and supplies a context instance,
but config events post through `getEventBus()` without a Forge config handler.
Newer NeoForge supplies config events through `acceptEvent()` and no longer has
instance lookup or a `contextExtension` field. The instance collection and Forge
handler are therefore limited to their respective legacy targets; the optional
context-field lookup remains compatible with newer loader implementations.
