package com.digitallock.registry;

import com.digitallock.DigitalLock;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/**
 * Tipo de daño por PIN incorrecto. El {@link DamageType} en sí se define por
 * datapack en {@code data/digitallock/damage_type/wrong_pin.json}; acá está la
 * key y el helper para construir el {@link DamageSource}.
 */
public final class ModDamageTypes {
    private ModDamageTypes() {}

    public static final ResourceKey<DamageType> WRONG_PIN = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(DigitalLock.MODID, "wrong_pin"));

    public static DamageSource wrongPin(Level level) {
        return new DamageSource(level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(WRONG_PIN));
    }
}
