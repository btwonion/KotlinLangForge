package dev.nyon.klf.mv

import java.lang.reflect.InvocationTargetException

//? if lp: >=3.0 {
import net.neoforged.fml.ModLoadingIssue
//?}

//? if lp: <=2.0 {
/*import org.objectweb.asm.Type
import java.lang.annotation.ElementType
import java.util.stream.Stream

internal fun ModFileScanData.getAnnotatedBy(clazz: Class<out Any>, elementType: ElementType): Stream<AnnotationData> {
    val type = Type.getType(clazz)
    return annotations.filter { data -> data.targetType == elementType && data.annotationType == type }.stream()
} *///?}

internal val gameBus: IEventBus
    get() = /*? if lp: <=2.0 {*/ /*Bindings.getForgeBus().get() *//*?} else if lp: <=3.0 {*/ /*Bindings.getGameBus() *//*?} else {*/ FMLLoader.getCurrent().bindings.gameBus /*?}*/

internal fun modLoadingException(e: Throwable, modInfo: IModInfo): ModLoadingException {
    val cause = e.unwrapInvocationTargetException()
    return /*? if lp: <=2.0 {*/ /*ModLoadingException(modInfo, ModLoadingStage.CONSTRUCT, "fml.modloading.failedtoloadmod", cause)
        *//*?} else {*/ ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.failedtoloadmod", cause).withCause(cause).withAffectedMod(modInfo)) /*?}*/
}

internal fun Throwable.unwrapInvocationTargetException(): Throwable {
    var cause = this
    while (cause is InvocationTargetException) {
        cause = cause.targetException ?: return cause
    }
    return cause
}

internal val dist: Dist
    get() = /*? if lp: <=3.0 {*/ /*FMLEnvironment.dist *//*?} else {*/ FMLLoader.getCurrent().dist /*?}*/

internal val IModFileInfo.moduleName: String
    get() = /*? if lp: <=3.0 {*/ /*this.moduleName() *//*?} else {*/ this.file.id /*?}*/
