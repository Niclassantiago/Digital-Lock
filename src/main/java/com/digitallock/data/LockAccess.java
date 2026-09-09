package com.digitallock.data;

import java.util.Optional;

import com.digitallock.registry.ModDataAttachments;

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
}
