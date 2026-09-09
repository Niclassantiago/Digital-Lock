package com.digitallock.registry;

import com.digitallock.DigitalLock;
import com.digitallock.data.LockData;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Registro de los Data Attachments del mod. El candado se guarda como un
 * {@link AttachmentType} sobre el BlockEntity vanilla del cofre / barril, sin
 * crear un BlockEntity propio.
 */
public final class ModDataAttachments {
    private ModDataAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, DigitalLock.MODID);

    /**
     * Attachment con el estado del candado. Persiste en NBT vía
     * {@link LockData#CODEC}. La sincronización al cliente (para el renderer) se
     * hace con un packet S2C a partir de la Etapa 3.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<LockData>> LOCK_DATA =
            ATTACHMENT_TYPES.register("lock_data", () -> AttachmentType.builder(() -> LockData.EMPTY)
                    .serialize(LockData.CODEC)
                    .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
