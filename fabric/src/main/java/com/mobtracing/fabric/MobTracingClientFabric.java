package com.mobtracing.fabric;

import com.mobtracing.client.config.ModConfig;
import com.mobtracing.client.render.OverlayRenderer;
import com.mobtracing.client.render.PathRenderer;
import com.mobtracing.client.render.ToggleNotification;
import com.mobtracing.client.tracker.EntityTrackerManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

// Fabric-specific client init — wires Fabric API events to the platform-neutral common code.
public class MobTracingClientFabric {

    public static KeyMapping TOGGLE_KEY;

    public static void init() {
        ModConfig.setConfigDir(FabricLoader.getInstance().getConfigDir());
        ModConfig.load();

        TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mobtracing.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                "key.categories.misc"
        ));

        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) ->
                OverlayRenderer.renderHud(guiGraphics, tickCounter));

        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            if (context.consumers() == null) return;
            PathRenderer.onRender(
                    context.matrixStack(),
                    context.consumers(),
                    context.camera().getPosition(),
                    net.minecraft.client.Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)
            );
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_KEY.consumeClick()) {
                ModConfig.get().enabled = !ModConfig.get().enabled;
                ModConfig.save();
                ToggleNotification.show(ModConfig.get().enabled);
            }
            if (client.level != null && client.player != null) {
                EntityTrackerManager.tick(client);
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                EntityTrackerManager.clear());

        ClientLifecycleEvents.CLIENT_STOPPING.register(client ->
                EntityTrackerManager.clear());
    }
}
