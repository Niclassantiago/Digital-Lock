package com.digitallock.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * Pantalla base con teclado numérico para ingresar un PIN de 4 dígitos: botones
 * 0-9, borrar y limpiar, con un beep en cada tecla. Las subclases implementan
 * {@link #onConfirm(String)} para mandar el packet correspondiente.
 */
public abstract class PinKeypadScreen extends Screen {

    protected final BlockPos pos;
    private final StringBuilder pin = new StringBuilder();
    private Button confirmButton;

    protected PinKeypadScreen(Component title, BlockPos pos) {
        super(title);
        this.pos = pos;
    }

    /** Se llama con el PIN completo (4 dígitos) al confirmar. */
    protected abstract void onConfirm(String pin);

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int bw = 40;
        int bh = 24;
        int gap = 4;
        int gridW = bw * 3 + gap * 2;
        int left = cx - gridW / 2;
        int top = cy - 24;

        // Dígitos 1-9 en grilla 3x3.
        for (int i = 0; i < 9; i++) {
            int digit = i + 1;
            int col = i % 3;
            int row = i / 3;
            addKey(String.valueOf(digit), left + col * (bw + gap), top + row * (bh + gap), bw, bh, () -> pressDigit(digit));
        }
        // Fila inferior: Limpiar, 0, Borrar.
        int rowY = top + 3 * (bh + gap);
        addKey("C", left, rowY, bw, bh, this::clear);
        addKey("0", left + (bw + gap), rowY, bw, bh, () -> pressDigit(0));
        addKey("←", left + 2 * (bw + gap), rowY, bw, bh, this::backspace);

        // Confirmar / Cancelar.
        int confY = rowY + bh + gap + 6;
        this.confirmButton = Button.builder(Component.translatable("screen.digitallock.confirm"), b -> confirm())
                .bounds(left, confY, gridW / 2 - 2, 20).build();
        this.confirmButton.active = false;
        addRenderableWidget(this.confirmButton);
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> this.onClose())
                .bounds(left + gridW / 2 + 2, confY, gridW / 2 - 2, 20).build());
    }

    private void addKey(String label, int x, int y, int w, int h, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> action.run()).bounds(x, y, w, h).build());
    }

    private void pressDigit(int digit) {
        if (pin.length() < 4) {
            pin.append(digit);
            beep(0.9f + digit * 0.06f);
            refresh();
        }
    }

    private void backspace() {
        if (pin.length() > 0) {
            pin.deleteCharAt(pin.length() - 1);
            beep(0.7f);
            refresh();
        }
    }

    private void clear() {
        if (pin.length() > 0) {
            pin.setLength(0);
            beep(0.6f);
            refresh();
        }
    }

    private void confirm() {
        if (pin.length() == 4) {
            onConfirm(pin.toString());
            onClose();
        }
    }

    private void refresh() {
        if (this.confirmButton != null) {
            this.confirmButton.active = pin.length() == 4;
        }
    }

    private void beep(float pitch) {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, pitch));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= 48 && keyCode <= 57) { // fila superior 0-9
            pressDigit(keyCode - 48);
            return true;
        }
        if (keyCode >= 320 && keyCode <= 329) { // teclado numérico 0-9
            pressDigit(keyCode - 320);
            return true;
        }
        if (keyCode == 259) { // backspace
            backspace();
            return true;
        }
        if (keyCode == 257 || keyCode == 335) { // enter
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        int cy = this.height / 2;
        guiGraphics.drawCenteredString(this.font, this.title, cx, cy - 64, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, pinDisplay(), cx, cy - 46, 0xFFFF55);
    }

    /** Muestra los dígitos ingresados como puntos y los faltantes como guiones. */
    private String pinDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append(i < pin.length() ? '●' : '―');
            if (i < 3) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
