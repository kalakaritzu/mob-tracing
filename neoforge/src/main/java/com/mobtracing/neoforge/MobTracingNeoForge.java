package com.mobtracing.neoforge;

import com.mobtracing.client.config.ModConfig;
import com.mobtracing.client.render.OverlayRenderer;
import com.mobtracing.client.render.PathRenderer;
import com.mobtracing.client.render.ToggleNotification;
import com.mobtracing.client.tracker.EntityTrackerManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

// NeoForge entry point for Mob Tracing (client-side only).
@Mod(value = MobTracingNeoForge.MOD_ID, dist = Dist.CLIENT)
public class MobTracingNeoForge {

    public static final String MOD_ID = "mobtracing";
    public static KeyMapping TOGGLE_KEY;

    public MobTracingNeoForge(IEventBus modEventBus) {
        ModConfig.setConfigDir(FMLPaths.CONFIGDIR.get());
        ModConfig.load();

        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterKeyMappings);

        NeoForge.EVENT_BUS.register(this);
    }

    private void onClientSetup(FMLClientSetupEvent event) {}

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        TOGGLE_KEY = new KeyMapping(
                "key.mobtracing.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                "key.categories.misc"
        );
        event.register(TOGGLE_KEY);
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        while (TOGGLE_KEY != null && TOGGLE_KEY.consumeClick()) {
            ModConfig.get().enabled = !ModConfig.get().enabled;
            ModConfig.save();
            ToggleNotification.show(ModConfig.get().enabled);
        }

        if (client.level != null && client.player != null) {
            EntityTrackerManager.tick(client);
        }
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        OverlayRenderer.renderHud(event.getGuiGraphics(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) return;

        var camera    = event.getCamera();
        var consumers = client.renderBuffers().bufferSource();
        float tickDelta = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        PathRenderer.onRender(
                event.getPoseStack(),
                consumers,
                camera.getPosition(),
                tickDelta
        );
    }

    @SubscribeEvent
    public void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        EntityTrackerManager.clear();
    }
}
