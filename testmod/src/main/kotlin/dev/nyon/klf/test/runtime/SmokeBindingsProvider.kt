package dev.nyon.klf.test.runtime

import dev.nyon.klf.test.*
import java.util.function.Supplier

// Registered only inside smokeFixtureJar, never in the real Minecraft fixture.
// Supplies real event buses/config events without bootstrapping Minecraft's global singleton.
class SmokeBindingsProvider : IBindingsProvider {
    private val gameBus = BusBuilder.builder().build()

    //? if lp: <=2.0 {
    /*override fun getForgeBusSupplier(): Supplier<IEventBus> = Supplier { gameBus }
    override fun getMessageParser(): Supplier<I18NParser> = Supplier {
        object : I18NParser {
            override fun parseMessage(key: String, vararg args: Any?): String = key + args.joinToString(prefix = " [", postfix = "]")
            override fun stripControlCodes(message: String): String = message
        }
    }
    override fun getConfigConfiguration(): Supplier<ConfigConfig> = Supplier { configConfiguration() }
    *///?} else {
    override fun getGameBus(): IEventBus = gameBus
    //? if lp: <=3.0
    //override fun getConfigConfiguration(): ConfigConfig = configConfiguration()
    //?}

    //? if lp: <=3.0 {
    /*private fun configConfiguration() = ConfigConfig(::ConfigLoading, ::ConfigReloading, ::ConfigUnloading)
    *///?}
}
