package com.digitallock.network;

import java.util.Optional;

import com.digitallock.data.LockAccess;
import com.digitallock.data.LockData;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;

/** Helpers server-side para mandar el estado del candado al cliente. */
public final class LockSync {
    private LockSync() {}

    /** Manda el estado actual del candado de {@code pos} a todos los que trackean el chunk. */
    public static void sendToTrackers(ServerLevel level, BlockPos pos) {
        Optional<LockData> data = LockAccess.getLock(level.getBlockEntity(pos));
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos),
                new LockSyncPacket(pos.immutable(), data));
    }

    /** Manda el candado de {@code pos} a un jugador puntual (al cargarle el chunk). */
    public static void sendToPlayer(ServerPlayer player, BlockPos pos, LockData data) {
        PacketDistributor.sendToPlayer(player, new LockSyncPacket(pos.immutable(), Optional.of(data)));
    }
}
