package com.digitallock.data;

import java.util.Optional;

import com.digitallock.registry.ModDataAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Helpers de acceso al {@link LockData} guardado como attachment sobre el
 * BlockEntity vanilla del cofre / barril. Centraliza el uso del attachment para
 * no dispersar {@code getData}/{@code hasData} por todo el código.
 *
 * <p><b>Importante:</b> en NeoForge, {@code getData} materializa (y persiste) el
 * valor por defecto si el attachment no existe. Por eso SIEMPRE se chequea
 * primero {@code hasData}; nunca exponer {@code getData} crudo hacia afuera.
 *
 * <p>La resolución de cofres dobles (escribir/leer ambas mitades) llega en la
 * Etapa 9; por ahora se opera sobre el BE concreto que se pasa.
 */
public final class LockAccess {
    private LockAccess() {}

    /** Devuelve el candado del BE si existe, o {@link Optional#empty()}. */
    public static Optional<LockData> getLock(BlockEntity be) {
        if (be == null || !be.hasData(ModDataAttachments.LOCK_DATA.get())) {
            return Optional.empty();
        }
        return Optional.of(be.getData(ModDataAttachments.LOCK_DATA.get()));
    }

    /** True si el BE tiene un candado aplicado (con o sin PIN). */
    public static boolean hasLock(BlockEntity be) {
        return be != null && be.hasData(ModDataAttachments.LOCK_DATA.get());
    }

    /**
     * True si {@code player} está autorizado sobre un candado <b>con PIN</b>:
     * admin OP (bypass), dueño, o validado en sesión.
     */
    public static boolean isAuthorized(Player player, BlockPos pos, LockData lock) {
        if (player.hasPermissions(2)) {
            return true; // bypass de moderación (configurable en Etapa 11)
        }
        if (lock.isOwner(player.getUUID())) {
            return true;
        }
        return LockSession.isValidated(player.getUUID(), pos);
    }

    /** True si {@code player} puede abrir el contenedor directamente. */
    public static boolean canOpen(Player player, BlockEntity be) {
        Optional<LockData> lock = getLock(be);
        if (lock.isEmpty() || !lock.get().hasPin()) {
            return true; // sin candado, o candado sin PIN -> abierto para todos
        }
        return isAuthorized(player, be.getBlockPos(), lock.get());
    }

    /** True si {@code player} puede setear el PIN (hay candado y todavía no tiene PIN). */
    public static boolean canSetPin(Player player, BlockEntity be) {
        Optional<LockData> lock = getLock(be);
        return lock.isPresent() && !lock.get().hasPin();
    }

    /** True si {@code player} puede quitar el candado. */
    public static boolean canRemove(Player player, BlockEntity be) {
        Optional<LockData> lock = getLock(be);
        if (lock.isEmpty()) {
            return false;
        }
        if (!lock.get().hasPin()) {
            return true; // sin PIN: cualquiera que lo aplicó puede sacarlo
        }
        return isAuthorized(player, be.getBlockPos(), lock.get());
    }
}
