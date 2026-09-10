package com.digitallock.client.screen;

import com.digitallock.network.SubmitPinPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Pantalla de ingreso de PIN para un jugador no autorizado. Manda
 * {@link SubmitPinPacket} y se cierra: el servidor decide si abre el cofre
 * (PIN correcto) o aplica daño (PIN incorrecto).
 */
public class PinEntryScreen extends Screen {

    private final BlockPos pos;
    private EditBox pinField;

    public PinEntryScreen(BlockPos pos) {
        super(Component.translatable("screen.digitallock.enter_pin"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.pinField = new EditBox(this.font, cx - 60, cy - 10, 120, 20,
                Component.translatable("screen.digitallock.pin_field"));
        this.pinField.setMaxLength(4);
        this.pinField.setFilter(s -> s.isEmpty() || s.matches("[0-9]+"));
        this.addRenderableWidget(this.pinField);
        this.setInitialFocus(this.pinField);

        this.addRenderableWidget(Button.builder(Component.translatable("screen.digitallock.confirm"), b -> submit())
                .bounds(cx - 60, cy + 20, 58, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> this.onClose())
                .bounds(cx + 2, cy + 20, 58, 20).build());
    }

    private void submit() {
        String pin = this.pinField.getValue();
        if (pin.length() == 4) {
            PacketDistributor.sendToServer(new SubmitPinPacket(this.pos, pin));
            this.onClose(); // el servidor abre el cofre (correcto) o aplica daño (incorrecto)
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
