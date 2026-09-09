package com.digitallock.network;

import java.util.Optional;

import com.digitallock.DigitalLock;
import com.digitallock.data.LockData;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet S2C que sincroniza el estado del candado de un bloque al cliente para
 * poder renderizarlo. {@code data} vacío significa "sacá este candado del cache"
 * (se usa al remover el candado, Etapa 7).
 */
public record LockSyncPacket(BlockPos pos, Optional<LockData> data) implements CustomPacketPayload {

    public static final Type<LockSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DigitalLock.MODID, "lock_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LockSyncPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LockSyncPacket::pos,
            ByteBufCodecs.optional(LockData.STREAM_CODEC), LockSyncPacket::data,
            LockSyncPacket::new);

    @Override
    public Type<LockSyncPacket> type() {
        return TYPE;
    }
}
