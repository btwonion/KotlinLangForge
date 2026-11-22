package dev.nyon.klf.compat.kff;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import net.lenni0451.reflect.stream.RStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.lang.module.ModuleDescriptor;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Drops empty bridge modules whose required modules are gone, right before ModLauncher builds a layer.
 * <p>
 * Kotlin 2.x's {@code kotlin-stdlib-jdk7} and {@code kotlin-stdlib-jdk8} jars contain no classes, only a module-info
 * that requires {@code kotlin.stdlib}. Some mods add their own Kotlin jars to a layer after mod discovery and rename
 * the overlapping ones to KLF's module name (Essential does this), which leaves the bridges requiring a module that no
 * longer exists, so resolution fails. Those jars are added after the {@code SETUP_MODS} patch, so they are handled here
 * instead.
 * <p>
 * ModLauncher builds a layer by streaming its list of jars. Mods that add jars do so by wrapping that list and changing
 * it when it's streamed; the innermost wrapper runs last. This filter wraps the lists during
 * {@code ITransformationService#initialize}, before any service starts scanning, so it sees the final list.
 * Only jars with an explicit module-info and no packages are dropped, so no classes are lost.
 */
final class OrphanedBridgeFilter {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<IModuleLayerManager.Layer> LAYERS =
        List.of(IModuleLayerManager.Layer.PLUGIN, IModuleLayerManager.Layer.GAME);

    private OrphanedBridgeFilter() { }

    static void install() {
        try {
            IModuleLayerManager manager = Launcher.INSTANCE.findLayerManager().orElseThrow();
            EnumMap<IModuleLayerManager.Layer, List<Object>> layers =
                RStream.of(manager.getClass()).fields().by("layers").get(manager);
            for (IModuleLayerManager.Layer layer : LAYERS) {
                layers.compute(layer, (key, list) ->
                    new FilteringList(list == null ? new ArrayList<>() : list, key, manager));
            }
        } catch (Throwable t) {
            LOGGER.warn("KLF could not hook into ModLauncher's module layers. Empty Kotlin bridge modules that other "
                + "mods add won't be filtered, which may fail module resolution.", t);
        }
    }

    private static void removeOrphanedBridges(
        List<Object> elements,
        IModuleLayerManager.Layer layer,
        IModuleLayerManager manager
    ) {
        Map<Object, SecureJar> jars = new LinkedHashMap<>();
        for (Object element : elements) {
            SecureJar jar = jarOf(element);
            // Without every module name, a requirement can't be called missing.
            if (jar == null) return;
            jars.put(element, jar);
        }
        Set<String> parentModules = parentModuleNames(layer, manager);

        boolean changed = true;
        while (changed) {
            changed = false;
            Set<String> available = new HashSet<>(parentModules);
            for (SecureJar jar : jars.values()) available.add(jar.name());

            for (var entry : List.copyOf(jars.entrySet())) {
                SecureJar jar = entry.getValue();
                ModuleDescriptor descriptor;
                try {
                    descriptor = jar.moduleDataProvider().descriptor();
                } catch (RuntimeException | LinkageError e) {
                    continue;
                }
                if (descriptor.isAutomatic() || !descriptor.packages().isEmpty()) continue;
                Set<String> missing = new HashSet<>(KotlinJarPatcher.requiredModules(descriptor));
                missing.removeAll(available);
                if (missing.isEmpty()) continue;

                elements.remove(entry.getKey());
                jars.remove(entry.getKey());
                changed = true;
                LOGGER.info("Dropped empty module {} ({}) from the {} layer: it contains no classes and requires {}, "
                        + "which no module provides. This happens when another mod renames its Kotlin modules to "
                        + "KLF's module name, as Essential does.", jar.name(), jar.moduleDataProvider().uri(), layer,
                    KotlinJarPatcher.sorted(missing));
            }
        }
    }

    /** The jar behind one of ModLauncher's {@code PathOrJar} records; path-only entries are opened. */
    private static SecureJar jarOf(Object pathOrJar) {
        try {
            SecureJar jar = RStream.of(pathOrJar.getClass()).fields().by("jar").get(pathOrJar);
            if (jar != null) return jar;
            return RStream.of(pathOrJar.getClass()).methods().by("build").invokeInstance(pathOrJar);
        } catch (Throwable t) {
            LOGGER.debug("Could not read layer element {}", pathOrJar, t);
            return null;
        }
    }

    private static Set<String> parentModuleNames(IModuleLayerManager.Layer layer, IModuleLayerManager manager) {
        Set<String> names = new HashSet<>();
        List<ModuleLayer> pending = new ArrayList<>();
        pending.add(ModuleLayer.boot());
        for (IModuleLayerManager.Layer other : IModuleLayerManager.Layer.values()) {
            if (other != layer) manager.getLayer(other).ifPresent(pending::add);
        }
        Set<ModuleLayer> seen = new HashSet<>();
        while (!pending.isEmpty()) {
            ModuleLayer moduleLayer = pending.remove(pending.size() - 1);
            if (!seen.add(moduleLayer)) continue;
            for (Module module : moduleLayer.modules()) names.add(module.getName());
            pending.addAll(moduleLayer.parents());
        }
        return names;
    }

    /** Delegates to ModLauncher's list and filters it once the layer is built from it. */
    private static final class FilteringList extends AbstractList<Object> {
        private final List<Object> delegate;
        private final IModuleLayerManager.Layer layer;
        private final IModuleLayerManager manager;
        private boolean filtering;

        private FilteringList(List<Object> delegate, IModuleLayerManager.Layer layer, IModuleLayerManager manager) {
            this.delegate = delegate;
            this.layer = layer;
            this.manager = manager;
        }

        @Override
        public Object get(int index) {
            return delegate.get(index);
        }

        @Override
        public Object set(int index, Object element) {
            return delegate.set(index, element);
        }

        @Override
        public void add(int index, Object element) {
            delegate.add(index, element);
        }

        @Override
        public Object remove(int index) {
            return delegate.remove(index);
        }

        @Override
        public int size() {
            return delegate.size();
        }

        // ModLauncher streams the list when it builds the layer.
        @Override
        public @NotNull Stream<Object> stream() {
            filter();
            return delegate.stream();
        }

        // Forge's ModLauncher fork uses toArray instead.
        @Override
        public <T> T @NotNull [] toArray(T @NotNull [] array) {
            filter();
            return delegate.toArray(array);
        }

        private void filter() {
            if (filtering) return;
            filtering = true;
            try {
                removeOrphanedBridges(delegate, layer, manager);
            } catch (Throwable t) {
                LOGGER.warn("KLF could not check the {} layer for empty Kotlin bridge modules.", layer, t);
            } finally {
                filtering = false;
            }
        }
    }
}
