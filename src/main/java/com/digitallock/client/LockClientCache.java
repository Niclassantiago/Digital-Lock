package com.digitallock.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.digitallock.data.LockData;
import com.digitallock.network.LockSyncPacket;

import net.minecraft.core.BlockPos;

/**
 * Cache client-side de los candados conocidos, alimentado por
 * {@link LockSyncPacket}. Lo consume el renderer para dibujar el candado sobre
 * la cara del bloque.
 *
 * <p>No importa código client-only a propósito: así se puede referenciar de
 * forma segura desde el registro de packets (código común). Se limpia al
 * descargar el mundo (ver el renderer).
 */
public final class LockClientCache {
    private LockClientCache() {}

    private static final Map<BlockPos, LockData> LOCKS = new ConcurrentHashMap<>();

    /** Aplica un packet de sync: presente = guardar/actualizar, vacío = remover. */
    public static void handle(LockSyncPacket packet) {
        packet.data().ifPresentOrElse(
                data -> LOCKS.put(packet.pos().immutable(), data),
                () -> LOCKS.remove(packet.pos()));
    }

    /** Vista de solo lectura para el renderer (mapa concurrente, seguro de iterar). */
    public static Map<BlockPos, LockData> view() {
        return LOCKS;
    }

    public static void clear() {
        LOCKS.clear();
    }
}
