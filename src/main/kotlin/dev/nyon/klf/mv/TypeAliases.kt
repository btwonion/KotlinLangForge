package dev.nyon.klf.mv

typealias Event = /*? if forge {*/  /*net.minecraftforge.eventbus.api.Event  *//*?} else {*/ net.neoforged.bus.api.Event /*?}*/
typealias EventBusErrorMessage = /*? if forge {*/  /*net.minecraftforge.eventbus.EventBusErrorMessage  *//*?} else {*/ net.neoforged.bus.EventBusErrorMessage /*?}*/
typealias SubscribeEvent = /*? if forge {*/  /*net.minecraftforge.eventbus.api.SubscribeEvent  *//*?} else {*/ net.neoforged.bus.api.SubscribeEvent /*?}*/
typealias BusBuilder = /*? if forge {*/  /*net.minecraftforge.eventbus.api.BusBuilder  *//*?} else {*/ net.neoforged.bus.api.BusBuilder /*?}*/
typealias IEventBus = /*? if forge {*/  /*net.minecraftforge.eventbus.api.IEventBus  *//*?} else {*/ net.neoforged.bus.api.IEventBus /*?}*/
typealias Bindings = /*? if forge {*/  /*net.minecraftforge.fml.Bindings  *//*?} else {*/ net.neoforged.fml.Bindings /*?}*/
typealias ModContainer = /*? if forge {*/  /*net.minecraftforge.fml.ModContainer  *//*?} else {*/ net.neoforged.fml.ModContainer /*?}*/
typealias ModLoadingContext = /*? if forge {*/  /*net.minecraftforge.fml.ModLoadingContext  *//*?} else {*/ net.neoforged.fml.ModLoadingContext /*?}*/
typealias ModLoadingException = /*? if forge {*/  /*net.minecraftforge.fml.ModLoadingException  *//*?} else {*/ net.neoforged.fml.ModLoadingException /*?}*/
//? if lp: <=2.0 {
/*typealias ModLoadingStage = /^? if forge {^/  /^net.minecraftforge.fml.ModLoadingStage  ^//^?} else {^/ net.neoforged.fml.ModLoadingStage /^?}^/
*///?}
typealias EventBusSubscriber = /*? if forge {*/  /*net.minecraftforge.fml.common.Mod.EventBusSubscriber  *//*?} else if lp: >=3.0 {*/ net.neoforged.fml.common.EventBusSubscriber /*?} else {*/ /*net.neoforged.fml.common.Mod.EventBusSubscriber *//*?}*/
typealias Mod = /*? if forge {*/  /*net.minecraftforge.fml.common.Mod  *//*?} else {*/ net.neoforged.fml.common.Mod /*?}*/
typealias IModBusEvent = /*? if forge {*/  /*net.minecraftforge.fml.event.IModBusEvent  *//*?} else {*/ net.neoforged.fml.event.IModBusEvent /*?}*/
typealias FMLEnvironment = /*? if forge {*/  /*net.minecraftforge.fml.loading.FMLEnvironment  *//*?} else {*/ net.neoforged.fml.loading.FMLEnvironment /*?}*/
typealias FMLLoader = /*? if forge {*/  /*net.minecraftforge.fml.loading.FMLLoader  *//*?} else {*/ net.neoforged.fml.loading.FMLLoader /*?}*/
typealias EnumHolder = /*? if forge {*/  /*net.minecraftforge.fml.loading.moddiscovery.ModAnnotation.EnumHolder  *//*?} else if lp: >=3.0 {*/ net.neoforged.fml.loading.modscan.ModAnnotation.EnumHolder /*?} else {*/ /*net.neoforged.fml.loading.moddiscovery.ModAnnotation.EnumHolder *//*?}*/
typealias Dist = /*? if forge {*/  /*net.minecraftforge.api.distmarker.Dist  *//*?} else {*/ net.neoforged.api.distmarker.Dist /*?}*/
typealias ModFileScanData = /*? if forge {*/  /*net.minecraftforge.forgespi.language.ModFileScanData  *//*?} else {*/ net.neoforged.neoforgespi.language.ModFileScanData /*?}*/
typealias AnnotationData = /*? if forge {*/  /*net.minecraftforge.forgespi.language.ModFileScanData.AnnotationData  *//*?} else {*/ net.neoforged.neoforgespi.language.ModFileScanData.AnnotationData /*?}*/
typealias IModInfo = /*? if forge {*/  /*net.minecraftforge.forgespi.language.IModInfo  *//*?} else {*/ net.neoforged.neoforgespi.language.IModInfo /*?}*/
typealias IModLanguageLoader = /*? if forge {*/  /*net.minecraftforge.forgespi.language.IModLanguageProvider.IModLanguageLoader  *//*?} else if lp: >2.0 {*/ net.neoforged.neoforgespi.language.IModLanguageLoader /*?} else {*/ /*net.neoforged.neoforgespi.language.IModLanguageProvider.IModLanguageLoader *//*?}*/