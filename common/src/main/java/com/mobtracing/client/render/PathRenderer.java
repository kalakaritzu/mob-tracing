package com.mobtracing.client.render;

import com.mobtracing.client.config.ModConfig;
import com.mobtracing.client.data.EntityAIData;
import com.mobtracing.client.tracker.EntityTrackerManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Draws all 3-D path, target, and look-direction geometry into the world.
// Platform-neutral — both Fabric and NeoForge call onRender from their own event hooks.
public final class PathRenderer {

    private PathRenderer() {}

    public static void onRender(PoseStack poseStack, MultiBufferSource bufferSource,
                                 Vec3 camPos, float tickDelta) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.enabled) return;
        if (!cfg.showPaths && !cfg.showTargetLines
                && !cfg.showLookDirection && !cfg.showAggroRadius) return;

        if (bufferSource == null) return;

        Minecraft client = Minecraft.getInstance();
        VertexConsumer lines = bufferSource.getBuffer(RenderType.LINES);

        // Translate once so all world-space coordinates become camera-relative
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
        PoseStack.Pose pose = poseStack.last();

        for (EntityAIData data : EntityTrackerManager.getTrackedData()) {
            try {
                renderEntity(lines, pose, data, cfg, client, tickDelta);
            } catch (Exception ignored) {
                // Protect the render loop against individual entity failures
            }
        }

        poseStack.popPose();
    }

    private static void renderEntity(VertexConsumer buffer, PoseStack.Pose pose,
                                      EntityAIData data, ModConfig cfg,
                                      Minecraft client, float tickDelta) {
        int   baseColor = data.getCategoryColor();
        float r = RenderHelper.r(baseColor);
        float g = RenderHelper.g(baseColor);
        float b = RenderHelper.b(baseColor);

        Vec3 entityPos = interpolatedPos(data, client, tickDelta);
        double entityHeight = data.entityHeight();

        if (cfg.showPaths && data.isNavigating()) {
            renderPath(buffer, pose, data, r, g, b);
        }

        if (cfg.showTargetLines && data.targetPos() != null) {
            Vec3 src = entityPos.add(0, entityHeight * 0.5, 0);
            Vec3 dst = data.targetPos();
            RenderHelper.drawLine(buffer, pose,
                    (float) src.x, (float) src.y, (float) src.z,
                    (float) dst.x, (float) dst.y, (float) dst.z,
                    0.95f, 0.20f, 0.20f, 0.55f);

            // Small cross at target
            RenderHelper.drawCross(buffer, pose,
                    (float) dst.x, (float) dst.y, (float) dst.z,
                    0.18f, 0.95f, 0.20f, 0.20f, 0.70f);
        }

        if (cfg.showLookDirection) {
            Vec3 eye     = entityPos.add(0, entityHeight * 0.88, 0);
            Vec3 lookEnd = eye.add(data.lookDirection().scale(5.5));
            RenderHelper.drawLine(buffer, pose,
                    (float) eye.x,     (float) eye.y,     (float) eye.z,
                    (float) lookEnd.x, (float) lookEnd.y, (float) lookEnd.z,
                    0.95f, 0.90f, 0.25f, 0.28f);
        }

        if (cfg.showAggroRadius && data.followRange() > 0f) {
            boolean isHostile = data.category() == EntityAIData.EntityCategory.HOSTILE;
            boolean isAggro   = data.targetPos() != null;
            if (isHostile || isAggro) {
                drawRadiusIndicator(buffer, pose,
                        (float) entityPos.x, (float) entityPos.y + 0.05f, (float) entityPos.z,
                        data.followRange(),
                        Math.min(r * 0.5f + 0.5f, 1f),
                        Math.min(g * 0.5f + 0.5f, 1f),
                        Math.min(b * 0.5f + 0.5f, 1f));
            }
        }

        if (cfg.showAggroRadius && data.isBeingTempted()) {
            drawRadiusIndicator(buffer, pose,
                    (float) entityPos.x, (float) entityPos.y + 0.05f, (float) entityPos.z,
                    10.0f,
                    1.00f, 0.82f, 0.18f);  // warm gold
        }
    }

    private static void drawRadiusIndicator(VertexConsumer buffer, PoseStack.Pose pose,
                                             float cx, float cy, float cz, float range,
                                             float r, float g, float b) {
        RenderHelper.drawHorizontalCircle(buffer, pose, cx, cy, cz,
                range, 64, r, g, b, 0.90f);
        RenderHelper.drawHorizontalCircle(buffer, pose, cx, cy + 1.0f, cz,
                range, 64, r, g, b, 0.30f);
        RenderHelper.drawCircleWall(buffer, pose, cx, cy, cz,
                range, 64, 1.0f, r, g, b, 0.30f);
    }

    private static void renderPath(VertexConsumer buffer, PoseStack.Pose pose,
                                    EntityAIData data,
                                    float r, float g, float b) {
        List<Vec3> nodes   = data.pathNodes();
        int        current = data.currentNodeIndex();

        float pr = Math.min(r * 0.5f + 0.5f, 1f);
        float pg = Math.min(g * 0.5f + 0.5f, 1f);
        float pb = Math.min(b * 0.5f + 0.5f, 1f);

        for (int i = 0; i < nodes.size() - 1; i++) {
            Vec3 from = nodes.get(i);
            Vec3 to   = nodes.get(i + 1);

            boolean ahead = i >= current;
            float alpha = ahead ? 0.92f : 0.08f;

            RenderHelper.drawLine(buffer, pose,
                    (float) from.x, (float) from.y + 0.06f, (float) from.z,
                    (float) to.x,   (float) to.y   + 0.06f, (float) to.z,
                    ahead ? pr : r, ahead ? pg : g, ahead ? pb : b, alpha);
        }

        // Highlight the current target node
        if (current >= 0 && current < nodes.size() - 1) {
            Vec3 node = nodes.get(current);
            float nr = Math.min(r * 1.6f, 1f);
            float ng = Math.min(g * 1.6f, 1f);
            float nb = Math.min(b * 1.6f, 1f);
            float s = 0.40f;
            float nx2 = (float) node.x, ny2 = (float) node.y + 0.06f, nz2 = (float) node.z;
            RenderHelper.drawLine(buffer, pose, nx2-s, ny2, nz2-s, nx2+s, ny2, nz2-s, nr, ng, nb, 1.0f);
            RenderHelper.drawLine(buffer, pose, nx2+s, ny2, nz2-s, nx2+s, ny2, nz2+s, nr, ng, nb, 1.0f);
            RenderHelper.drawLine(buffer, pose, nx2+s, ny2, nz2+s, nx2-s, ny2, nz2+s, nr, ng, nb, 1.0f);
            RenderHelper.drawLine(buffer, pose, nx2-s, ny2, nz2+s, nx2-s, ny2, nz2-s, nr, ng, nb, 1.0f);
        }

        // Destination marker
        if (!nodes.isEmpty()) {
            Vec3 dest = nodes.get(nodes.size() - 1);
            float dx = 0.28f;

            float dr = Math.min(r * 1.4f + 0.25f, 1f);
            float dg = Math.min(g * 1.4f + 0.25f, 1f);
            float db = Math.min(b * 1.4f + 0.25f, 1f);

            RenderHelper.drawBox(buffer, pose,
                    (float) dest.x - dx, (float) dest.y,        (float) dest.z - dx,
                    (float) dest.x + dx, (float) dest.y + 0.6f, (float) dest.z + dx,
                    dr, dg, db, 1.0f);
        }
    }

    // Falls back to the snapshot position if the entity is no longer loaded
    private static Vec3 interpolatedPos(EntityAIData data, Minecraft client, float tickDelta) {
        if (client.level == null) return data.entityPos();
        Entity liveEntity = client.level.getEntity(data.entityId());
        if (liveEntity == null) return data.entityPos();
        return new Vec3(
                liveEntity.xo + (liveEntity.getX() - liveEntity.xo) * tickDelta,
                liveEntity.yo + (liveEntity.getY() - liveEntity.yo) * tickDelta,
                liveEntity.zo + (liveEntity.getZ() - liveEntity.zo) * tickDelta
        );
    }
}
