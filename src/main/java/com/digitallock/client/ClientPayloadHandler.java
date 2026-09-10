package com.digitallock.client;

import com.digitallock.client.screen.PinEntryScreen;
import com.digitallock.network.OpenPinScreenPacket;

import net.minecraft.client.Minecraft;

/**
 * Handlers client-side de packets S2C. Solo se clase-carga en el cliente (se
 * referencia únicamente desde handlers que corren en el cliente).
 */
public final class ClientPayloadHandler {
    private ClientPayloadHandler() {}

    public static void openPinScreen(OpenPinScreenPacket packet) {
        Minecraft.getInstance().setScreen(new PinEntryScreen(packet.pos()));
    }
}
