package com.mobtracing.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

// Low-level geometry helpers for drawing lines and shapes in world space.
// Caller must push/translate the matrix stack to camera-relative coords before calling.
public final class RenderHelper {

    private RenderHelper() {}

    public static void drawLine(VertexConsumer buffer, PoseStack.Pose pose,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float r, float g, float b, float a) {
        if (a <= 0f) return;

        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-4f) return;

        float nx = dx / len, ny = dy / len, nz = dz / len;

        buffer.addVertex(pose.pose(), x1, y1, z1)
              .setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255))
              .setNormal(pose, nx, ny, nz);
        buffer.addVertex(pose.pose(), x2, y2, z2)
              .setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255))
              .setNormal(pose, nx, ny, nz);
    }

    // Small 3-axis cross, used to mark path nodes
    public static void drawCross(VertexConsumer buffer, PoseStack.Pose pose,
                                  float x, float y, float z, float size,
                                  float r, float g, float b, float a) {
        drawLine(buffer, pose, x - size, y, z, x + size, y, z, r, g, b, a);
        drawLine(buffer, pose, x, y - size, z, x, y + size, z, r, g, b, a);
        drawLine(buffer, pose, x, y, z - size, x, y, z + size, r, g, b, a);
    }

    // Wireframe AABB, used to mark path destinations
    public static void drawBox(VertexConsumer buffer, PoseStack.Pose pose,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float r, float g, float b, float a) {
        // Bottom face
        drawLine(buffer, pose, x1, y1, z1, x2, y1, z1, r, g, b, a);
        drawLine(buffer, pose, x2, y1, z1, x2, y1, z2, r, g, b, a);
        drawLine(buffer, pose, x2, y1, z2, x1, y1, z2, r, g, b, a);
        drawLine(buffer, pose, x1, y1, z2, x1, y1, z1, r, g, b, a);
        // Top face
        drawLine(buffer, pose, x1, y2, z1, x2, y2, z1, r, g, b, a);
        drawLine(buffer, pose, x2, y2, z1, x2, y2, z2, r, g, b, a);
        drawLine(buffer, pose, x2, y2, z2, x1, y2, z2, r, g, b, a);
        drawLine(buffer, pose, x1, y2, z2, x1, y2, z1, r, g, b, a);
        // Vertical edges
        drawLine(buffer, pose, x1, y1, z1, x1, y2, z1, r, g, b, a);
        drawLine(buffer, pose, x2, y1, z1, x2, y2, z1, r, g, b, a);
        drawLine(buffer, pose, x2, y1, z2, x2, y2, z2, r, g, b, a);
        drawLine(buffer, pose, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    public static void drawHorizontalCircle(VertexConsumer buffer, PoseStack.Pose pose,
                                             float cx, float cy, float cz, float radius,
                                             int segments,
                                             float r, float g, float b, float a) {
        float step = (float) (2.0 * Math.PI / segments);
        float prevX = cx + radius;
        float prevZ = cz;

        for (int i = 1; i <= segments; i++) {
            float angle = i * step;
            float nextX = cx + (float) Math.cos(angle) * radius;
            float nextZ = cz + (float) Math.sin(angle) * radius;
            drawLine(buffer, pose, prevX, cy, prevZ, nextX, cy, nextZ, r, g, b, a);
            prevX = nextX;
            prevZ = nextZ;
        }
    }

    // Vertical struts around a circle — pair with two drawHorizontalCircle calls to make a cylinder
    public static void drawCircleWall(VertexConsumer buffer, PoseStack.Pose pose,
                                       float cx, float cy, float cz, float radius,
                                       int segments, float wallHeight,
                                       float r, float g, float b, float a) {
        float step = (float) (2.0 * Math.PI / segments);
        for (int i = 0; i < segments; i++) {
            float angle = i * step;
            float wx = cx + (float) Math.cos(angle) * radius;
            float wz = cz + (float) Math.sin(angle) * radius;
            drawLine(buffer, pose, wx, cy, wz, wx, cy + wallHeight, wz, r, g, b, a);
        }
    }

    public static float r(int color) { return ((color >> 16) & 0xFF) / 255f; }
    public static float g(int color) { return ((color >>  8) & 0xFF) / 255f; }
    public static float b(int color) { return (color & 0xFF) / 255f; }
}
