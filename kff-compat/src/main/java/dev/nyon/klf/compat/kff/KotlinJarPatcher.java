package dev.nyon.klf.compat.kff;

import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.Jar;
import cpw.mods.jarhandling.impl.ModuleJarMetadata;
import cpw.mods.jarhandling.impl.SimpleJarMetadata;
import dev.nyon.klf.compat.kff.accessors.JarAccessor;
import dev.nyon.klf.compat.kff.accessors.ModJarMetadataAccessor;
import dev.nyon.klf.compat.kff.accessors.SimpleJarMetadataAccessor;
//? if forge {
import net.minecraftforge.fml.loading.moddiscovery.ModJarMetadata;
//?} else {
/*import net.neoforged.fml.loading.moddiscovery.ModJarMetadata;
*///?}

import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Removes the packages KLF already provides from another jar's module metadata, so the module system doesn't see the
 * same package in two modules.
 * <p>
 * Service providers are filtered per implementation class: only classes in a removed package are dropped, because
 * the module system rejects a {@code provides} whose implementation isn't in the module. A service entry is kept as
 * long as at least one of its implementations remains, so a jar's own services (for example a language provider)
 * survive even when KLF provides the same service name.
 * <p>
 * What happens to a jar depends on its metadata type:
 * <ul>
 *     <li>Mods (a {@code mods.toml}, {@link ModJarMetadata}) and jars without a module-info
 *     ({@link SimpleJarMetadata}) are patched: they stay loaded and only lose the overlapping packages. A mod is
 *     never removed, so its mod ID stays available to the mods that depend on it.</li>
 *     <li>Libraries with an explicit module-info ({@link ModuleJarMetadata}) can't be patched. FML only uses this
 *     type for jars without a {@code mods.toml}, so such a jar has no mod ID. If KLF provides all its packages, for
 *     example a {@code kotlin-stdlib} shipped through jar-in-jar, it's removed from the mod list; otherwise it's kept
 *     and reported.</li>
 * </ul>
 */
final class KotlinJarPatcher {
    private KotlinJarPatcher() { }

    enum Outcome {
        /** The jar has no package in common with KLF. */
        UNTOUCHED,
        /** The overlapping packages (and the providers in them) were removed from the jar's metadata. */
        PATCHED,
        /**
         * A library with an explicit module-info (never a mod: FML gives jars with a {@code mods.toml} a
         * {@link ModJarMetadata}) whose packages are all provided by KLF. Its module can't be patched, so the whole
         * jar is dropped from the mod list.
         */
        REMOVE,
        /** The jar overlaps with KLF, but can't be patched. */
        SKIPPED
    }

    record Result(
        Outcome outcome,
        Set<String> strippedPackages,
        List<String> removedProviderClasses,
        List<String> removedServices,
        String reason
    ) {
        static Result of(Outcome outcome, Set<String> strippedPackages, String reason) {
            return new Result(outcome, strippedPackages, List.of(), List.of(), reason);
        }
    }

    static Result patch(SecureJar secureJar, Set<String> klfPackages) {
        if (!(secureJar instanceof Jar jar)) {
            return Result.of(Outcome.SKIPPED, Set.of(),
                "unsupported SecureJar implementation " + secureJar.getClass().getName());
        }

        Set<String> packages = jar.getPackages();
        Set<String> stripped = packages.stream().filter(klfPackages::contains).collect(Collectors.toUnmodifiableSet());
        if (stripped.isEmpty()) return Result.of(Outcome.UNTOUCHED, Set.of(), null);
        Set<String> kept = packages.stream()
            .filter(it -> !stripped.contains(it))
            .collect(Collectors.toUnmodifiableSet());

        JarMetadata metadata = JarAccessor.getMetadata(jar);
        if (metadata instanceof ModuleJarMetadata) {
            // An explicit module-info: its package list can't be changed without rewriting the module.
            return kept.isEmpty()
                ? Result.of(Outcome.REMOVE, stripped, "library with a module-info, all packages provided by KLF")
                : Result.of(Outcome.SKIPPED, stripped, "library with a module-info, which can't be patched, and "
                    + "packages KLF doesn't provide: " + sorted(kept));
        }
        if (!(metadata instanceof SimpleJarMetadata) && !(metadata instanceof ModJarMetadata)) {
            return Result.of(Outcome.SKIPPED, stripped, "unsupported metadata implementation "
                + (metadata == null ? "null" : metadata.getClass().getName()));
        }

        List<String> removedClasses = new ArrayList<>();
        List<String> removedServices = new ArrayList<>();
        List<SecureJar.Provider> jarProviders =
            filterProviders(jar.getProviders(), stripped, removedClasses, removedServices);

        // Keep the jar's own caches consistent with the descriptor, so later callers see the same view.
        JarAccessor.setPackages(jar, kept);
        JarAccessor.setProviders(jar, jarProviders);

        if (metadata instanceof SimpleJarMetadata simple) {
            // The record captured its own copies when the jar was opened.
            List<SecureJar.Provider> metadataProviders =
                filterProviders(simple.providers(), stripped, new ArrayList<>(), new ArrayList<>());
            SimpleJarMetadataAccessor.setPkgs(simple, kept);
            SimpleJarMetadataAccessor.setProviders(simple, metadataProviders);
        } else {
            // ModJarMetadata builds its descriptor lazily from the jar's packages and providers; drop any cached one.
            ModJarMetadataAccessor.resetDescriptor((ModJarMetadata) metadata);
        }

        return new Result(Outcome.PATCHED, stripped, List.copyOf(removedClasses), List.copyOf(removedServices), null);
    }

    /**
     * Drops the implementation classes that live in a stripped package. A service entry is only removed when none of
     * its implementations remain; entries that were empty to begin with are left alone.
     */
    static List<SecureJar.Provider> filterProviders(
        List<SecureJar.Provider> providers,
        Set<String> strippedPackages,
        List<String> removedClassesOut,
        List<String> removedServicesOut
    ) {
        List<SecureJar.Provider> result = new ArrayList<>(providers.size());
        for (SecureJar.Provider provider : providers) {
            List<String> implementations = provider.providers();
            List<String> kept = implementations.stream()
                .filter(it -> !strippedPackages.contains(packageOf(it)))
                .toList();
            if (kept.size() == implementations.size()) {
                result.add(provider);
                continue;
            }
            implementations.stream()
                .filter(it -> strippedPackages.contains(packageOf(it)))
                .forEach(it -> removedClassesOut.add(provider.serviceName() + " -> " + it));
            if (kept.isEmpty()) removedServicesOut.add(provider.serviceName());
            else result.add(new SecureJar.Provider(provider.serviceName(), kept));
        }
        return List.copyOf(result);
    }

    /** Package of a binary class name as written in a service file (nested classes use {@code $}). */
    static String packageOf(String className) {
        int index = className.lastIndexOf('.');
        return index < 0 ? "" : className.substring(0, index);
    }

    /** Modules an explicit module-info requires. Other jars become automatic modules, which require nothing. */
    static Set<String> requiredModules(SecureJar secureJar) {
        if (!(secureJar instanceof Jar jar)) return Set.of();
        if (!(JarAccessor.getMetadata(jar) instanceof ModuleJarMetadata metadata)) return Set.of();
        Set<String> required = new HashSet<>();
        for (ModuleDescriptor.Requires requires : metadata.descriptor().requires()) required.add(requires.name());
        return required;
    }

    /**
     * Picks the fully covered jars that can be dropped. A candidate is kept if a remaining explicit module requires
     * it; repeated until stable, since keeping a jar makes its own requirements matter.
     */
    static <T> List<T> selectRemovals(Collection<T> all, List<T> candidates, Function<T, SecureJar> jarOf) {
        List<T> removals = new ArrayList<>(candidates);
        boolean changed = true;
        while (changed && !removals.isEmpty()) {
            Set<String> required = new HashSet<>();
            for (T file : all) {
                if (removals.contains(file)) continue;
                try {
                    required.addAll(requiredModules(jarOf.apply(file)));
                } catch (RuntimeException | LinkageError ignored) {
                    // A jar whose descriptor can't be read fails resolution on its own; it doesn't block removals.
                }
            }
            changed = removals.removeIf(file -> required.contains(jarOf.apply(file).name()));
        }
        return removals;
    }

    static List<String> sorted(Set<String> values) {
        return values.stream().sorted().toList();
    }
}
