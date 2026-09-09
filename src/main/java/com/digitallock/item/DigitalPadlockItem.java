package com.digitallock.item;

import com.digitallock.data.LockAccess;
import com.digitallock.data.LockData;
import com.digitallock.network.LockSync;
import com.digitallock.registry.ModDataAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ítem candado digital. Con Shift + click derecho sobre un cofre, cofre trampa
 * o barril, adjunta un {@link LockData} sin PIN al BlockEntity vanilla del
 * bloque y consume el ítem.
 *
 * <p><b>Etapa 2:</b> solo aplica el candado (sin PIN). El PIN, la GUI y las
 * protecciones llegan en etapas posteriores. La whitelist de bloques está
 * hardcodeada por ahora (pasa a config en la Etapa 11) y solo se escribe la
 * mitad clickeada de un cofre doble (la sincronización de halves es Etapa 9).
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

        // Ya tiene candado: avisar y no gastar el ítem.
        if (LockAccess.hasLock(be)) {
            player.displayClientMessage(Component.translatable("message.digitallock.already_locked"), true);
            return InteractionResult.CONSUME;
        }

        // Adjuntar el candado sin PIN y marcar el BE como modificado para que
        // persista en NBT.
        be.setData(ModDataAttachments.LOCK_DATA.get(), LockData.freshUnlocked(level.getGameTime()));
        be.setChanged();

        // Sincronizar a los clientes que trackean el chunk para que dibujen el
        // candado sobre la cara del bloque.
        LockSync.sendToTrackers((ServerLevel) level, pos);

        // Consumir el ítem salvo en creativo.
        if (!player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        player.displayClientMessage(Component.translatable("message.digitallock.applied"), true);
        return InteractionResult.CONSUME;
    }

    /** Bloques soportados. Hardcodeado por ahora; pasa a config en la Etapa 11. */
    private static boolean isSupported(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL;
    }
}
