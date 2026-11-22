package dev.nyon.klf.compat.kff.accessors;

import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.Jar;
import net.lenni0451.reflect.stream.RStream;
import net.lenni0451.reflect.stream.field.FieldWrapper;

import java.util.List;
import java.util.Set;

public class JarAccessor {
    private static final Class<Jar> clazz = Jar.class;

    private static final RStream stream = RStream.of(clazz);

    private static final FieldWrapper metadataField = stream.fields().by("metadata");
    private static final FieldWrapper packagesField = stream.fields().by("packages");
    private static final FieldWrapper providersField = stream.fields().by("providers");

    public static JarMetadata getMetadata(Jar jar) {
        return metadataField.get(jar);
    }

    /** Replaces the lazily computed package cache behind {@link Jar#getPackages()}. */
    public static void setPackages(Jar jar, Set<String> packages) {
        packagesField.set(jar, packages);
    }

    /** Replaces the lazily computed provider cache behind {@link Jar#getProviders()}. */
    public static void setProviders(Jar jar, List<SecureJar.Provider> providers) {
        providersField.set(jar, providers);
    }
}
