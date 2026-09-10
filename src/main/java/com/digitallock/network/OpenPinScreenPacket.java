package com.digitallock.network;

import com.digitallock.DigitalLock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** S2C: el servidor le pide al cliente que abra la GUI de ingreso de PIN. */
public record OpenPinScreenPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<OpenPinScreenPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DigitalLock.MODID, "open_pin_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPinScreenPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenPinScreenPacket::pos,
            OpenPinScreenPacket::new);

    @Override
    public Type<OpenPinScreenPacket> type() {
        return TYPE;
    }
}
