package dev.nyon.klf.compat.kff;
/*? if forge {*/
/*import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.Jar;
import cpw.mods.jarhandling.impl.SimpleJarMetadata;
import dev.nyon.klf.compat.kff.accessors.JarAccessor;
import dev.nyon.klf.compat.kff.accessors.SimpleJarMetadataAccessor;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModLocator;
import settingdust.preloading_tricks.api.PreloadingTricksCallback;
import settingdust.preloading_tricks.api.PreloadingTricksModManager;
import settingdust.preloading_tricks.lexforge.LexForgeModManager;
import settingdust.preloading_tricks.lexforge.mod_candidate.DefinedModLocator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PreloadingCallback implements PreloadingTricksCallback {

    private Path klfFilePath = null;

    @Override
    public void onCollectModCandidates() {
        try {
            List<Path> paths = Files.list(Path.of("mods/"))
                .filter(p-> p.toString().contains("KotlinLangForge"))
                .toList();
            klfFilePath = paths.get(0);
            DefinedModLocator.definedCandidates.add(klfFilePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onSetupMods() {
        LexForgeModManager modManager = PreloadingTricksModManager.get();

        ModFile kffFile = null;
        for (ModFile file : modManager.all()) {
            if (file.getSecureJar().name().equals("thedarkcolour.kotlinforforge")) kffFile = file;
        }
        IModFile klfFile = loadKlfFile();
        if (kffFile == null || klfFile == null) return;

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
    }

    private IModFile loadKlfFile() {
        DefinedModLocator locator = new DefinedModLocator();
        IModLocator.ModFileOrException modFileOrException = locator.scanMods().get(0);
        if (modFileOrException.ex() != null) return null;
        return modFileOrException.file();
    }
}
*//*?}*/
