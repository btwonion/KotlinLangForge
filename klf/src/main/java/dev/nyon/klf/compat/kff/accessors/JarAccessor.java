package dev.nyon.klf.compat.kff.accessors;
/*? if forge {*/
/*import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.impl.Jar;
import net.lenni0451.reflect.stream.RStream;
import net.lenni0451.reflect.stream.field.FieldWrapper;

public class JarAccessor {
    private static final Class<Jar> clazz = Jar.class;

    private static final RStream stream = RStream.of(clazz);

    private static final FieldWrapper metadataField = stream.fields().by("metadata");

    public static JarMetadata getMetadata(Jar jar) {
        return metadataField.get(jar);
    }
}
*//*?}*/