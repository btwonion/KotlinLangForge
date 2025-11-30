package dev.nyon.klf.compat.kff.modloading;
/*? if forge {*/
/*import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileModLocator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class KlfModFileLocator extends AbstractJarFileModLocator {
    @Override
    public Stream<Path> scanCandidates() {
        return Stream.empty();
    }

    public ModFileOrException getKlfFile() {
        Path path;
        try {
            List<Path> paths = Files.list(Path.of("mods/"))
                .filter(p-> p.toString().contains("KotlinLangForge"))
                .toList();
            path = paths.get(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return createMod(path);
    }

    @Override
    public String name() {
        return "KlfModFileLocator";
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {}
}
/^}^/*/