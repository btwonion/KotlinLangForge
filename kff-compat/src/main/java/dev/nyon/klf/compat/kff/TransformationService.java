package dev.nyon.klf.compat.kff;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.Jar;
import cpw.mods.jarhandling.impl.SimpleJarMetadata;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import cpw.mods.niofs.union.UnionPath;
import dev.nyon.klf.compat.kff.accessors.JarAccessor;
import dev.nyon.klf.compat.kff.accessors.SimpleJarMetadataAccessor;
import settingdust.preloading_tricks.api.ModManager;
import settingdust.preloading_tricks.api.PreloadingTricksCallbacks;
import settingdust.preloading_tricks.api.modlauncher.ModLauncherPreloadingCallbacks;
//? if forge {
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
//?} else {
/*import net.neoforged.fml.loading.moddiscovery.ModFile;
import java.lang.module.ModuleDescriptor;
*///?}
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TransformationService implements ITransformationService {

    private final Logger LOGGER = LogManager.getLogger();

    public TransformationService() {
        try {
            registerCallbacks();
        } catch (LinkageError e) {
            String message = "KLF could not register its early KFF compatibility callbacks. "
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
            ModManager<ModFile> modManager = (ModManager<ModFile>) _manager;

            ModFile kffFile = null;
            ModFile klfFile = null;
            for (ModFile file : modManager.all()) {
                if (file.getSecureJar().name().equals("thedarkcolour.kotlinforforge")) kffFile = file;
                if (file.getSecureJar().name().equals("klf")) klfFile = file;
            }
            if (kffFile == null || klfFile == null) return;
            LOGGER.info("Applying early KLF/KFF compatibility: KLF={}, KFF={}",
                klfFile.getFilePath(), kffFile.getFilePath());

            try {
                patchMetadata(kffFile.getSecureJar(), klfFile.getSecureJar());
            } catch (RuntimeException | LinkageError e) {
                String message = "KLF could not patch KFF metadata before module resolution. KLF="
                    + klfFile.getFilePath() + ", KFF=" + kffFile.getFilePath()
                    + ". Check the Forge, KLF, KFF and Preloading Tricks versions and the original error below. "
                    + "Keep providers required by other mods installed.";
                LOGGER.error(message, e);
                throw new IllegalStateException(message, e);
            }
            LOGGER.info("KLF/KFF metadata compatibility patch applied before module resolution.");
        });
    }

    static void patchMetadata(
        SecureJar kffSecureJar,
        SecureJar klfSecureJar
    ) {
        /*? if forge {*/
        Set<String> klfPackages = klfSecureJar.getPackages();
        Set<String> klfProvides = klfSecureJar
            .getProviders()
            .stream()
            .map(SecureJar.Provider::serviceName)
            .collect(Collectors.toSet());
        /*?} else {*/
        /*Set<String> klfPackages = klfSecureJar.moduleDataProvider().descriptor().packages();
        Set<String> klfProvides = klfSecureJar.moduleDataProvider().descriptor().provides()
            .stream()
            .map(ModuleDescriptor.Provides::service)
            .collect(Collectors.toSet());
        *//*?}*/

        if (!(kffSecureJar instanceof Jar kffJar)) {
            throw new IllegalStateException("Unsupported KFF SecureJar implementation: "
                + kffSecureJar.getClass().getName());
        }

        var jarMetadata = JarAccessor.getMetadata(kffJar);
        if (!(jarMetadata instanceof SimpleJarMetadata metadata)) {
            throw new IllegalStateException("Unsupported KFF metadata implementation: "
                + (jarMetadata == null ? "null" : jarMetadata.getClass().getName()));
        }
        SimpleJarMetadataAccessor.setPkgs(
            metadata,
            kffJar.getPackages()
                .stream()
                .filter(it -> !klfPackages.contains(it))
                .collect(Collectors.toSet())
        );
        SimpleJarMetadataAccessor.setProviders(
            metadata,
            metadata.providers()
                .stream()
                .filter(it -> !klfProvides.contains(it.serviceName()))
                .toList()
        );
    }

    @Override
    public @NotNull String name() {
        return "KLF-KFF-Compat";
    }

    @Override
    public void initialize(IEnvironment iEnvironment) { }

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
