package com.digitallock.registry;

import com.digitallock.DigitalLock;
import com.digitallock.item.DigitalPadlockItem;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registro de ítems del mod. */
public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DigitalLock.MODID);

    /** Ítem candado digital: se aplica con Shift + click derecho a un contenedor. */
    public static final DeferredItem<DigitalPadlockItem> DIGITAL_PADLOCK =
            ITEMS.registerItem("digital_padlock", DigitalPadlockItem::new, new Item.Properties().stacksTo(16));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
