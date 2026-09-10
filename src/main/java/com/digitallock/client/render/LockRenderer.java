package com.digitallock.client.render;

import java.util.List;
import java.util.Map;

import com.digitallock.DigitalLock;
import com.digitallock.client.LockClientCache;
import com.digitallock.data.LockData;
import com.digitallock.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
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
 * <p>El candado <b>es el modelo del ítem</b> (el sprite del autor extruído a 3D
 * por {@code item/generated}) renderizado a mano con {@code putBulkData} sobre
 * el atlas de bloques. Así el candado del mundo es literalmente la imagen del
 * autor con volumen. Un solo estado visual (sin PIN y con PIN se ven igual).
 */
@EventBusSubscriber(modid = DigitalLock.MODID, value = Dist.CLIENT)
public final class LockRenderer {
    private LockRenderer() {}

    // Ajustes de tamaño/posición (fáciles de tunear a ojo con un screenshot):
    private static final float SCALE = 0.22f;   // ancho/alto del candado (fracción de bloque)
    private static final float ZSCALE = 1.0f;   // profundidad (relieve 3D)
    private static final float FACE_Z = 0.47f;  // qué tan afuera del centro se apoya (0.5 = cara del cubo)
    private static final float VERT = -0.13f;   // desplazamiento vertical (negativo = más abajo)

    private static final RandomSource RANDOM = RandomSource.create();
    private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS);

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

        // Modelo del ítem = sprite del autor extruído a 3D.
        BakedModel model = mc.getItemRenderer().getItemModelShaper().getItemModel(ModItems.DIGITAL_PADLOCK.get());
        if (model == null) {
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
            pose.translate(0.5, 0.5 + VERT, 0.5); // centro del bloque, un poco más abajo
            rotateToFace(pose, facing);          // +Z local -> facing
            pose.translate(0.0, 0.0, FACE_Z);    // sacar hacia la cara
            pose.scale(SCALE, SCALE, ZSCALE);    // achicar en X/Y, dar profundidad en Z
            pose.translate(-0.5, -0.5, -0.5);    // centrar el modelo del ítem (autorado en 0..1)

            renderModel(vc, pose, model, light);

            pose.popPose();
        }

        buffers.endBatch(RENDER_TYPE);
    }

    private static void renderModel(VertexConsumer vc, PoseStack pose, BakedModel model, int light) {
        PoseStack.Pose last = pose.last();
        for (Direction dir : Direction.values()) {
            RANDOM.setSeed(42L);
            List<BakedQuad> quads = model.getQuads(null, dir, RANDOM);
            for (BakedQuad quad : quads) {
                vc.putBulkData(last, quad, 1f, 1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY);
            }
        }
        RANDOM.setSeed(42L);
        List<BakedQuad> quads = model.getQuads(null, null, RANDOM);
        for (BakedQuad quad : quads) {
            vc.putBulkData(last, quad, 1f, 1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY);
        }
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

    /** Rota para que el frente del modelo (+Z local) mire hacia {@code facing}. */
    private static void rotateToFace(PoseStack pose, Direction facing) {
        if (facing.getAxis().isHorizontal()) {
            // Misma fórmula de orientación que el ChestRenderer vanilla (modelo por defecto al sur).
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        } else {
            pose.mulPose(Axis.XP.rotationDegrees(facing == Direction.UP ? -90f : 90f));
        }
    }
}
