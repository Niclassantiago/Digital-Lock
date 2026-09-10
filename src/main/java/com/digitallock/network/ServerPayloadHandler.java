package com.digitallock.network;

import java.util.Optional;

import com.digitallock.config.ModConfig;
import com.digitallock.data.LockAccess;
import com.digitallock.data.LockData;
import com.digitallock.data.LockManager;
import com.digitallock.data.LockSession;
import com.digitallock.data.PinHasher;
import com.digitallock.registry.ModDamageTypes;
import com.digitallock.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Handlers server-side de los packets C2S. Todo se valida acá (server-side
 * authoritative): el cliente nunca decide permisos ni si el PIN es correcto.
 */
public final class ServerPayloadHandler {
    private ServerPayloadHandler() {}

    /** Distancia máxima (al cuadrado) para aceptar una acción sobre un bloque. */
    private static final double REACH_SQR = 64.0; // 8 bloques

    public static void handleSetPin(SetPinPacket packet, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        String pin = packet.pin();
        if (pin == null || !pin.matches("\\d{4}")) {
            return;
        }
        BlockPos pos = packet.pos();
        if (!inReach(player, pos)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || !LockAccess.canSetPin(player, level, pos)) {
            return;
        }
        LockManager.applyPin(level, pos, player.getUUID(), pin);
        level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.7f, 1.3f);
    }

    public static void handleSubmitPin(SubmitPinPacket packet, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        String pin = packet.pin();
        if (pin == null || !pin.matches("\\d{4}")) {
            return;
        }
        BlockPos pos = packet.pos();
        if (!inReach(player, pos)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(pos);
        Optional<LockData> lock = LockAccess.getEffectiveLock(level, pos);
        if (lock.isEmpty() || !lock.get().hasPin()) {
            openContainer(player, level, pos, state); // ya no está bloqueado
            return;
        }
        LockData data = lock.get();
        boolean ok = data.salt().isPresent() && data.pinHash().isPresent()
                && PinHasher.verify(pin, data.salt().get(), data.pinHash().get());
        if (ok) {
            // Validar ambas mitades del cofre doble para esta sesión.
            LockManager.forEachHalf(level, pos, half -> LockSession.validate(player.getUUID(), half));
            level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.7f, 1.2f);
            openContainer(player, level, pos, state);
        } else {
            player.hurt(ModDamageTypes.wrongPin(level), (float) (double) ModConfig.WRONG_PIN_DAMAGE.get());
            level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.8f, 0.8f);
        }
    }

    public static void handleRemoveLock(RemoveLockPacket packet, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        BlockPos pos = packet.pos();
        if (!inReach(player, pos)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || !LockAccess.canRemove(player, level, pos)) {
            return;
        }
        if (LockManager.removeLock(level, pos)) {
            level.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 0.8f, 1.0f);
            if (ModConfig.DROP_LOCK_ON_REMOVE.get()) {
                ItemStack stack = new ItemStack(ModItems.DIGITAL_PADLOCK.get());
                if (!player.addItem(stack)) {
                    player.drop(stack, false);
                }
            }
        }
    }

    /** Abre el contenedor vanilla para el jugador (getMenuProvider maneja cofres dobles). */
    private static void openContainer(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        MenuProvider provider = state.getMenuProvider(level, pos);
        if (provider != null) {
            player.openMenu(provider);
        }
    }

    static boolean inReach(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= REACH_SQR;
    }
}
