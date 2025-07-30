package dev.nyon.klf.mv


//? if lp: >=3.0 {
import net.neoforged.fml.ModLoadingIssue
//?}

//? if lp: <=2.0 {
/*
import org.objectweb.asm.Type
import java.lang.annotation.ElementType
import java.util.stream.Stream

internal fun ModFileScanData.getAnnotatedBy(clazz: Class<out Any>, elementType: ElementType): Stream<AnnotationData> {
    val type = Type.getType(clazz)
    return annotations.filter { data -> data.targetType == elementType && data.annotationType == type }.stream()
} *///?}

internal val gameBus: IEventBus
    get() {
        return /*? if lp: <=2.0 {*/ /*Bindings.getForgeBus().get() *//*?} else {*/ Bindings.getGameBus() /*?}*/
    }

internal fun modLoadingException(e: Throwable, modInfo: IModInfo): ModLoadingException {
    return /*? if lp: <=2.0 {*/ /*ModLoadingException(modInfo, ModLoadingStage.CONSTRUCT, "fml.modloading.failedtoloadmod", e)
        *//*?} else {*/ ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.failedtoloadmod", e).withCause(e).withAffectedMod(modInfo)) /*?}*/
}