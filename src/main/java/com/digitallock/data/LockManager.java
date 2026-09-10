package com.digitallock.data;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import com.digitallock.network.LockSync;
import com.digitallock.registry.ModDataAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Mutaciones server-side del candado (aplicar, setear PIN, quitar). Centraliza
 * escribir el attachment + persistir + invalidar capabilities + sincronizar, y
 * propaga a ambas mitades de un cofre doble.
 */
public final class LockManager {
    private LockManager() {}

    /** Aplica un candado sin PIN (si no tiene ya) a la(s) mitad(es). */
    public static void applyLock(ServerLevel level, BlockPos pos) {
        LockData data = LockData.freshUnlocked(level.getGameTime());
        forEachHalf(level, pos, halfPos -> {
            BlockEntity be = level.getBlockEntity(halfPos);
            if (be != null && !be.hasData(ModDataAttachments.LOCK_DATA.get())) {
                be.setData(ModDataAttachments.LOCK_DATA.get(), data);
                be.setChanged();
                LockSync.sendToTrackers(level, halfPos);
            }
        });
    }

    /** Setea el PIN del candado, con {@code owner} como dueño, en la(s) mitad(es). */
    public static void applyPin(ServerLevel level, BlockPos pos, UUID owner, String pin) {
        long createdAt = LockAccess.getEffectiveLock(level, pos).map(LockData::createdAt).orElse(level.getGameTime());
        byte[] salt = PinHasher.newSalt();
        byte[] hash = PinHasher.hash(pin, salt);
        LockData data = new LockData(owner, Optional.of(hash), Optional.of(salt), createdAt);
        forEachHalf(level, pos, halfPos -> {
            BlockEntity be = level.getBlockEntity(halfPos);
            if (be != null) {
                be.setData(ModDataAttachments.LOCK_DATA.get(), data);
                be.setChanged();
                level.invalidateCapabilities(halfPos); // el PIN corta el item handler
                LockSync.sendToTrackers(level, halfPos);
            }
        });
    }

    /** Quita el candado de la(s) mitad(es). Devuelve true si había uno. */
    public static boolean removeLock(ServerLevel level, BlockPos pos) {
        boolean[] removed = {false};
        forEachHalf(level, pos, halfPos -> {
            BlockEntity be = level.getBlockEntity(halfPos);
            if (be != null && be.hasData(ModDataAttachments.LOCK_DATA.get())) {
                be.removeData(ModDataAttachments.LOCK_DATA.get());
                be.setChanged();
                level.invalidateCapabilities(halfPos);
                LockSession.invalidate(halfPos);
                LockSync.sendToTrackers(level, halfPos); // data vacía -> el cliente lo remueve
                removed[0] = true;
            }
        });
        return removed[0];
    }

    /** Ejecuta {@code action} sobre {@code pos} y, si es cofre doble, sobre la otra mitad. */
    public static void forEachHalf(ServerLevel level, BlockPos pos, Consumer<BlockPos> action) {
        action.accept(pos);
        BlockPos partner = LockAccess.partnerPos(level.getBlockState(pos), pos);
        if (partner != null) {
            action.accept(partner);
        }
    }
}
