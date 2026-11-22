package dev.nyon.klf.compat.kff.accessors;

import net.lenni0451.reflect.stream.RStream;
import net.lenni0451.reflect.stream.field.FieldWrapper;
//? if forge {
import net.minecraftforge.fml.loading.moddiscovery.ModJarMetadata;
//?} else {
/*import net.neoforged.fml.loading.moddiscovery.ModJarMetadata;
*///?}

public class ModJarMetadataAccessor {
    private static final Class<ModJarMetadata> clazz = ModJarMetadata.class;

    private static final RStream stream = RStream.of(clazz);

    private static final FieldWrapper descriptorField = stream.fields().by("descriptor");

    /** Drops the cached module descriptor, so it's rebuilt from the jar's current packages and providers. */
    public static void resetDescriptor(ModJarMetadata metadata) {
        descriptorField.set(metadata, null);
    }
}
