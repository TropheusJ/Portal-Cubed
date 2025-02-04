package com.fusionflux.portalcubed.compat;

import com.fusionflux.portalcubed.PortalCubedConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

@SuppressWarnings("unused")
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> PortalCubedConfig.getScreen(parent, "portalcubed");
    }
}
