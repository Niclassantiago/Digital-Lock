package com.digitallock.data;

import java.util.Optional;

import com.digitallock.config.ModConfig;
import com.digitallock.registry.ModDataAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Helpers de acceso al {@link LockData} guardado como attachment sobre el
 * BlockEntity vanilla del cofre / barril. Centraliza el uso del attachment y la
 * resolución de cofres dobles.
 *
 * <p><b>Importante:</b> en NeoForge, {@code getData} materializa (y persiste) el
 * valor por defecto si el attachment no existe. Por eso SIEMPRE se chequea
 * primero {@code hasData}; nunca exponer {@code getData} crudo hacia afuera.
 */
public final class LockAccess {
    private LockAccess() {}

    /** Devuelve el candado del BE concreto si existe, o {@link Optional#empty()}. */
    public static Optional<LockData> getLock(BlockEntity be) {
        if (be == null || !be.hasData(ModDataAttachments.LOCK_DATA.get())) {
            return Optional.empty();
        }
        return Optional.of(be.getData(ModDataAttachments.LOCK_DATA.get()));
    }

    /** True si el BE concreto tiene un candado aplicado (con o sin PIN). */
    public static boolean hasLock(BlockEntity be) {
        return be != null && be.hasData(ModDataAttachments.LOCK_DATA.get());
    }

    /**
     * Candado efectivo de {@code pos}: mira la mitad clickeada y, si es un cofre
     * doble, también la otra mitad (single source of truth). Así el par queda
     * protegido aunque solo una mitad tenga el dato.
     */
    public static Optional<LockData> getEffectiveLock(BlockGetter level, BlockPos pos) {
        Optional<LockData> here = getLock(level.getBlockEntity(pos));
        if (here.isPresent()) {
            return here;
        }
        BlockPos partner = partnerPos(level.getBlockState(pos), pos);
        return partner == null ? Optional.empty() : getLock(level.getBlockEntity(partner));
    }

    /** La otra mitad de un cofre doble, o null si es single (cofre simple / barril). */
    public static BlockPos partnerPos(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof ChestBlock
                && state.hasProperty(ChestBlock.TYPE)
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return pos.relative(ChestBlock.getConnectedDirection(state));
        }
        return null;
    }

    /**
     * True si {@code player} está autorizado sobre un candado <b>con PIN</b>:
     * admin OP (bypass), dueño, o validado en sesión.
     */
    public static boolean isAuthorized(Player player, BlockPos pos, LockData lock) {
        if (ModConfig.ADMIN_BYPASS.get() && player.hasPermissions(2)) {
            return true; // bypass de moderación (configurable)
        }
        if (lock.isOwner(player.getUUID())) {
            return true;
        }
        return LockSession.isValidated(player.getUUID(), pos);
    }

    /** True si {@code player} puede abrir el contenedor directamente. */
    public static boolean canOpen(Player player, BlockGetter level, BlockPos pos) {
        Optional<LockData> lock = getEffectiveLock(level, pos);
        if (lock.isEmpty() || !lock.get().hasPin()) {
            return true; // sin candado, o candado sin PIN -> abierto para todos
        }
        return isAuthorized(player, pos, lock.get());
    }

    /** True si {@code player} puede setear el PIN (hay candado y todavía no tiene PIN). */
    public static boolean canSetPin(Player player, BlockGetter level, BlockPos pos) {
        Optional<LockData> lock = getEffectiveLock(level, pos);
        return lock.isPresent() && !lock.get().hasPin();
    }

    /** True si {@code player} puede quitar el candado. */
    public static boolean canRemove(Player player, BlockGetter level, BlockPos pos) {
        Optional<LockData> lock = getEffectiveLock(level, pos);
        if (lock.isEmpty()) {
            return false;
        }
        if (!lock.get().hasPin()) {
            return true; // sin PIN: cualquiera que lo aplicó puede sacarlo
        }
        return isAuthorized(player, pos, lock.get());
    }
}
