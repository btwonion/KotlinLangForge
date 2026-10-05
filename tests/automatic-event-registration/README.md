Run from the repository root with JDK 21:

```sh
./gradlew -p tests/automatic-event-registration check
```

This isolated test project compiles the current production `AutomaticEventSubscriber.kt` and
`KotlinModContainer.kt` directly. It uses the real NeoForge event bus 8.0.5 and a named fixture
module, with small fixtures for loader metadata, scan results, side selection, the loading
context, and the base container. It does not start Minecraft or require Loom's game setup.

The tests cover zero-argument helpers/getters, static and file handlers, Kotlin objects,
unannotated handlers, mod/game buses, multiple and empty entrypoint lists, construction order,
construction failure, side/mod-id filtering, priorities, and canceled events. Each handler must
run exactly once for each posted event.

The harness targets the currently checked-in 3.1 NeoForge source variant. The repository's
normal Gradle build remains responsible for compiling the four Stonecutter variants against
their actual loader APIs.