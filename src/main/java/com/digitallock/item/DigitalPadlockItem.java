package com.digitallock.item;

import com.digitallock.config.ModConfig;
import com.digitallock.data.LockAccess;
import com.digitallock.data.LockManager;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ítem candado digital. Con Shift + click derecho sobre un cofre, cofre trampa
 * o barril, adjunta un candado sin PIN al BlockEntity vanilla del bloque
 * (ambas mitades si es cofre doble) y consume el ítem.
 *
 * <p>La whitelist de bloques está hardcodeada por ahora (pasa a config en la
 * Etapa 11).
 */
public class DigitalPadlockItem extends Item {

    public DigitalPadlockItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // Solo con Shift (secondary use). Sin Shift dejamos pasar para que el
        // bloque haga su interacción normal (abrir el contenedor).
        if (!context.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!isSupported(state)) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        // En el cliente devolvemos éxito para el swing; la lógica real es
        // server-side authoritative.
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return InteractionResult.PASS;
        }

        // Ya tiene candado (mirando ambas mitades): avisar y no gastar el ítem.
        if (LockAccess.getEffectiveLock(level, pos).isPresent()) {
            player.displayClientMessage(Component.translatable("message.digitallock.already_locked"), true);
            return InteractionResult.CONSUME;
        }

        // Adjuntar el candado sin PIN (a ambas mitades si es cofre doble),
        // persistir y sincronizar a los clientes que trackean el chunk.
        LockManager.applyLock((ServerLevel) level, pos);
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8f, 1.2f);

        // Consumir el ítem salvo en creativo.
        if (!player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        player.displayClientMessage(Component.translatable("message.digitallock.applied"), true);
        return InteractionResult.CONSUME;
    }

    /** Bloques soportados (whitelist de config). */
    private static boolean isSupported(BlockState state) {
        return ModConfig.isSupported(state);
    }
}
