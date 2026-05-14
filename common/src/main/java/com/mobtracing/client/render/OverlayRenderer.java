package com.mobtracing.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

// Platform-neutral HUD rendering — called directly by each platform's event hook.
public final class OverlayRenderer {

    private OverlayRenderer() {}

    public static void renderHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        if (client.options.hideGui) return;

        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();

        ToggleNotification.render(guiGraphics, w, h);
    }

    // Overload for platforms that provide a float partial tick instead of DeltaTracker
    public static void renderHud(GuiGraphics guiGraphics, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        if (client.options.hideGui) return;

        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();

        ToggleNotification.render(guiGraphics, w, h);
    }
}
