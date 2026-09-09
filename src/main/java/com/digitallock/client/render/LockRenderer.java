package com.digitallock.client.render;

import java.util.Map;

import com.digitallock.DigitalLock;
import com.digitallock.client.LockClientCache;
import com.digitallock.data.LockData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Dibuja el candado sobre la cara frontal ({@code FACING}) de cada cofre /
 * barril bloqueado conocido por el cliente ({@link LockClientCache}).
 *
 * <p>Etapa 3: usa la textura del ítem como placeholder. Cuando lleguen las
 * texturas definitivas se reemplaza {@link #TEXTURE} (y, si hace falta, se
 * distingue el estado sin PIN vs con PIN mirando {@link LockData#hasPin()}).
 */
@EventBusSubscriber(modid = DigitalLock.MODID, value = Dist.CLIENT)
public final class LockRenderer {
    private LockRenderer() {}

    // Placeholder: se reemplaza cuando lleguen las texturas definitivas.
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DigitalLock.MODID, "textures/item/digital_padlock.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);

    private static final float HALF = 0.18f;   // medio lado del candado (~0.36 de lado)
    private static final float OFFSET = 0.02f; // separación de la cara para evitar z-fighting

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Map<BlockPos, LockData> locks = LockClientCache.view();
        if (locks.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            return;
        }

        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RENDER_TYPE);

        for (Map.Entry<BlockPos, LockData> entry : locks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = level.getBlockState(pos);
            Direction facing = frontFace(state);
            if (facing == null) {
                continue; // el bloque ya no es un cofre/barril (roto, chunk sin cargar, etc.)
            }
            int light = LevelRenderer.getLightColor(level, pos.relative(facing));

            pose.pushPose();
            pose.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
            drawLock(vc, pose, facing, light);
            pose.popPose();
        }

        buffers.endBatch(RENDER_TYPE);
    }

    /** Limpia el cache al descargar el nivel (cambio de dimensión / desconexión). */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            LockClientCache.clear();
        }
    }

    /** Cara frontal del bloque, o null si no es un bloque candeable. */
    private static Direction frontFace(BlockState state) {
        if (state.getBlock() instanceof ChestBlock) { // cubre cofre trampa (subclase)
            return state.getValue(ChestBlock.FACING);
        }
        if (state.getBlock() instanceof BarrelBlock) {
            return state.getValue(BarrelBlock.FACING);
        }
        return null;
    }

    /** Dibuja un quad texturizado sobre la cara {@code facing}, en coords locales 0..1. */
    private static void drawLock(VertexConsumer vc, PoseStack pose, Direction facing, int light) {
        // Centro del quad, un pelo por fuera de la cara del bloque.
        float d = 0.5f + OFFSET;
        float cx = 0.5f + facing.getStepX() * d;
        float cy = 0.5f + facing.getStepY() * d;
        float cz = 0.5f + facing.getStepZ() * d;

        // Ejes del quad: "right" horizontal perpendicular a la cara, "up" mundial.
        float rx, ry = 0f, rz;
        float ux = 0f, uy = 1f, uz = 0f;
        switch (facing) {
            case NORTH -> { rx = 1;  rz = 0; }
            case SOUTH -> { rx = -1; rz = 0; }
            case EAST  -> { rx = 0;  rz = 1; }
            case WEST  -> { rx = 0;  rz = -1; }
            case UP    -> { rx = 1;  rz = 0; ux = 0; uy = 0; uz = -1; }
            default    -> { rx = 1;  rz = 0; ux = 0; uy = 0; uz = 1; } // DOWN
        }

        PoseStack.Pose last = pose.last();
        float nx = facing.getStepX();
        float ny = facing.getStepY();
        float nz = facing.getStepZ();

        // Esquinas: BL, BR, TR, TL (con winding no-cull, se ve de ambos lados).
        putVertex(vc, last, cx - rx * HALF - ux * HALF, cy - ry * HALF - uy * HALF, cz - rz * HALF - uz * HALF, 0f, 1f, light, nx, ny, nz);
        putVertex(vc, last, cx + rx * HALF - ux * HALF, cy + ry * HALF - uy * HALF, cz + rz * HALF - uz * HALF, 1f, 1f, light, nx, ny, nz);
        putVertex(vc, last, cx + rx * HALF + ux * HALF, cy + ry * HALF + uy * HALF, cz + rz * HALF + uz * HALF, 1f, 0f, light, nx, ny, nz);
        putVertex(vc, last, cx - rx * HALF + ux * HALF, cy - ry * HALF + uy * HALF, cz - rz * HALF + uz * HALF, 0f, 0f, light, nx, ny, nz);
    }

    private static void putVertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z,
                                  float u, float v, int light, float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
