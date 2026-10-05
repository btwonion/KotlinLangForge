# Compatibility with other Kotlin providers

Last reviewed: **2026-10-05**. This guide distinguishes implementation support, upstream reports, and local regression
checks. It does not certify every modpack or every combination of library versions.

## Providers and dependent mods

KotlinLangForge (KLF) supplies Kotlin libraries and the `klf` language adapter. Kotlin for Forge (KFF) supplies libraries
and its own `kotlinforforge` adapter and APIs. A mod written in Kotlin is a **dependent mod**, not necessarily another
runtime provider. Its required adapter and APIs still matter even if another installed provider supplies Kotlin classes.
See [KFF's usage instructions](https://github.com/thedarkcolour/KotlinForForge#readme).

**Keep every provider required by installed mods.** KLF is not a drop-in replacement for a mod's declared KFF dependency,
and KFF does not replace the `klf` adapter. Check the dependent mod's installation instructions and dependency metadata
before changing providers. To isolate a problem, copy the instance and test coherent groups of providers plus their
dependents; removing a required provider alone introduces a different loading failure.

## KLF plus KFF by loader and Minecraft version

The language-provider line (`1.0`, `2.0`, `3.0`, `3.1`) is KLF's own compatibility label. The table describes that line's
Minecraft generation; individual artifacts accept specific Minecraft versions, listed in their
[build properties](klf/versions). Use matching loader builds of both providers.

| Loader / Minecraft generation | KLF line | KLF + KFF expectation | Evidence / verification level |
| --- | --- | --- | --- |
| Forge 1.16.5 | 1.0 (legacy) | No current compatibility claim for the historical artifact. | No 1.0 build target in this checkout; unverified here. |
| Forge 1.17.1–1.20.4 | 2.0 | Coexistence is implemented through KLF's early compatibility service and requires **Preloading Tricks**. Use the distributed Forge artifact containing that service. | [Maintainer-confirmed support](https://github.com/btwonion/KotlinLangForge/issues/72#issuecomment-3616106531); regression checks cover metadata resolution, not game launches across the range. |
| NeoForge 1.20.1 | Forge-era fork; no dedicated current KLF target | Unverified; do not assume the Forge service works simply because this fork uses Forge package names. | Included in the [maintainer's unsupported Preloading Tricks range](https://github.com/btwonion/KotlinLangForge/issues/72#issuecomment-3616106531). No validated compatibility artifact for this fork. |
| NeoForge 1.20.2–1.20.4 | 2.0 | KLF itself targets these versions, but coexistence with shaded KFF remains unsupported by the current compatibility service. | The NeoForge `kff-compat` target is [disabled](settings.gradle.kts); released Preloading Tricks still has an incompatible API for this generation. |
| NeoForge 1.20.5–1.21.8 | 3.0 | Shared libraries are nested dependencies, allowing loader deduplication when coordinates and accepted version ranges agree. No KLF KFF-patching service is shipped for this line. | [KLF packaging](klf/build.gradle.kts), [KFF 5.x packaging](https://github.com/thedarkcolour/KotlinForForge/blob/5.x/build.gradle.kts), and [NeoForge dependency selection](https://docs.neoforged.net/toolchain/docs/plugins/mdg/). Whole-pack behavior remains version-dependent. |
| NeoForge 1.21.9–26.3 | 3.1 | The same nested-dependency approach applies. Choose the KFF generation intended for your Minecraft version. | Implemented by [KLF packaging](klf/build.gradle.kts); not a locally tested matrix of KFF releases or game launches. |

KLF has no current Forge target above 1.20.4. A NeoForge compatibility row is not a promise of support on Forge, Fabric,
or a loader bridge. The supported versions in each artifact remain authoritative.

## How Forge coexistence works

The [`KLF-KFF-Compat` transformation service](kff-compat/src/main/java/dev/nyon/klf/compat/kff/TransformationService.java)
registers Preloading Tricks callbacks before the game module layer is resolved. When it finds modules named `klf` and
`thedarkcolour.kotlinforforge`, it removes KFF's package and service metadata entries that overlap KLF's. KFF's distinct
packages remain available. This targets that specific packaging arrangement; it is not a general patch for any jar
containing Kotlin classes, and it does not negotiate library API versions.

The distributed Forge file includes the compatibility service and KLF. **Preloading Tricks is a separate required
dependency**, declared by [KLF's publication configuration](klf/build.gradle.kts). The service currently compiles against
Preloading Tricks **3.6.0**. A bare KLF library/development jar without the service does not provide the same compatibility
behavior. Use the release files and dependencies appropriate to your Forge instance.

If the service cannot link its callback API, it reports Preloading Tricks guidance and retains the original error.
If metadata patching fails, it reports the KLF and KFF paths, version checks, and the original cause before module
resolution. A successful patch log means the metadata operation completed; it does not prove all dependent mods' API
requirements are satisfied.

## Why the latest Preloading Tricks does not unblock old NeoForge

As of the review date, the latest published release is **3.7.3 (2026-06-29)**. Release **3.7.0 (2026-05-24)** added a
NeoForge adapter, but its ModLauncher NeoForge target is **Minecraft 1.21.1**, alongside a newer loader target. The
release's combined Minecraft and loader tags do not describe every possible pairing.
See the [release history](https://github.com/SettingDust/preloading-tricks/blob/main/CHANGELOG.md),
[published files](https://modrinth.com/mod/preloading-tricks/version/3.7.3), and
[upstream targets](https://github.com/SettingDust/preloading-tricks/blob/main/build.gradle.kts).

The released NeoForge `VirtualModFile` uses `ModFileDiscoveryAttributes`, which is absent from the loader/SPI **2.0.17**
bundled with **NeoForge 20.4.237 (Minecraft 1.20.4)**. This was checked against the published source and loader jars.
See [the upstream implementation](https://github.com/SettingDust/preloading-tricks/blob/main/src/platform/neoforge/modlauncher/main/java/settingdust/preloading_tricks/neoforge/modlauncher/virtual_mod/VirtualModFile.java)
and [the NeoForge 20.4.237 artifacts](https://maven.neoforged.net/releases/net/neoforged/neoforge/20.4.237/).
A dependency bump alone does not supply that missing loader API.

NeoForge 1.20.1 uses Forge-era namespaces, so this specific API mismatch is not proof of its failure mechanism.
Its loader is a separate fork, and there is no validated KLF compatibility target for it here. Extending support needs
a compatible early hook and launch testing with both providers and dependent mods, before enabling or publishing it.
[Issue #72](https://github.com/btwonion/KotlinLangForge/issues/72) remains open.

## Other reports and their limits

| Combination | What is known as of 2026-10-05 | Practical next step |
| --- | --- | --- |
| Essential | [KLF #48](https://github.com/btwonion/KotlinLangForge/issues/48) is a historical June 2025 report, closed as not planned. It mentions Essential's built-in Kotlin loader, without a current version matrix or verified fix. | Treat current combinations as unverified. Reproduce with exact versions and logs before concluding that all Essential releases conflict. |
| Kilt / Fabric bridge | [Kilt #877](https://github.com/KiltMC/Kilt/issues/877) reports a startup failure on Kilt 20.1.17 / Minecraft 1.20.1 while adding Surveyor Atlases and its KLF/Preloading Tricks dependencies. The issue remains open without a proven cause. | Report bridge-specific logs and versions to Kilt. Native Forge support does not establish support through Kilt, and this report alone does not establish a Kotlin split-package cause. |
| Fiw Tools / Fiw Custom Items on NeoForge | [Fiw-Tools #1](https://github.com/Fi3w0/Fiw-Tools/issues/1) reports Kotlin classes merged into the third-party 1.3.0 jar. Its [maintainer reports a 1.3.1 packaging fix](https://github.com/Fi3w0/Fiw-Tools/issues/1#issuecomment-5884904242) and a successful test with NeoForge 21.1.249 + KFF 5.12.0. | Use the upstream fixed build matching your instance and verify it. This is an upstream-reported test, not a KLF launch test; no blanket claim that all versions still conflict. |

Modern nested dependency selection can select one shared library using its group/artifact identity and compatible
version constraints. It cannot deduplicate Kotlin classes merged directly into an unrelated mod's main jar. That mod's
packaging needs a fix; removing a provider needed elsewhere merely changes which problem appears next.
See [NeoForge's Jar-in-Jar guidance](https://docs.neoforged.net/toolchain/docs/dependencies/jarinjar/).

## Troubleshooting a startup failure

1. Record Minecraft, loader and Java versions, both provider filenames, Preloading Tricks if applicable, and the first
   exception with its `Caused by` chain. Keep `latest.log` and `debug.log` when available.
2. For `ResolutionException` / duplicate Kotlin packages, identify the **two modules and jar paths named in the error**.
   They may be KLF/KFF, a third-party jar with shaded Kotlin, or a different pair. A Kotlin-dependent mod named as a
   reader of the package is not necessarily the jar that supplies the duplicate classes.
3. On Forge 2.0, confirm the service-bearing release and Preloading Tricks are installed. Look for the early KLF/KFF
   compatibility log. Its absence is a clue, not proof of a particular cause: the failure may precede callback execution.
4. On modern NeoForge, check that each library is packaged as a nested dependency with matching coordinates and
   compatible version ranges. Update the mod owning any merged Kotlin copy to a fixed release.
5. Reproduce in a copied minimal instance containing the required providers and one dependent mod for each. Report
   the result, filenames and logs to the project owning the failing packaging or loader hook; include relevant issue links.

A module-layer error can occur **before KLF's language loader or a mod constructor executes**. Those later components
cannot catch it or display a mod-loading error screen for it. The early Forge diagnostics apply only if that service
and its callbacks are reached; the launcher log remains necessary for earlier failures.

## Validation scope

The `:kff-compat:2.0-forge:compatibilityTest` regression task constructs actual SecureJar metadata and checks that Java
module resolution rejects overlapping packages before patching and succeeds afterward. It also checks preservation of
KLF metadata and KFF-only packages/services, repeat patching, either provider alone, early callback failure diagnostics
with provider paths and retained causes, and service construction without Preloading Tricks. These fixtures do not launch
Minecraft, exercise every loader build, or establish compatibility
with Essential, Kilt, old NeoForge, or all KFF library versions.
