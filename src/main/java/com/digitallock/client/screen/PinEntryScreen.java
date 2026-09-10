package com.digitallock.client.screen;

import com.digitallock.network.SubmitPinPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Teclado de ingreso de PIN para un jugador no autorizado. Manda
 * {@link SubmitPinPacket} y se cierra: el servidor decide si abre el cofre
 * (PIN correcto) o aplica daño (PIN incorrecto).
 */
public class PinEntryScreen extends PinKeypadScreen {

    public PinEntryScreen(BlockPos pos) {
        super(Component.translatable("screen.digitallock.enter_pin"), pos);
    }

    @Override
    protected void onConfirm(String pin) {
        PacketDistributor.sendToServer(new SubmitPinPacket(this.pos, pin));
    }
}
