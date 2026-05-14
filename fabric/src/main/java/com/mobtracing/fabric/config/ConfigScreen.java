package com.mobtracing.fabric.config;

import com.mobtracing.client.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

// Settings screen opened via the Mod Menu gear icon.
public class ConfigScreen extends Screen {

    // All buttons are 200 px wide, 20 px tall, left-aligned to screen centre − 100
    private static final int W  = 200;
    private static final int H  = 20;
    private static final int RS = 21;   // normal row stride
    private static final int SG = 9;    // extra gap before a section header

    private int featuresSectionY;
    private int displaySectionY;

    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.literal("Mob Tracing Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ModConfig cfg = ModConfig.get();
        int cx = this.width  / 2;
        int x  = cx - W / 2;

        int y = 40;
        addOnOff(x, y, "AI Vision Overlay", cfg.enabled,
                (btn, v) -> { cfg.enabled = v; ModConfig.save(); });

        y += H + SG;
        featuresSectionY = y;
        y += 13;

        addOnOff(x, y, "Mob Paths", cfg.showPaths,
                (btn, v) -> { cfg.showPaths = v; ModConfig.save(); });
        y += RS;
        addOnOff(x, y, "Target Lines", cfg.showTargetLines,
                (btn, v) -> { cfg.showTargetLines = v; ModConfig.save(); });
        y += RS;
        addOnOff(x, y, "Look Direction", cfg.showLookDirection,
                (btn, v) -> { cfg.showLookDirection = v; ModConfig.save(); });
        y += RS;
        addOnOff(x, y, "Aggro Radius", cfg.showAggroRadius,
                (btn, v) -> { cfg.showAggroRadius = v; ModConfig.save(); });

        y += H + SG;
        displaySectionY = y;
        y += 13;

        // Max render distance [8–128 blocks]
        final float MIN_D = 8f, MAX_D = 128f;
        double initDist = (cfg.maxRenderDistance - MIN_D) / (MAX_D - MIN_D);
        final int finalY1 = y;
        addRenderableWidget(new AbstractSliderButton(x, finalY1, W, H,
                distLabel((int) cfg.maxRenderDistance), initDist) {
            @Override protected void updateMessage() {
                setMessage(distLabel((int)(MIN_D + value * (MAX_D - MIN_D))));
            }
            @Override protected void applyValue() {
                ModConfig.get().maxRenderDistance = (float)(MIN_D + value * (MAX_D - MIN_D));
                ModConfig.save();
            }
        });
        y += RS;

        // Label opacity [0–100 %]
        final int finalY2 = y;
        addRenderableWidget(new AbstractSliderButton(x, finalY2, W, H,
                opacityLabel((int)(cfg.labelOpacity * 100)), cfg.labelOpacity) {
            @Override protected void updateMessage() {
                setMessage(opacityLabel((int)(value * 100)));
            }
            @Override protected void applyValue() {
                ModConfig.get().labelOpacity = (float) value;
                ModConfig.save();
            }
        });
        y += RS;

        // Update interval [1–10 ticks]
        double initTick = (cfg.updateIntervalTicks - 1) / 9.0;
        final int finalY3 = y;
        addRenderableWidget(new AbstractSliderButton(x, finalY3, W, H,
                intervalLabel(cfg.updateIntervalTicks), initTick) {
            @Override protected void updateMessage() {
                int t = 1 + (int) Math.round(value * 9);
                setMessage(intervalLabel(t));
            }
            @Override protected void applyValue() {
                ModConfig.get().updateIntervalTicks = 1 + (int) Math.round(value * 9);
                ModConfig.save();
            }
        });

        y += H + SG + 4;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> onClose())
                .bounds(cx - 75, y, 150, H)
                .build());
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        int cx = this.width / 2;

        ctx.drawCenteredString(font,
                Component.literal("Mob Tracing"), cx, 10, 0xFFFFFF);
        ctx.drawCenteredString(font,
                Component.literal("Press F7 to toggle · Mod Menu to configure"), cx, 22, 0x888888);

        drawSectionHeader(ctx, cx, featuresSectionY, "FEATURES");
        drawSectionHeader(ctx, cx, displaySectionY,  "DISPLAY");
    }

    private void drawSectionHeader(GuiGraphics ctx, int cx, int y, String label) {
        int lineY      = y + 4;
        int lineColour = 0xFF3A3A3A;
        int textColour = 0xFF777777;
        int halfLabel  = font.width(label) / 2 + 5;

        ctx.hLine(cx - W / 2,      cx - halfLabel, lineY, lineColour);
        ctx.hLine(cx + halfLabel,  cx + W / 2,     lineY, lineColour);
        ctx.drawCenteredString(font, Component.literal(label), cx, y, textColour);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void addOnOff(int x, int y, String label, boolean initial,
                          java.util.function.BiConsumer<Button, Boolean> cb) {
        final boolean[] state = { initial };
        addRenderableWidget(Button.builder(
                Component.literal(label + ": " + (initial ? "ON" : "OFF")),
                btn -> {
                    state[0] = !state[0];
                    btn.setMessage(Component.literal(label + ": " + (state[0] ? "ON" : "OFF")));
                    cb.accept(btn, state[0]);
                })
                .bounds(x, y, W, H)
                .build());
    }

    private static Component distLabel(int blocks) {
        return Component.literal("Max Distance: " + blocks + " blocks");
    }

    private static Component opacityLabel(int pct) {
        return Component.literal("Label Opacity: " + pct + "%");
    }

    private static Component intervalLabel(int ticks) {
        return Component.literal("Update Interval: " + ticks + (ticks == 1 ? " tick" : " ticks"));
    }
}
