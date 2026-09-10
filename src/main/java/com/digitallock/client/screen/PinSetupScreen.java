package com.digitallock.client.screen;

import com.digitallock.network.SetPinPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Teclado para setear un PIN de 4 dígitos. Manda {@link SetPinPacket} al servidor. */
public class PinSetupScreen extends PinKeypadScreen {

    public PinSetupScreen(BlockPos pos) {
        super(Component.translatable("screen.digitallock.set_pin"), pos);
    }

    @Override
    protected void onConfirm(String pin) {
        PacketDistributor.sendToServer(new SetPinPacket(this.pos, pin));
    }
}
