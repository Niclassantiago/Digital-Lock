package com.digitallock.event;

import com.digitallock.DigitalLock;
import com.digitallock.data.LockAccess;
import com.digitallock.network.LockSync;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;

/** Eventos server-side que disparan la sincronización del candado al cliente. */
@EventBusSubscriber(modid = DigitalLock.MODID)
public final class LockSyncEvents {
    private LockSyncEvents() {}

    /**
     * Al enviarle un chunk a un jugador, le mandamos el estado de cada candado
     * que ese chunk contenga, así ve el candado al acercarse a un cofre ya
     * bloqueado (no solo al aplicarlo).
     */
    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        LevelChunk chunk = event.getChunk();
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            LockAccess.getLock(be).ifPresent(data ->
                    LockSync.sendToPlayer(event.getPlayer(), be.getBlockPos(), data));
        }
    }
}
