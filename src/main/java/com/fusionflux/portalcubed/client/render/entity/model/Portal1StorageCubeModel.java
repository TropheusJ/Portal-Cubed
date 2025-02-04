// Made with Blockbench 4.0.4
// Exported for Minecraft version 1.17
// Paste this class into your mod and generate all required imports

package com.fusionflux.portalcubed.client.render.entity.model;

import com.fusionflux.portalcubed.entity.Portal1StorageCubeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import static com.fusionflux.portalcubed.PortalCubed.id;

public class Portal1StorageCubeModel extends FizzleableModel<Portal1StorageCubeEntity> {
    public static final ModelLayerLocation COMPANION_CUBE_MAIN_LAYER = new ModelLayerLocation(id("portal_1_storage_cube"), "main");
    @SuppressWarnings("checkstyle:MemberName")
    private final ModelPart bb_main;

    public Portal1StorageCubeModel(ModelPart root) {
        this.bb_main = root.getChild("bone");
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition modelPartData = modelData.getRoot();
        modelPartData.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 0).addBox(-5.5F, -10.0F, -5.0F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.0F))
            .texOffs(0, 20).addBox(-5.5F, -10.0F, -5.0F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.5F)), PartPose.offset(0.5F, 24.0F, 0.0F));
        return LayerDefinition.create(modelData, 64, 64);
    }

    @Override
    public void renderFizzled(PoseStack matrixStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

        bb_main.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

}
