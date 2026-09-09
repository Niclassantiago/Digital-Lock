package com.digitallock.registry;

import com.digitallock.DigitalLock;
import com.digitallock.client.LockClientCache;
import com.digitallock.network.LockSyncPacket;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registro de los packets del mod (sistema nuevo de NeoForge 1.21.1 vía
 * {@link RegisterPayloadHandlersEvent}).
 */
@EventBusSubscriber(modid = DigitalLock.MODID)
public final class ModPayloads {
    private ModPayloads() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(DigitalLock.MODID);

        // S2C: sincroniza el estado del candado al cliente para el render.
        // El handler corre solo en el cliente; LockClientCache no importa código
        // client-only, así que es seguro referenciarlo desde acá.
        registrar.playToClient(
                LockSyncPacket.TYPE,
                LockSyncPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> LockClientCache.handle(payload)));
    }
}
