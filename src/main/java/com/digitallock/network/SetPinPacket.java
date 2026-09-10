package com.digitallock.network;

import com.digitallock.DigitalLock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: el jugador setea el PIN de un candado sin PIN. Se valida server-side. */
public record SetPinPacket(BlockPos pos, String pin) implements CustomPacketPayload {

    public static final Type<SetPinPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DigitalLock.MODID, "set_pin"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetPinPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetPinPacket::pos,
            ByteBufCodecs.stringUtf8(16), SetPinPacket::pin,
            SetPinPacket::new);

    @Override
    public Type<SetPinPacket> type() {
        return TYPE;
    }
}
