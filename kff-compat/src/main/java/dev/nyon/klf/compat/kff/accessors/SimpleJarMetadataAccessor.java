package dev.nyon.klf.compat.kff.accessors;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.SimpleJarMetadata;
import net.lenni0451.reflect.stream.RStream;
import net.lenni0451.reflect.stream.field.FieldWrapper;

import java.util.List;
import java.util.Set;

public class SimpleJarMetadataAccessor {
    private static final Class<SimpleJarMetadata> clazz = SimpleJarMetadata.class;

    private static final RStream stream = RStream.of(clazz);

    private static final FieldWrapper pkgsField = stream.fields().by("pkgs");
    private static final FieldWrapper providersField = stream.fields().by("providers");

    public static void setPkgs(SimpleJarMetadata metadata, Set<String> pkgs) {
        pkgsField.set(metadata, pkgs);
    }

    public static void setProviders(SimpleJarMetadata metadata, List<SecureJar.Provider> providers) {
        providersField.set(metadata, providers);
    }
}