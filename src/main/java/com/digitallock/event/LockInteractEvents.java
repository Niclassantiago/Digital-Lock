package com.digitallock.event;

import java.util.Optional;

import com.digitallock.DigitalLock;
import com.digitallock.data.LockAccess;
import com.digitallock.data.LockData;
import com.digitallock.data.LockSession;
import com.digitallock.network.OpenPinScreenPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Control de acceso server-side: al abrir un cofre / barril bloqueado con PIN,
 * el dueño / validado / admin abre normal; el resto recibe la GUI de ingreso de
 * PIN en vez del contenedor.
 */
@EventBusSubscriber(modid = DigitalLock.MODID)
public final class LockInteractEvents {
    private LockInteractEvents() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock)) {
            return;
        }
        // Agachado + ítem en mano = interacción del ítem (p. ej. aplicar candado), no abrir.
        if (player.isSecondaryUseActive() && !player.getItemInHand(event.getHand()).isEmpty()) {
            return;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return;
        }
        Optional<LockData> lock = LockAccess.getLock(be);
        if (lock.isEmpty() || !lock.get().hasPin()) {
            return; // sin candado o sin PIN -> abrir normal
        }
        if (LockAccess.isAuthorized(player, pos, lock.get())) {
            return; // dueño / validado / admin -> abrir normal
        }
        // No autorizado: cancelar la apertura y mandar la GUI de PIN.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new OpenPinScreenPacket(pos.immutable()));
        }
    }

    /** Limpia las validaciones de sesión del jugador al desloguear. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LockSession.clear(event.getEntity().getUUID());
    }
}
