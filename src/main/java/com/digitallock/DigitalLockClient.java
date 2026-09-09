package com.digitallock;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// Esta clase no se carga en servidores dedicados. Es seguro acceder a código
// client-side desde acá.
@Mod(value = DigitalLock.MODID, dist = Dist.CLIENT)
public class DigitalLockClient {
    public DigitalLockClient(ModContainer container) {
        // Permite abrir la pantalla de config del mod desde el menú de Mods.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
