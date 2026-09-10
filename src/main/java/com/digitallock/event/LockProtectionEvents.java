package com.digitallock.event;

import com.digitallock.DigitalLock;
import com.digitallock.data.LockAccess;
import com.digitallock.data.LockData;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;

/**
 * Protecciones pasivas de un candado con PIN: rotura, explosión y pistón.
 * Los hoppers/automatización se cortan por capability ({@link CapabilityHandler}).
 * Todas las lecturas usan {@code getEffectiveLock} para cubrir ambas mitades de
 * un cofre doble.
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
        LockData lock = pinLock(event.getLevel(), event.getPos());
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
        event.getAffectedBlocks().removeIf(pos -> pinLock(level, pos) != null);
    }

    /** Los pistones no pueden mover ni destruir un bloque bloqueado (anti-dupe). */
    @SubscribeEvent
    public static void onPiston(PistonEvent.Pre event) {
        LevelAccessor level = event.getLevel();
        PistonStructureResolver helper = event.getStructureHelper();
        if (helper != null && helper.resolve()) {
            for (BlockPos pos : helper.getToPush()) {
                if (pinLock(level, pos) != null) {
                    event.setCanceled(true);
                    return;
                }
            }
            for (BlockPos pos : helper.getToDestroy()) {
                if (pinLock(level, pos) != null) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
        // Bloque adyacente a la cara (caso pull de pistón pegajoso).
        if (pinLock(level, event.getFaceOffsetPos()) != null) {
            event.setCanceled(true);
        }
    }

    /** Devuelve el {@link LockData} si {@code pos} tiene candado con PIN (mirando ambas mitades), o null. */
    private static LockData pinLock(BlockGetter level, BlockPos pos) {
        return LockAccess.getEffectiveLock(level, pos).filter(LockData::hasPin).orElse(null);
    }
}
