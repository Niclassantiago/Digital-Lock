package com.digitallock.network;

import com.digitallock.data.LockAccess;
import com.digitallock.data.LockManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Handlers server-side de los packets C2S. Todo se valida acá (server-side
 * authoritative): el cliente nunca decide permisos ni si el PIN es correcto.
 */
public final class ServerPayloadHandler {
    private ServerPayloadHandler() {}

    /** Distancia máxima (al cuadrado) para aceptar una acción sobre un bloque. */
    private static final double REACH_SQR = 64.0; // 8 bloques

    public static void handleSetPin(SetPinPacket packet, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        String pin = packet.pin();
        if (pin == null || !pin.matches("\\d{4}")) {
            return;
        }
        BlockPos pos = packet.pos();
        if (!inReach(player, pos)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || !LockAccess.canSetPin(player, be)) {
            return;
        }
        LockManager.applyPin(level, pos, player.getUUID(), pin);
    }

    static boolean inReach(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= REACH_SQR;
    }
}
