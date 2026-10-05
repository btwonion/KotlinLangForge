package dev.nyon.klf.compat.kff;

import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.Jar;
import cpw.mods.jarhandling.impl.SimpleJarMetadata;
import dev.nyon.klf.compat.kff.accessors.JarAccessor;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import settingdust.preloading_tricks.api.ModManager;
import settingdust.preloading_tricks.api.PreloadingTricksCallbacks;

import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ModuleReader;
import java.lang.module.ResolutionException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Dependency-free regression checks, run by the compatibilityTest Gradle task. */
public class CompatibilityTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("klf-kff-test");
        try {
            SecureJar klf = fixture(root.resolve("klf"), "klf", Set.of("kotlin.shared", "klf.only"),
                List.of(new SecureJar.Provider("shared.Service", List.of("klf.only.Provider"))));
            SecureJar kff = fixture(root.resolve("kff"), "thedarkcolour.kotlinforforge",
                Set.of("kotlin.shared", "kff.only"), List.of(
                    new SecureJar.Provider("shared.Service", List.of("kff.only.SharedProvider")),
                    new SecureJar.Provider("kff.only.Service", List.of("kff.only.Provider"))));
            ModuleDescriptor originalKlf = klf.moduleDataProvider().descriptor();

            try {
                resolve(klf, kff);
                throw new AssertionError("Overlapping providers must reproduce a split-package failure");
            } catch (ResolutionException expected) {
                require(expected.getMessage().contains("kotlin.shared"), "Identify the conflicting package");
            }

            new TransformationService();
            setupMods(klf);
            require(originalKlf.equals(klf.moduleDataProvider().descriptor()), "KLF alone needs no patch");
            setupMods(kff);
            require(kff.moduleDataProvider().descriptor().packages().contains("kotlin.shared"),
                "KFF alone needs no patch");
            setupMods(klf, kff);
            resolve(klf, kff);
            ModuleDescriptor patchedKff = kff.moduleDataProvider().descriptor();
            require(patchedKff.packages().equals(Set.of("kff.only")), "Preserve KFF-specific packages");
            require(patchedKff.provides().stream().map(ModuleDescriptor.Provides::service)
                .collect(Collectors.toSet()).equals(Set.of("kff.only.Service")), "Preserve KFF-specific services");
            require(originalKlf.equals(klf.moduleDataProvider().descriptor()), "Preserve KLF metadata");
            require(kff.name().equals("thedarkcolour.kotlinforforge"), "Keep the KFF module available");
            TransformationService.patchMetadata(kff, klf);
            require(patchedKff.equals(kff.moduleDataProvider().descriptor()), "Repeated patching is stable");

            SecureJar unsupported = SecureJar.from(jar -> new JarMetadata() {
                public String name() { return "thedarkcolour.kotlinforforge"; }
                public String version() { return "1"; }
                public ModuleDescriptor descriptor() { return ModuleDescriptor.newAutomaticModule(name()).build(); }
            }, root.resolve("kff"));
            try {
                TransformationService.patchMetadata(unsupported, klf);
                throw new AssertionError("Unsupported metadata must fail before attempting mutation");
            } catch (IllegalStateException expected) {
                require(expected.getMessage().contains("Unsupported KFF metadata implementation"),
                    "Explain unsupported metadata");
                require(expected.getMessage().contains(JarAccessor.getMetadata((Jar) unsupported).getClass().getName()),
                    "Identify the unsupported metadata type");
            }
            try {
                setupMods(klf, unsupported);
                throw new AssertionError("The early callback must report metadata failures");
            } catch (IllegalStateException expected) {
                require(expected.getMessage().contains("before module resolution")
                    && expected.getMessage().contains(klf.getPrimaryPath().toString())
                    && expected.getMessage().contains(unsupported.getPrimaryPath().toString()),
                    "Identify the failure stage and both provider paths");
                require(expected.getMessage().contains("Keep providers required by other mods installed"),
                    "Give guidance that preserves required dependencies");
                require(expected.getCause() instanceof IllegalStateException
                    && expected.getCause().getMessage().contains("Unsupported KFF metadata"),
                    "Preserve the original metadata failure");
            }
            missingPreloadingTricks();
            System.out.println("PASS: module resolution, retained metadata, repeat patch, "
                + "unsupported metadata, missing dependency");
        } finally {
            try (var files = Files.walk(root)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void setupMods(SecureJar... jars) {
        List<ModFile> files = Arrays.stream(jars)
            .map(jar -> new ModFile(jar, null, file -> null, "LIBRARY")).toList();
        ModManager<?> manager = (ModManager<?>) Proxy.newProxyInstance(ModManager.class.getClassLoader(),
            new Class<?>[]{ModManager.class}, (proxy, method, args) -> {
                if (method.getName().equals("all")) return files;
                throw new AssertionError("Metadata compatibility must not remove mods: " + method.getName());
            });
        PreloadingTricksCallbacks.SETUP_MODS.getInvoker().onSetupMods(manager);
    }

    private static SecureJar fixture(Path path, String name, Set<String> packages,
                                     List<SecureJar.Provider> providers) throws Exception {
        for (String pkg : packages) {
            Path directory = Files.createDirectories(path.resolve(pkg.replace('.', '/')));
            Files.write(directory.resolve("Placeholder.class"), new byte[0]);
        }
        for (SecureJar.Provider provider : providers) {
            Path services = Files.createDirectories(path.resolve("META-INF/services"));
            Files.write(services.resolve(provider.serviceName()), provider.providers());
        }
        return SecureJar.from(jar -> new SimpleJarMetadata(name, "1", jar.getPackages(), jar.getProviders()), path);
    }

    private static void resolve(SecureJar klf, SecureJar kff) {
        ModuleDescriptor dependent = ModuleDescriptor.newModule("dependent.mod")
            .requires("klf").requires("thedarkcolour.kotlinforforge").build();
        Set<ModuleReference> references = Set.of(reference(dependent),
            reference(klf.moduleDataProvider().descriptor()), reference(kff.moduleDataProvider().descriptor()));
        ModuleFinder finder = new ModuleFinder() {
            public Optional<ModuleReference> find(String name) {
                return references.stream().filter(ref -> ref.descriptor().name().equals(name)).findFirst();
            }
            public Set<ModuleReference> findAll() { return references; }
        };
        ModuleLayer.boot().configuration().resolveAndBind(finder, ModuleFinder.of(), Set.of("dependent.mod"));
    }

    private static ModuleReference reference(ModuleDescriptor descriptor) {
        return new ModuleReference(descriptor, URI.create("memory:/" + descriptor.name())) {
            public ModuleReader open() { throw new UnsupportedOperationException("Resolution does not read classes"); }
        };
    }

    private static void missingPreloadingTricks() throws Exception {
        var urls = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
            .filter(path -> !Path.of(path).getFileName().toString().contains("preloading-tricks"))
            .map(path -> {
                try { return Path.of(path).toUri().toURL(); }
                catch (Exception e) { throw new IllegalStateException(e); }
            }).toArray(java.net.URL[]::new);
        try (var isolated = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader())) {
            var service = isolated.loadClass(TransformationService.class.getName());
            try {
                service.getConstructor().newInstance();
                throw new AssertionError("The service must report missing Preloading Tricks");
            } catch (InvocationTargetException expected) {
                Throwable cause = expected.getCause();
                require(cause instanceof IllegalStateException, "Expose the early compatibility diagnostic");
                require(cause.getMessage().contains("Preloading Tricks")
                    && cause.getMessage().contains("COMPATIBILITY.md"), "Provide dependency guidance");
                require(cause.getCause() instanceof NoClassDefFoundError, "Preserve the original linkage error");
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
