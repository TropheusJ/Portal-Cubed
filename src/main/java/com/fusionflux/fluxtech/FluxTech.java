package com.fusionflux.fluxtech;

//import com.fusionflux.fluxtech.config.FluxTechConfig;

import com.fusionflux.fluxtech.blocks.FluxTechBlocks;
import com.fusionflux.fluxtech.config.FluxTechConfig2;
import com.fusionflux.fluxtech.entity.FluxTechEntities;
import com.fusionflux.fluxtech.items.FluxTechItems;
import com.fusionflux.fluxtech.sound.FluxTechSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class FluxTech implements ModInitializer {

    public static final FluxTechConfig2 CONFIG = new FluxTechConfig2();

    public static final String MOD_ID = "fluxtech";

    public static final ItemGroup FLUXTECH_GROUP = FabricItemGroupBuilder.build(
            new Identifier("fluxtech", "general"),
            () -> new ItemStack(FluxTechBlocks.BOTTOM_2X2_GRITTY_WHITE_PANEL));

    @Override
    public void onInitialize() {
        FluxTechConfig2.register();
        FluxTechBlocks.registerBlocks();
        FluxTechItems.registerItems();
        FluxTechEntities.registerEntities();
        FluxTechSounds.registerSounds();
    }

}
