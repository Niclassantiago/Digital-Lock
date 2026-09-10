package com.digitallock.config;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Config común del mod (NeoForge {@link ModConfigSpec}). Valores mínimos del
 * briefing: daño por PIN incorrecto, bloques soportados, bypass de admins y si
 * el candado dropea al quitarlo.
 */
public final class ModConfig {
    private ModConfig() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue WRONG_PIN_DAMAGE = BUILDER
            .comment("Daño por PIN incorrecto (4.0 = 2 corazones).")
            .defineInRange("wrongPinDamage", 4.0, 0.0, 1000.0);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SUPPORTED_BLOCKS = BUILDER
            .comment("IDs de los bloques a los que se puede aplicar el candado.")
            .defineListAllowEmpty("supportedBlocks",
                    List.of("minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel"),
                    () -> "minecraft:chest",
                    ModConfig::isValidBlockId);

    public static final ModConfigSpec.BooleanValue ADMIN_BYPASS = BUILDER
            .comment("Si los admins con OP bypassean el candado (abrir / romper).")
            .define("adminBypass", true);

    public static final ModConfigSpec.BooleanValue DROP_LOCK_ON_REMOVE = BUILDER
            .comment("Si al quitar el candado se devuelve el ítem al jugador.")
            .define("dropLockOnRemove", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** True si al bloque de {@code state} se le puede aplicar el candado (según config). */
    public static boolean isSupported(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String key = id.toString();
        for (String supported : SUPPORTED_BLOCKS.get()) {
            if (supported.equals(key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidBlockId(Object obj) {
        return obj instanceof String s
                && ResourceLocation.tryParse(s) != null
                && BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse(s));
    }
}
