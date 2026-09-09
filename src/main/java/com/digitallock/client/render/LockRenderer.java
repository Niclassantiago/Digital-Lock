package com.digitallock.client.render;

import java.util.Map;

import com.digitallock.DigitalLock;
import com.digitallock.client.LockClientCache;
import com.digitallock.data.LockData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

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
 * Dibuja un candado 3D sobresaliendo de la cara frontal ({@code FACING}) de cada
 * cofre / barril bloqueado conocido por el cliente ({@link LockClientCache}).
 *
 * <p>El candado se modela como cajas en coordenadas de bloque, con la geometría
 * autorada mirando al SUR (+Z, igual que el modelo del cofre vanilla); la
 * orientación real se resuelve rotando con la misma fórmula que usa el
 * BlockEntityRenderer del cofre ({@code -FACING.toYRot()}).
 *
 * <p>Etapa 3: texturas placeholder simples (cuerpo con teclado 3×3 + arco gris).
 * Un solo estado visual (sin PIN y con PIN se ven igual, por decisión del autor).
 */
@EventBusSubscriber(modid = DigitalLock.MODID, value = Dist.CLIENT)
public final class LockRenderer {
    private LockRenderer() {}

    private static final ResourceLocation BODY_TEX =
            ResourceLocation.fromNamespaceAndPath(DigitalLock.MODID, "textures/block/digital_padlock_body.png");
    private static final ResourceLocation METAL_TEX =
            ResourceLocation.fromNamespaceAndPath(DigitalLock.MODID, "textures/block/digital_padlock_metal.png");
    private static final RenderType RT_BODY = RenderType.entityCutoutNoCull(BODY_TEX);
    private static final RenderType RT_METAL = RenderType.entityCutoutNoCull(METAL_TEX);

    // Cajas del candado en coords de bloque [0..1], autoradas mirando al SUR (+Z)
    // y sobresaliendo de la cara (z > 1). Formato: {x0, y0, z0, x1, y1, z1}.
    private static final float U = 1f / 16f;
    private static final float[] BODY   = { 4 * U,  2 * U, 13 * U, 12 * U, 11 * U, 18 * U };
    private static final float[] LEG_L  = { 5 * U, 10 * U, 14 * U,  7 * U, 14 * U, 17 * U };
    private static final float[] LEG_R  = { 9 * U, 10 * U, 14 * U, 11 * U, 14 * U, 17 * U };
    private static final float[] TOP    = { 5 * U, 13 * U, 14 * U, 11 * U, 15 * U, 17 * U };

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
            orient(pose, facing);

            drawBox(buffers.getBuffer(RT_BODY), pose, BODY, light);
            VertexConsumer metal = buffers.getBuffer(RT_METAL);
            drawBox(metal, pose, LEG_L, light);
            drawBox(metal, pose, LEG_R, light);
            drawBox(metal, pose, TOP, light);

            pose.popPose();
        }

        buffers.endBatch();
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

    /** Rota la geometría (autorada al sur) para que mire hacia {@code facing}, girando en el centro del bloque. */
    private static void orient(PoseStack pose, Direction facing) {
        pose.translate(0.5, 0.5, 0.5);
        if (facing.getAxis().isHorizontal()) {
            // Misma fórmula que el ChestRenderer vanilla (modelo por defecto mira al sur).
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        } else {
            pose.mulPose(Axis.XP.rotationDegrees(facing == Direction.UP ? -90f : 90f));
        }
        pose.translate(-0.5, -0.5, -0.5);
    }

    /** Dibuja las 6 caras de una caja [x0,y0,z0]-[x1,y1,z1] en coords de bloque. */
    private static void drawBox(VertexConsumer vc, PoseStack pose, float[] b, int light) {
        float x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
        PoseStack.Pose p = pose.last();
        quad(vc, p, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, light);   // sur  (+Z)
        quad(vc, p, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, light);  // norte(-Z)
        quad(vc, p, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0, light);   // este (+X)
        quad(vc, p, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, light);  // oeste(-X)
        quad(vc, p, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0, light);   // arriba(+Y)
        quad(vc, p, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, light);  // abajo(-Y)
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz, int light) {
        vert(vc, p, ax, ay, az, 0f, 1f, nx, ny, nz, light);
        vert(vc, p, bx, by, bz, 1f, 1f, nx, ny, nz, light);
        vert(vc, p, cx, cy, cz, 1f, 0f, nx, ny, nz, light);
        vert(vc, p, dx, dy, dz, 0f, 0f, nx, ny, nz, light);
    }

    private static void vert(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z,
                             float u, float v, float nx, float ny, float nz, int light) {
        vc.addVertex(p, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(p, nx, ny, nz);
    }
}
