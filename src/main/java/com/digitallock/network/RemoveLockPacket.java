package com.digitallock.network;

import com.digitallock.DigitalLock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: quitar el candado de un bloque (dueño / validado / sin PIN). */
public record RemoveLockPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<RemoveLockPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DigitalLock.MODID, "remove_lock"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveLockPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RemoveLockPacket::pos,
            RemoveLockPacket::new);

    @Override
    public Type<RemoveLockPacket> type() {
        return TYPE;
    }
}
