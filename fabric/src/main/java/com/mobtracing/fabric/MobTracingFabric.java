package com.mobtracing.fabric;

import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric client entry point for Mob Tracing.
 * Delegates all initialization to {@link MobTracingClientFabric}.
 */
public class MobTracingFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MobTracingClientFabric.init();
    }
}
