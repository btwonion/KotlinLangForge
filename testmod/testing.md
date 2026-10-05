# Runtime verification

The fixture now asserts behavior. Printing a success message or merely compiling the mod is not a pass.

## Headless JVM checks (normal CI)

Run from the repository root with JDK 21:

```sh
./gradlew :testmod:2.0-forge:check :testmod:2.0-neoforge:check \
  :testmod:3.0-neoforge:check :testmod:3.1-neoforge:check \
  --no-parallel --max-workers=2
```

`check` runs `test` and `testClientDistribution` in separate JVMs. The latter sets a **synthetic FML CLIENT distribution**; it does not launch Minecraft or load client-only Minecraft classes. The separation matters because legacy `FMLEnvironment.dist` is captured in a static final field. `build-commit.yml` runs these suites as independent matrix jobs and uploads JUnit XML and HTML reports even on failure.

| Project | Testmod Minecraft / loader | Boundary |
| --- | --- | --- |
| `2.0-forge` | 1.20.1 / Forge 47.4.4 | Legacy construction activity, Forge instance/config APIs |
| `2.0-neoforge` | 1.20.4 / NeoForge 20.4.237 | Legacy NeoForge provider/container API |
| `3.0-neoforge` | 1.20.6 / NeoForge 20.6.121 | New language loader and loading issues, static FML access |
| `3.1-neoforge` | 1.21.9 / NeoForge 21.9.16-beta | Instance FMLLoader, new module metadata and bindings |

These are representative fixture versions, not certification of every Minecraft version listed in KLF's artifact metadata. Forge's testmod intentionally uses 1.20.1 even though the KLF 2.0 Forge build uses 1.18.2.

The suite instantiates the **production** `KotlinModContainer` and `KotlinLanguageLoader`, loads Kotlin fixtures from a real named module, and uses the version's real FML, scan records, reflection, and event-bus libraries. Discovery metadata and the module-layer manager are small interface proxies. A fixture-only FML bindings service supplies real event buses/config events and a minimal legacy message formatter; it is registered only in the JVM fixture jar and cannot replace Minecraft's bindings during a real launch. It initializes minimal FML version/distribution state through reflection instead of bootstrapping Minecraft; there is no replacement container or fake event bus.

Normal checks cover empty/singleton entrypoints, constructor bus/container/distribution injection and loading context, invalid constructor forms, annotated/unannotated/private (NeoForge) and public static event handlers, exact invocation counts through container event dispatch and mod/game-bus routing, matching and opposite subscriber sides, mod-id filtering, modern entrypoint side filtering, NeoForge configuration event dispatch, and rejection of a wrong Minecraft version before entrypoint loading. The wrong-version check inspects KLF's diagnostic cause; legacy FML's outer translated message requires a real game layer and is outside this JVM suite.

NeoForge's configuration check exercises its container `acceptEvent`/event-bus path. Forge has a different `dispatchConfigEvent`/handler path, covered by the strict regression below. These checks do not read/write real configuration files.

## Strict regressions and fix dependencies

The normal suite explicitly excludes three named tags. `@JvmStatic` object handlers already delivered once on the baseline and remain in the normal passing suite. They assert the **desired** behavior and fail on the baseline; they are not expected-failure tests and do not swallow exceptions. Run them with:

```sh
./gradlew :testmod:2.0-forge:runtimeRegressionTest \
  :testmod:2.0-neoforge:runtimeRegressionTest \
  :testmod:3.0-neoforge:runtimeRegressionTest \
  :testmod:3.1-neoforge:runtimeRegressionTest \
  --continue --no-parallel --max-workers=2
```

This task includes the normal checks and the strict regressions in the DEDICATED_SERVER JVM. A manual `build-commit` workflow run with **runtime_regressions** enabled runs it in CI and reports failures normally.

| Tag | Desired behavior | Fix area |
| --- | --- | --- |
| `event-regression` | Helpers/getters/non-event signatures are safely ignored; multiple entrypoints deliver each event once | [Event fix PR #143](https://github.com/btwonion/KotlinLangForge/pull/143) |
| `forge-regression` | `getMod()` returns the constructed object; `matches` and `ModList.getModContainerByObject` find it; config loading/reloading each reach the bus once | [Container fix PR #144](https://github.com/btwonion/KotlinLangForge/pull/144) (instance APIs on legacy Forge/NeoForge; config forwarding on Forge only) |
| `diagnostics-regression` | Constructor exceptions expose the original cause directly, using legacy causes or modern loading issues as appropriate | [Diagnostics PR #145](https://github.com/btwonion/KotlinLangForge/pull/145) |

This infrastructure PR does not contain those production fixes. It can merge independently with immediate passing coverage. Apply the corresponding fix PRs together with this change to run their strict cases. Combining #144 and #145 requires resolving their constructor edit: retain the instance return/storage from #144, use the constructor validator from #145, and return the Kotlin object instance when that validator returns no constructor. The event injection must remain outside the entrypoint loop from #143. Once the fixes are merged, remove their exclusions from both normal Test tasks so those cases become required CI checks. Do not enable strict regressions while discarding their exit status.

The failing scenarios live in unannotated fixture classes. Tests supply scan records explicitly; ordinary Minecraft launches do not inadvertently enable known failing scenarios.

## Dedicated-server smoke launch

`runSmokeServer` uses an isolated `testmod/versions/<version>/build/smoke-server` directory. It starts a real dedicated server without a GUI on an ephemeral port, asserts the constructor/lifecycle/registry/physical-side behavior, writes a result only after `ServerStartedEvent`, and requests a clean server stop. The task removes stale results before launch and fails if that result is missing, even if the Java process exits successfully.

Use the appropriate Java runtime (17 for legacy Minecraft, 21 for 1.20.6+). Loom's development server can bypass the usual EULA prompt; if your launch generates an EULA file and stops, review and accept it in that smoke directory if you agree to it. Use a time bound so a bootstrap failure or missing lifecycle event cannot hang unattended verification:

```sh
timeout 300 ./gradlew :testmod:3.1-neoforge:runSmokeServer --no-parallel --max-workers=2
```

The result file must contain `KLF_SERVER_SMOKE_PASSED`. An EULA/setup run that stops before server startup must fail without that result. Forge additionally needs its normal language-provider/bootstrap dependencies available; this development task is not a test of the packaged Preloading Tricks/KFF integration. CI additionally smoke-launches the newest `3.1-neoforge` dedicated server. Other versions retain the runnable smoke task but require separate launch validation; the JVM matrix does not certify those launches.

## Real-client checks still required

On a machine with a display, use `:testmod:<version>:runClient` for each row of the matrix. Launching a real client is required to exercise `ClientOnlyLaunchSubscriber`, which calls the real Minecraft client API. The dedicated-server launch exercises `ServerOnlyLaunchSubscriber` and must never initialize the client subscriber. At load completion, the fixture asserts that exactly the matching side ran and that all expected constructor/construct/registry events ran once. Any assertion fails startup.

Join a world, disconnect/rejoin, and load a player again. The player handler asserts that the same event instance is never delivered twice; it allows different legitimate player-loading events. JVM tests do not cover a renderer, world/player lifecycle, real config-file reloads, FML's translated error screens, installed jar discovery, or wrong-artifact cross-version launches. Continue those manual checks for releases; a green JVM matrix must not be described as full client/server launch coverage.

## Validation recorded for this change (2026-10-05)

- Normal JVM matrix: 112 checks passed across all four versions and both synthetic distributions, with no skipped cases.
- Strict baseline: 15 desired-behavior assertions failed (5 Forge, 4 legacy NeoForge, 3 each modern NeoForge), with no skipped cases. Forge configuration delivery was observed as zero, not hidden behind a bootstrap error.
- Applying event #143 (`59e2e63`) and container #144 (`f283450`) together made all event/instance/config regressions pass; only constructor-cause assertions remained failing.
- Adding diagnostics #145 (`93440e1`) with the constructor merge resolution above made all 71 strict checks pass across the matrix. These were temporary production source overlays in this worktree, restored after validation; this PR contains no production fixes.
- A real `3.1-neoforge` dedicated server reached `ServerStartedEvent`, passed the constructor/registry/physical-side assertions, wrote the checked result, and stopped cleanly.
- Local execution used the installed JDK 25. CI is configured for JDK 21; a local JDK 21 run and graphical-client/other full-server launches have not been performed. Real config-file parsing/reloading and release-jar/bootstrap integration remain outside this change's runtime certification.
