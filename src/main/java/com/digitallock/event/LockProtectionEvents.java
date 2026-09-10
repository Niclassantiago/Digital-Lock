package com.digitallock.event;

import com.digitallock.DigitalLock;
import com.digitallock.data.LockAccess;
import com.digitallock.data.LockData;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;

/**
 * Protecciones pasivas de un candado con PIN: rotura, explosión y pistón.
 * Los hoppers/automatización se cortan por capability ({@link CapabilityHandler}).
 */
@EventBusSubscriber(modid = DigitalLock.MODID)
public final class LockProtectionEvents {
    private LockProtectionEvents() {}

    /** Cofre bloqueado = irrompible salvo para dueño / validado / admin OP. */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        LockData lock = pinLock(be);
        if (lock == null) {
            return;
        }
        Player player = event.getPlayer();
        if (!LockAccess.isAuthorized(player, event.getPos(), lock)) {
            event.setCanceled(true);
        }
    }

    /** Las explosiones no afectan bloques bloqueados con PIN. */
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        event.getAffectedBlocks().removeIf(pos -> pinLock(level.getBlockEntity(pos)) != null);
    }

    /** Los pistones no pueden mover ni destruir un bloque bloqueado (anti-dupe). */
    @SubscribeEvent
    public static void onPiston(PistonEvent.Pre event) {
        LevelAccessor level = event.getLevel();
        PistonStructureResolver helper = event.getStructureHelper();
        if (helper != null && helper.resolve()) {
            for (BlockPos pos : helper.getToPush()) {
                if (pinLock(level.getBlockEntity(pos)) != null) {
                    event.setCanceled(true);
                    return;
                }
            }
            for (BlockPos pos : helper.getToDestroy()) {
                if (pinLock(level.getBlockEntity(pos)) != null) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
        // Bloque adyacente a la cara (caso pull de pistón pegajoso).
        if (pinLock(level.getBlockEntity(event.getFaceOffsetPos())) != null) {
            event.setCanceled(true);
        }
    }

    /** Devuelve el {@link LockData} si el BE tiene candado con PIN, o null. */
    private static LockData pinLock(BlockEntity be) {
        return LockAccess.getLock(be).filter(LockData::hasPin).orElse(null);
    }
}
