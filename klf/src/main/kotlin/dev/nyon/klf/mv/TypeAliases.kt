package dev.nyon.klf.mv

internal typealias EventBusErrorMessage = /*? if forge {*/  /*net.minecraftforge.eventbus.EventBusErrorMessage  *//*?} else {*/ net.neoforged.bus.EventBusErrorMessage /*?}*/
internal typealias Event = /*? if forge {*/  /*net.minecraftforge.eventbus.api.Event  *//*?} else {*/ net.neoforged.bus.api.Event /*?}*/
internal typealias SubscribeEvent = /*? if forge {*/  /*net.minecraftforge.eventbus.api.SubscribeEvent  *//*?} else {*/ net.neoforged.bus.api.SubscribeEvent /*?}*/
internal typealias BusBuilder = /*? if forge {*/  /*net.minecraftforge.eventbus.api.BusBuilder  *//*?} else {*/ net.neoforged.bus.api.BusBuilder /*?}*/
internal typealias IEventBus = /*? if forge {*/  /*net.minecraftforge.eventbus.api.IEventBus  *//*?} else {*/ net.neoforged.bus.api.IEventBus /*?}*/
internal typealias Bindings = /*? if forge {*/  /*net.minecraftforge.fml.Bindings  *//*?} else if lp: <=3.0 {*/ /*net.neoforged.fml.Bindings *//*?} else {*/ net.neoforged.neoforge.internal.NeoForgeBindings /*?}*/
internal typealias ModContainer = /*? if forge {*/  /*net.minecraftforge.fml.ModContainer  *//*?} else {*/ net.neoforged.fml.ModContainer /*?}*/
internal typealias ModLoadingContext = /*? if forge {*/  /*net.minecraftforge.fml.ModLoadingContext  *//*?} else {*/ net.neoforged.fml.ModLoadingContext /*?}*/
internal typealias ModLoadingException = /*? if forge {*/  /*net.minecraftforge.fml.ModLoadingException  *//*?} else {*/ net.neoforged.fml.ModLoadingException /*?}*/
//? if lp: <=2.0 {
/*internal typealias ModLoadingStage = /^? if forge {^/  /^net.minecraftforge.fml.ModLoadingStage  ^//^?} else {^/ net.neoforged.fml.ModLoadingStage /^?}^/
*///?}
internal typealias EventBusSubscriber = /*? if forge {*/  /*net.minecraftforge.fml.common.Mod.EventBusSubscriber  *//*?} else if lp: >=3.0 {*/ net.neoforged.fml.common.EventBusSubscriber /*?} else {*/ /*net.neoforged.fml.common.Mod.EventBusSubscriber *//*?}*/
internal typealias Mod = /*? if forge {*/  /*net.minecraftforge.fml.common.Mod  *//*?} else {*/ net.neoforged.fml.common.Mod /*?}*/
internal typealias IModBusEvent = /*? if forge {*/  /*net.minecraftforge.fml.event.IModBusEvent  *//*?} else {*/ net.neoforged.fml.event.IModBusEvent /*?}*/
internal typealias FMLEnvironment = /*? if forge {*/  /*net.minecraftforge.fml.loading.FMLEnvironment  *//*?} else {*/ net.neoforged.fml.loading.FMLEnvironment /*?}*/
internal typealias FMLLoader = /*? if forge {*/  /*net.minecraftforge.fml.loading.FMLLoader  *//*?} else {*/ net.neoforged.fml.loading.FMLLoader /*?}*/
internal typealias EnumHolder = /*? if forge {*/  /*net.minecraftforge.fml.loading.moddiscovery.ModAnnotation.EnumHolder  *//*?} else if lp: >=3.0 {*/ net.neoforged.fml.loading.modscan.ModAnnotation.EnumHolder /*?} else {*/ /*net.neoforged.fml.loading.moddiscovery.ModAnnotation.EnumHolder *//*?}*/
internal typealias Dist = /*? if forge {*/  /*net.minecraftforge.api.distmarker.Dist  *//*?} else {*/ net.neoforged.api.distmarker.Dist /*?}*/
internal typealias ModFileScanData = /*? if forge {*/  /*net.minecraftforge.forgespi.language.ModFileScanData  *//*?} else {*/ net.neoforged.neoforgespi.language.ModFileScanData /*?}*/
internal typealias AnnotationData = /*? if forge {*/  /*net.minecraftforge.forgespi.language.ModFileScanData.AnnotationData  *//*?} else {*/ net.neoforged.neoforgespi.language.ModFileScanData.AnnotationData /*?}*/
internal typealias IModInfo = /*? if forge {*/  /*net.minecraftforge.forgespi.language.IModInfo  *//*?} else {*/ net.neoforged.neoforgespi.language.IModInfo /*?}*/
internal typealias IModFileInfo = /*? if forge {*/  /*net.minecraftforge.forgespi.language.IModFileInfo  *//*?} else {*/ net.neoforged.neoforgespi.language.IModFileInfo /*?}*/
internal typealias IModLanguageLoader = /*? if forge {*/  /*net.minecraftforge.forgespi.language.IModLanguageProvider.IModLanguageLoader  *//*?} else if lp: >2.0 {*/ net.neoforged.neoforgespi.language.IModLanguageLoader /*?} else {*/ /*net.neoforged.neoforgespi.language.IModLanguageProvider.IModLanguageLoader *//*?}*/