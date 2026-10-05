package dev.nyon.klf.fixtures

//? if lp: <=3.0 {
/*import dev.nyon.klf.mv.BusBuilder
import dev.nyon.klf.mv.IEventBus
import java.util.function.Supplier

//? if forge {
import net.minecraftforge.fml.IBindingsProvider
import net.minecraftforge.fml.I18NParser
import net.minecraftforge.fml.config.IConfigEvent
import net.minecraftforge.fml.event.config.ModConfigEvent
//?} else {
import net.neoforged.fml.IBindingsProvider
import net.neoforged.fml.config.IConfigEvent
import net.neoforged.fml.event.config.ModConfigEvent
//? if lp: <=2.0
import net.neoforged.fml.I18NParser
//?}

// Supplies the loader's config-event factories without bootstrapping Minecraft.
class HeadlessBindings : IBindingsProvider {
    //? if lp: <=2.0 {
    override fun getForgeBusSupplier(): Supplier<IEventBus> = Supplier { BusBuilder.builder().build() }
    override fun getMessageParser(): Supplier<I18NParser> = Supplier { error("No translations in headless tests") }
    override fun getConfigConfiguration(): Supplier<IConfigEvent.ConfigConfig> = Supplier { configEvents() }
    //?} else {
    override fun getGameBus(): IEventBus = BusBuilder.builder().build()
    override fun getConfigConfiguration(): IConfigEvent.ConfigConfig = configEvents()
    //?}

    private fun configEvents() = IConfigEvent.ConfigConfig(
        { ModConfigEvent.Loading(it) },
        { ModConfigEvent.Reloading(it) },
        { ModConfigEvent.Unloading(it) }
    )
}
*///?}
