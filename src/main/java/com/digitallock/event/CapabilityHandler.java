package com.digitallock.event;

import com.digitallock.DigitalLock;
import com.digitallock.data.LockAccess;
import com.digitallock.data.LockData;

import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;

/**
 * Provider propio del {@code ItemHandler} para cofres / cofres trampa / barriles.
 * Si el bloque tiene candado <b>con PIN</b>, devuelve {@link EmptyItemHandler}
 * (corta hoppers, hopper minecarts, pipes, AE2, y cualquier automatización que
 * use la capability estándar). Si no, devuelve {@code null} para delegar en el
 * provider vanilla del contenedor.
 */
@EventBusSubscriber(modid = DigitalLock.MODID)
public final class CapabilityHandler {
    private CapabilityHandler() {}

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.ItemHandler.BLOCK,
                (level, pos, state, be, side) -> {
                    if (LockAccess.getEffectiveLock(level, pos).map(LockData::hasPin).orElse(false)) {
                        return EmptyItemHandler.INSTANCE;
                    }
                    return null; // sin candado / sin PIN -> delega en el handler vanilla
                },
                Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL);
    }
}
