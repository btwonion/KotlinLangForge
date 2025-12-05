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
import settingdust.preloading_tricks.api.modlauncher.ModLauncherPreloadingCallbacks;
import settingdust.preloading_tricks.lexforge.LexForgeModManager;
//? if forge {
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
//?} else {
/*import net.neoforged.fml.loading.moddiscovery.ModFile;
*///?}
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import settingdust.preloading_tricks.api.PreloadingTricksCallbacks;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TransformationService implements ITransformationService {

    private final Logger LOGGER = LogManager.getLogger();

    public TransformationService() {
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
            LexForgeModManager modManager = (LexForgeModManager) _manager;

            ModFile kffFile = null;
            ModFile klfFile = null;
            for (ModFile file : modManager.all()) {
                if (file.getSecureJar().name().equals("thedarkcolour.kotlinforforge")) kffFile = file;
                if (file.getSecureJar().name().equals("klf")) klfFile = file;
            }
            if (kffFile == null || klfFile == null) return;
            LOGGER.info("Found KFF: Applying compatibility patches.");

            Set<String> klfPackages = klfFile.getSecureJar().getPackages();
            Set<String> klfProvides = klfFile.getSecureJar()
                .getProviders()
                .stream()
                .map(SecureJar.Provider::serviceName)
                .collect(Collectors.toSet());
            Jar kffJar = (Jar) kffFile.getSecureJar();

            SimpleJarMetadata metadata = (SimpleJarMetadata) JarAccessor.getMetadata(kffJar);
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
            LOGGER.info("Compatibility patches applied successfully.");
        });
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
