package com.digitallock.data;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Estado del candado adjuntado (Data Attachment de NeoForge) al BlockEntity
 * vanilla del cofre / cofre trampa / barril. No reemplaza el BlockEntity: se
 * guarda como attachment sobre el BE existente.
 *
 * <p>Estados posibles:
 * <ul>
 *   <li><b>Aplicado sin PIN</b> → {@code pinHash} y {@code salt} vacíos,
 *       {@code ownerId = Util.NIL_UUID} (todavía no hay dueño). Indicador verde.</li>
 *   <li><b>Con PIN</b> → {@code pinHash}/{@code salt} presentes y {@code ownerId}
 *       = quien seteó el PIN. Indicador rojo.</li>
 * </ul>
 *
 * <p>El {@code ownerId} es "quien seteó el PIN", no quien aplicó el candado
 * físico (ver briefing). Hasta que se setea el PIN es {@link Util#NIL_UUID}.
 */
public record LockData(
        UUID ownerId,
        Optional<byte[]> pinHash,
        Optional<byte[]> salt,
        long createdAt
) {
    /** Codec de byte[] serializado como Base64 (amigable con NBT y JSON). */
    private static final Codec<byte[]> BYTE_ARRAY_CODEC = Codec.STRING.xmap(
            Base64.getDecoder()::decode,
            Base64.getEncoder()::encodeToString);

    /** Codec para persistir el attachment en NBT. */
    public static final Codec<LockData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("ownerId").forGetter(LockData::ownerId),
            BYTE_ARRAY_CODEC.optionalFieldOf("pinHash").forGetter(LockData::pinHash),
            BYTE_ARRAY_CODEC.optionalFieldOf("salt").forGetter(LockData::salt),
            Codec.LONG.fieldOf("createdAt").forGetter(LockData::createdAt)
    ).apply(instance, LockData::new));

    /** StreamCodec para sincronizar al cliente. */
    public static final StreamCodec<RegistryFriendlyByteBuf, LockData> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, LockData::ownerId,
            ByteBufCodecs.optional(ByteBufCodecs.BYTE_ARRAY), LockData::pinHash,
            ByteBufCodecs.optional(ByteBufCodecs.BYTE_ARRAY), LockData::salt,
            ByteBufCodecs.VAR_LONG, LockData::createdAt,
            LockData::new);

    /**
     * Valor por defecto del attachment. Solo se materializa si alguien llama a
     * {@code getData} sin chequear antes {@code hasData}; el código siempre pasa
     * por {@link LockAccess}, que hace ese chequeo. No usar como "candado real".
     */
    public static final LockData EMPTY = new LockData(Util.NIL_UUID, Optional.empty(), Optional.empty(), 0L);

    /** Candado recién aplicado: sin PIN ni dueño todavía. */
    public static LockData freshUnlocked(long createdAt) {
        return new LockData(Util.NIL_UUID, Optional.empty(), Optional.empty(), createdAt);
    }

    /** True si el candado ya tiene un PIN seteado (está "bloqueado"). */
    public boolean hasPin() {
        return pinHash.isPresent();
    }

    /** True si {@code playerId} es el dueño (quien seteó el PIN). */
    public boolean isOwner(UUID playerId) {
        return hasPin() && ownerId.equals(playerId);
    }
}
