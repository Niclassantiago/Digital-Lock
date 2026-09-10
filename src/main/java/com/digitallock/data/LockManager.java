package com.digitallock.data;

import java.util.Optional;
import java.util.UUID;

import com.digitallock.network.LockSync;
import com.digitallock.registry.ModDataAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Mutaciones server-side del candado (setear PIN, quitar). Centraliza el escribir
 * el attachment + persistir + sincronizar al cliente. La propagación a cofres
 * dobles se agrega en la Etapa 9.
 */
public final class LockManager {
    private LockManager() {}

    /** Setea el PIN del candado en {@code pos}, con {@code owner} como dueño. */
    public static void applyPin(ServerLevel level, BlockPos pos, UUID owner, String pin) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return;
        }
        long createdAt = LockAccess.getLock(be).map(LockData::createdAt).orElse(level.getGameTime());
        byte[] salt = PinHasher.newSalt();
        byte[] hash = PinHasher.hash(pin, salt);
        be.setData(ModDataAttachments.LOCK_DATA.get(),
                new LockData(owner, Optional.of(hash), Optional.of(salt), createdAt));
        be.setChanged();
        level.invalidateCapabilities(pos); // el PIN corta el item handler -> refrescar hoppers
        LockSync.sendToTrackers(level, pos);
    }

    /** Quita el candado de {@code pos}. Devuelve true si había uno. */
    public static boolean removeLock(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || !be.hasData(ModDataAttachments.LOCK_DATA.get())) {
            return false;
        }
        be.removeData(ModDataAttachments.LOCK_DATA.get());
        be.setChanged();
        level.invalidateCapabilities(pos); // vuelve a habilitar el item handler vanilla
        LockSession.invalidate(pos);
        LockSync.sendToTrackers(level, pos); // data vacía -> el cliente lo remueve
        return true;
    }
}
