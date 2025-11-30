package dev.nyon.klf.compat.kff.accessors;
/*? if forge {*/
/*import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.impl.Jar;
import net.lenni0451.reflect.stream.RStream;
import net.lenni0451.reflect.stream.field.FieldWrapper;

public class JarAccessor {
    public static JarMetadata getMetadata(Jar jar) {
        RStream rStream = RStream.of(Jar.class);
        FieldWrapper metadataField = rStream.fields().by("metadata");
        return metadataField.get(jar);
    }
}
*//*?}*/