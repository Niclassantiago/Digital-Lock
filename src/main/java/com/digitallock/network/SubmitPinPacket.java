package com.digitallock.network;

import com.digitallock.DigitalLock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: un jugador no-dueño intenta el PIN. Se valida server-side. */
public record SubmitPinPacket(BlockPos pos, String pin) implements CustomPacketPayload {

    public static final Type<SubmitPinPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DigitalLock.MODID, "submit_pin"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SubmitPinPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SubmitPinPacket::pos,
            ByteBufCodecs.stringUtf8(16), SubmitPinPacket::pin,
            SubmitPinPacket::new);

    @Override
    public Type<SubmitPinPacket> type() {
        return TYPE;
    }
}
