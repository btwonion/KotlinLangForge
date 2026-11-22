package dev.nyon.klf.compat.kff;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import cpw.mods.niofs.union.UnionPath;
import settingdust.preloading_tricks.api.ModManager;
import settingdust.preloading_tricks.api.PreloadingTricksCallbacks;
import settingdust.preloading_tricks.api.modlauncher.ModLauncherPreloadingCallbacks;
//? if forge {
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
//?} else {
/*import net.neoforged.fml.loading.moddiscovery.ModFile;
*///?}
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TransformationService implements ITransformationService {

    private final Logger LOGGER = LogManager.getLogger();

    public TransformationService() {
        try {
            registerCallbacks();
        } catch (LinkageError e) {
            String message = "KLF could not register its early Kotlin jar compatibility callbacks. "
                + "Install a compatible Forge release of Preloading Tricks, and check the original error below.";
            LOGGER.error(message, e);
            throw new IllegalStateException(message, e);
        }
    }

    private void registerCallbacks() {
        ModLauncherPreloadingCallbacks.COLLECT_ADDITIONAL_DEPENDENCY_SOURCES.register(manager -> {
            try {
                var selfPath =
                    ((UnionPath) Path.of(
                        TransformationService.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()))
                        .getFileSystem().getPrimaryPath();
                manager.add(selfPath, "magnetic_service");
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });

        PreloadingTricksCallbacks.SETUP_MODS.register(_manager -> {
            @SuppressWarnings("unchecked")
            ModManager<ModFile> modManager = (ModManager<ModFile>) _manager;
            patchMods(modManager);
        });
    }

    private void patchMods(ModManager<ModFile> modManager) {
        ModFile klfFile = null;
        for (ModFile file : modManager.all()) {
            if (file.getSecureJar().name().equals("klf")) klfFile = file;
        }
        if (klfFile == null) {
            LOGGER.warn("KLF jar not found in the mod list, skipping the early Kotlin jar compatibility patch.");
            return;
        }

        Set<String> klfPackages;
        Set<String> klfServices;
        try {
            klfPackages = packagesOf(klfFile.getSecureJar());
            klfServices = servicesOf(klfFile.getSecureJar());
        } catch (RuntimeException | LinkageError e) {
            LOGGER.error("KLF could not read its own packages from {}, skipping the early Kotlin jar compatibility patch.",
                describe(klfFile), e);
            return;
        }

        int patched = 0;
        int skipped = 0;
        List<ModFile> removalCandidates = new ArrayList<>();
        List<ModFile> bridges = new ArrayList<>();
        for (ModFile file : List.copyOf(modManager.all())) {
            if (file == klfFile) continue;

            KotlinJarPatcher.Result result;
            try {
                result = KotlinJarPatcher.patch(file.getSecureJar(), klfPackages);
            } catch (RuntimeException | LinkageError e) {
                LOGGER.warn("KLF could not patch {}. If it bundles Kotlin, module resolution may fail on duplicate packages.",
                    describe(file), e);
                skipped++;
                continue;
            }

            switch (result.outcome()) {
                case UNTOUCHED -> { }
                case PATCHED -> {
                    patched++;
                    LOGGER.info("Patched {}: removed {} package(s) KLF provides, the jar stays loaded.",
                        describe(file), result.strippedPackages().size());
                    LOGGER.debug("Removed packages from {}: {}", describe(file),
                        KotlinJarPatcher.sorted(result.strippedPackages()));
                    if (!result.removedProviderClasses().isEmpty()) {
                        LOGGER.debug("Removed service implementations from {}: {}", describe(file),
                            result.removedProviderClasses());
                    }
                    // Services KLF registers itself are still available; only warn about the others.
                    List<String> lostServices = result.removedServices().stream()
                        .filter(it -> !klfServices.contains(it))
                        .toList();
                    if (!lostServices.isEmpty()) {
                        LOGGER.warn("{} no longer provides these services, because all their implementations are in "
                            + "packages KLF provides: {}", describe(file), lostServices);
                    }
                    if (lostServices.size() != result.removedServices().size()) {
                        LOGGER.debug("Removed services from {} that KLF also provides: {}", describe(file),
                            result.removedServices().stream().filter(klfServices::contains).toList());
                    }
                }
                case REMOVE -> removalCandidates.add(file);
                case BRIDGE -> bridges.add(file);
                case SKIPPED -> {
                    if (result.strippedPackages().isEmpty()) {
                        // Overlap unknown, e.g. a virtual mod that isn't backed by a real jar.
                        LOGGER.debug("Not checking {} for Kotlin packages: {}", describe(file), result.reason());
                        continue;
                    }
                    skipped++;
                    LOGGER.warn("KLF could not patch {} ({}). It contains {} package(s) KLF also provides, so module "
                            + "resolution may fail: {}", describe(file), result.reason(),
                        result.strippedPackages().size(), KotlinJarPatcher.sorted(result.strippedPackages()));
                }
            }
        }

        List<ModFile> removed =
            KotlinJarPatcher.selectRemovals(modManager.all(), removalCandidates, bridges, ModFile::getSecureJar);
        for (ModFile file : bridges) {
            if (removed.contains(file)) {
                LOGGER.info("Removed empty library {} from the mod list: it contains no classes and only requires "
                    + "modules that were removed because KLF provides them: {}", describe(file),
                    KotlinJarPatcher.sorted(KotlinJarPatcher.requiredModules(file.getSecureJar())));
            }
        }
        for (ModFile file : removalCandidates) {
            if (removed.contains(file)) {
                LOGGER.info("Removed library {} from the mod list: KLF provides all its packages, and its module-info "
                    + "can't be patched. It has no mods.toml, so no mod depends on it by mod ID.", describe(file));
            } else {
                skipped++;
                LOGGER.warn("Kept library {} although KLF provides all its packages, because another module requires "
                    + "module {}. Module resolution may fail on duplicate packages.", describe(file),
                    file.getSecureJar().name());
            }
        }
        if (!removed.isEmpty()) modManager.removeAll(removed);

        LOGGER.info("KLF Kotlin jar compatibility: patched {} jar(s), removed {} library jar(s), could not patch {} "
            + "jar(s).", patched, removed.size(), skipped);
    }

    /** The mod file's path, or for jar-in-jar files (whose file path is empty) its module name and URI. */
    private static String describe(ModFile file) {
        String path = file.getFilePath().toString();
        if (!path.isEmpty()) return path;
        String name = file.getSecureJar().name();
        try {
            return name + " (" + file.getSecureJar().moduleDataProvider().uri() + ")";
        } catch (RuntimeException e) {
            return name;
        }
    }

    private static Set<String> servicesOf(SecureJar secureJar) {
        /*? if forge {*/
        return secureJar.getProviders().stream().map(SecureJar.Provider::serviceName).collect(Collectors.toSet());
        /*?} else {*/
        /*return secureJar.moduleDataProvider().descriptor().provides().stream()
            .map(java.lang.module.ModuleDescriptor.Provides::service)
            .collect(Collectors.toSet());
        *//*?}*/
    }

    private static Set<String> packagesOf(SecureJar secureJar) {
        /*? if forge {*/
        return secureJar.getPackages();
        /*?} else {*/
        /*return secureJar.moduleDataProvider().descriptor().packages();
        *//*?}*/
    }

    @Override
    public @NotNull String name() {
        return "KLF-KFF-Compat";
    }

    @Override
    public void initialize(IEnvironment iEnvironment) {
        // Before any service's beginScanning, so the filter wraps the layer lists innermost and runs last.
        OrphanedBridgeFilter.install();
    }

    @Override
    public void onLoad(
        IEnvironment iEnvironment,
        Set<String> set
    ) throws IncompatibleEnvironmentException {
    }

    @Override
    public @NotNull List<ITransformer> transformers() {
        return List.of();
    }
}
