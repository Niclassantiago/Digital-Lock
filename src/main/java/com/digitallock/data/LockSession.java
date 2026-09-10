package com.digitallock.data;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;

/**
 * Registro server-side (en memoria) de qué jugadores validaron el PIN de qué
 * bloque en esta sesión. No se persiste: se limpia al desloguear al jugador o
 * al reiniciar el servidor. Un jugador que ingresó el PIN correcto queda
 * habilitado para abrir ese bloque sin re-ingresarlo mientras dure la sesión.
 */
public final class LockSession {
    private LockSession() {}

    private static final Map<UUID, Set<Long>> VALIDATED = new ConcurrentHashMap<>();

    public static void validate(UUID player, BlockPos pos) {
        VALIDATED.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).add(pos.asLong());
    }

    public static boolean isValidated(UUID player, BlockPos pos) {
        Set<Long> set = VALIDATED.get(player);
        return set != null && set.contains(pos.asLong());
    }

    /** Limpia todas las validaciones de un jugador (al desloguear). */
    public static void clear(UUID player) {
        VALIDATED.remove(player);
    }

    /** Invalida un bloque para todos (al quitar el candado). */
    public static void invalidate(BlockPos pos) {
        long key = pos.asLong();
        for (Set<Long> set : VALIDATED.values()) {
            set.remove(key);
        }
    }
}
