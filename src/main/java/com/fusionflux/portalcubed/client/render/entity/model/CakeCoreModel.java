// Made with Blockbench 4.0.4
// Exported for Minecraft version 1.17
// Paste this class into your mod and generate all required imports

package com.fusionflux.portalcubed.client.render.entity.model;

import com.fusionflux.portalcubed.entity.CakeCoreEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import static com.fusionflux.portalcubed.PortalCubed.id;

public class CakeCoreModel extends FizzleableModel<CakeCoreEntity> {
    public static final ModelLayerLocation CAKE_CORE_LAYER = new ModelLayerLocation(id("cake_core"), "main");
    @SuppressWarnings("checkstyle:MemberName")
    private final ModelPart bb_main;

    public CakeCoreModel(ModelPart root) {
        this.bb_main = root.getChild("bb_main");
    }

    @SuppressWarnings("checkstyle:LocalVariableName")
    public static LayerDefinition getTexturedModelData() {
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition modelPartData = modelData.getRoot();
        PartDefinition bb_main = modelPartData.addOrReplaceChild("bb_main", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        bb_main.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(19, 26).addBox(-2.4F, -3.0F, 3.0F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, 5.0F, 3.5F, 0.0F, -0.7854F, 1.5708F));

        bb_main.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(19, 26).addBox(-2.5F, -3.0F, 3.25F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -3.0F, 3.5F, 0.0F, -0.7854F, 1.5708F));

        bb_main.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(24, 9).addBox(-1.5F, -1.5F, 0.35F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0F, 3.175F, 0.0F, 3.1416F, 0.0F));

        bb_main.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(0, 21).addBox(0.0F, -3.5F, -1.5F, 0.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.182F, 3.8536F, -3.1416F, -0.7854F, -1.5708F));

        bb_main.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 21).addBox(0.0F, -3.5F, -1.5F, 0.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.182F, 3.8536F, -3.1416F, 0.7854F, -1.5708F));

        bb_main.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(0, 12).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.2F))
            .texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, 3.1416F, 0.0F, 0.0F));
        return LayerDefinition.create(modelData, 32, 32);
    }

    @Override
    public void renderFizzled(PoseStack matrixStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

        bb_main.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

}
