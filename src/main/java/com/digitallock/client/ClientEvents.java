package com.digitallock.client;

import com.digitallock.DigitalLock;
import com.digitallock.client.screen.PinSetupScreen;
import com.digitallock.data.LockData;
import com.digitallock.network.RemoveLockPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Eventos client-side de GUI: recuerda el último bloque candeable clickeado e
 * inyecta los botones de candado ("Establecer PIN" / "Quitar candado") en la GUI
 * vanilla del cofre / barril cuando ese bloque tiene candado.
 */
@EventBusSubscriber(modid = DigitalLock.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    /** Último bloque candeable con candado que el jugador clickeó (para asociar con la GUI que abre). */
    private static BlockPos pendingLockPos;

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        boolean lockable = state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock;
        pendingLockPos = (lockable && LockClientCache.view().containsKey(pos)) ? pos.immutable() : null;
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        BlockPos pos = pendingLockPos;
        pendingLockPos = null; // one-shot: solo para la GUI que abre justo después del click
        if (pos == null) {
            return;
        }
        LockData lock = LockClientCache.view().get(pos);
        if (lock == null) {
            return;
        }

        int x = screen.getGuiLeft() + screen.getXSize() + 4;
        int y = screen.getGuiTop();

        // "Establecer PIN": solo si el candado todavía no tiene PIN.
        if (!lock.hasPin()) {
            event.addListener(Button.builder(
                            Component.translatable("screen.digitallock.set_pin_button"),
                            b -> Minecraft.getInstance().setScreen(new PinSetupScreen(pos)))
                    .bounds(x, y, 96, 20).build());
            y += 24;
        }

        // "Quitar candado": si el jugador ve el contenido, está autorizado a sacarlo.
        event.addListener(Button.builder(
                        Component.translatable("screen.digitallock.remove_button"),
                        b -> PacketDistributor.sendToServer(new RemoveLockPacket(pos)))
                .bounds(x, y, 96, 20).build());
    }
}
