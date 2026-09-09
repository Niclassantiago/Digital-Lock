package com.digitallock;

import org.slf4j.Logger;

import com.digitallock.registry.ModDataAttachments;
import com.digitallock.registry.ModItems;
import com.mojang.logging.LogUtils;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

// El valor debe coincidir con una entrada en META-INF/neoforge.mods.toml
@Mod(DigitalLock.MODID)
public class DigitalLock {
    // Mod id centralizado para que todo lo referencie
    public static final String MODID = "digitallock";
    // Logger slf4j directo
    public static final Logger LOGGER = LogUtils.getLogger();

    // El constructor del mod es lo primero que corre al cargar. FML reconoce
    // algunos tipos de parámetro (IEventBus, ModContainer) y los inyecta.
    public DigitalLock(IEventBus modEventBus, ModContainer modContainer) {
        // Registros del mod al mod event bus
        ModItems.register(modEventBus);
        ModDataAttachments.register(modEventBus);

        // Agregar el candado a una pestaña creativa vanilla
        modEventBus.addListener(this::addCreative);

        // Config de ejemplo heredada del generator. Se reemplaza por la config
        // real del mod en la Etapa 11.
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        LOGGER.info("Digital Lock inicializado");
    }

    /** Agrega el candado digital a la pestaña "Herramientas y utilidades". */
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.DIGITAL_PADLOCK);
        }
    }
}
