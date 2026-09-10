package com.digitallock.event;

import com.digitallock.DigitalLock;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

/**
 * Al ganar el logro del candado, anuncia un mensaje llamativo en el chat de TODO
 * el servidor (el logro tiene {@code announce_to_chat:false} para no duplicar).
 */
@EventBusSubscriber(modid = DigitalLock.MODID)
public final class AdvancementEvents {
    private AdvancementEvents() {}

    private static final ResourceLocation PADLOCK_ADVANCEMENT =
            ResourceLocation.fromNamespaceAndPath(DigitalLock.MODID, "digital_padlock");

    @SubscribeEvent
    public static void onEarn(AdvancementEvent.AdvancementEarnEvent event) {
        if (!event.getAdvancement().id().equals(PADLOCK_ADVANCEMENT)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Component msg = Component.literal("★ ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(player.getDisplayName().copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .append(Component.literal(" → ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("El cofre cerrado, pero el orto bien abierto")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD, ChatFormatting.ITALIC));
        server.getPlayerList().broadcastSystemMessage(msg, false);
    }
}
