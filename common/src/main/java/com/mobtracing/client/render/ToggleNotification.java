package com.mobtracing.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

// Minimal action-bar-style toast above the hotbar when F7 is pressed.
// No box — just text with shadow, fading in and out.
public final class ToggleNotification {

    private ToggleNotification() {}

    private static final long FADE_IN_MS  = 120L;
    private static final long HOLD_MS     = 1100L;
    private static final long FADE_OUT_MS = 500L;
    private static final long TOTAL_MS    = FADE_IN_MS + HOLD_MS + FADE_OUT_MS;

    private static long    shownAt = -1L;
    private static boolean enabled = false;

    private static final int COLOR_ON  = 0x55FFCC;   // teal
    private static final int COLOR_OFF = 0xFF7755;   // warm orange-red

    public static void show(boolean nowEnabled) {
        shownAt = System.currentTimeMillis();
        enabled = nowEnabled;
    }

    public static void render(GuiGraphics guiGraphics, int screenW, int screenH) {
        if (shownAt < 0) return;

        long elapsed = System.currentTimeMillis() - shownAt;
        if (elapsed >= TOTAL_MS) { shownAt = -1; return; }

        float alpha = computeAlpha(elapsed);
        if (alpha <= 0f) return;

        Font font = Minecraft.getInstance().font;

        String text  = enabled ? "Entity Vision  ON" : "Entity Vision  OFF";
        int    color = enabled ? COLOR_ON : COLOR_OFF;
        int    a     = (int)(alpha * 255);

        // Sit just above the hotbar
        int ty = screenH - 59;

        int argb = (a << 24) | color;
        guiGraphics.drawCenteredString(font, text, screenW / 2, ty, argb);
    }

    private static float computeAlpha(long elapsed) {
        if (elapsed < FADE_IN_MS) {
            return (float) elapsed / FADE_IN_MS;
        } else if (elapsed < FADE_IN_MS + HOLD_MS) {
            return 1.0f;
        } else {
            return 1.0f - (float)(elapsed - FADE_IN_MS - HOLD_MS) / FADE_OUT_MS;
        }
    }
}
