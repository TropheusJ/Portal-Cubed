package com.fusionflux.portalcubed.accessor;

import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface BlockCollisionsExt {
    BlockCollisions setPortalCutout(VoxelShape cutout);
}
