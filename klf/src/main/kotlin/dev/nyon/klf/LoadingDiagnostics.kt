package dev.nyon.klf

import dev.nyon.klf.mv.Event
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal fun findModConstructor(modClass: Class<*>, allowedArgumentTypes: Set<Class<*>>): Constructor<*>? {
    val constructors = modClass.constructors
    if (constructors.isEmpty() && modClass.kotlin.objectInstance != null) return null
    require(constructors.size == 1) {
        "Mod class ${modClass.name} must be a Kotlin object or have exactly one public constructor; " +
            "found ${constructors.size} public constructors. Avoid private constructors, secondary public " +
            "constructors, and @JvmOverloads."
    }

    val constructor = constructors.single()
    val supportedTypes = allowedArgumentTypes.joinToString(", ") { it.name }
    val unsupportedTypes = constructor.parameterTypes.filter { it !in allowedArgumentTypes }
    require(unsupportedTypes.isEmpty()) {
        "Mod constructor ${modClass.name} has unsupported argument types: " +
            "${unsupportedTypes.joinToString(", ") { it.name }}. Accepted types (at most once each, in any order): $supportedTypes."
    }
    val duplicateTypes = constructor.parameterTypes.groupBy { it }.filterValues { it.size > 1 }.keys
    require(duplicateTypes.isEmpty()) {
        "Mod constructor ${modClass.name} has duplicate argument types: " +
            "${duplicateTypes.joinToString(", ") { it.name }}. Accepted types (at most once each, in any order): $supportedTypes."
    }
    return constructor
}

internal fun validateAnnotatedSubscriber(method: Method, isObject: Boolean) {
    val acceptedSignature = "Expected a static method or a Kotlin object method with exactly one " +
        "parameter extending ${Event::class.java.name}, for example fun onEvent(event: EventSubtype)."
    require((Modifier.isStatic(method.modifiers) || isObject) &&
        method.parameterCount == 1 && Event::class.java.isAssignableFrom(method.parameterTypes.single())) {
        "Invalid @SubscribeEvent method ${method.toGenericString()}. $acceptedSignature"
    }
    //? if forge {
    /*require(Modifier.isPublic(method.modifiers) && Modifier.isPublic(method.declaringClass.modifiers)) {
        "Invalid @SubscribeEvent method ${method.toGenericString()}. Forge requires a public method in a public class. " +
            acceptedSignature
    }
    *///?}
}
