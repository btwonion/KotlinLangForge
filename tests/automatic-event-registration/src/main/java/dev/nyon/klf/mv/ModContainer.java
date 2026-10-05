package dev.nyon.klf.mv;

import net.neoforged.bus.api.IEventBus;

/** Minimal loader boundary; construction and subscriber registration use the production Kotlin classes. */
public abstract class ModContainer {
    private final IModInfo modInfo;

    protected ModContainer(IModInfo info) {
        modInfo = info;
    }

    public String getModId() {
        return modInfo.getModId();
    }

    public IModInfo getModInfo() {
        return modInfo;
    }

    protected abstract void constructMod();

    public void construct() {
        constructMod();
    }

    public abstract IEventBus getEventBus();
}