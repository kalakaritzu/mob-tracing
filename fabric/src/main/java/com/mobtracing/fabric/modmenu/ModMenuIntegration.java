package com.mobtracing.fabric.modmenu;

import com.mobtracing.fabric.config.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Registers Mob Tracing's settings screen with Mod Menu.
 *
 * When the user clicks the config button for Mob Tracing in Mod Menu,
 * this factory opens {@link ConfigScreen} with the current screen as
 * the parent so the Back/Done button returns to Mod Menu.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
