package dev.nyon.klf.compat.kff;
/*? if forge {*/
/*import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.Jar;
import cpw.mods.jarhandling.impl.SimpleJarMetadata;
import dev.nyon.klf.compat.kff.accessors.JarAccessor;
import dev.nyon.klf.compat.kff.accessors.SimpleJarMetadataAccessor;
import dev.nyon.klf.compat.kff.modloading.KlfModFileLocator;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModLocator;
import settingdust.preloading_tricks.api.PreloadingTricksCallback;
import settingdust.preloading_tricks.api.PreloadingTricksModManager;
import settingdust.preloading_tricks.lexforge.LexForgeModManager;

import java.util.Set;
import java.util.stream.Collectors;

public class PreloadingCallback implements PreloadingTricksCallback {

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
        KlfModFileLocator locator = new KlfModFileLocator();
        IModLocator.ModFileOrException modFileOrException = locator.getKlfFile();
        if (modFileOrException.ex() != null) return null;
        return modFileOrException.file();
    }
}
*//*?}*/
