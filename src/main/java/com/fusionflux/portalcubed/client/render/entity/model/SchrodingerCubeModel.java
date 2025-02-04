// Made with Blockbench 4.0.4
// Exported for Minecraft version 1.17
// Paste this class into your mod and generate all required imports

package com.fusionflux.portalcubed.client.render.entity.model;

import com.fusionflux.portalcubed.entity.SchrodingerCubeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;

import static com.fusionflux.portalcubed.PortalCubed.id;

public class SchrodingerCubeModel extends FizzleableModel<SchrodingerCubeEntity> {
    public static final ModelLayerLocation SCHRODINGER_CUBE_MAIN_LAYER = new ModelLayerLocation(id("schrodinger_cube"), "main");
    @SuppressWarnings("checkstyle:MemberName")
    private final ModelPart bb_main;

    public SchrodingerCubeModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.bb_main = root.getChild("bb_main");
    }


    @SuppressWarnings("checkstyle:LocalVariableName")
    public static LayerDefinition getTexturedModelData() {
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition modelPartData = modelData.getRoot();
        PartDefinition bb_main = modelPartData.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 0).addBox(-4.99F, -9.99F, -5.01F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(0, 20).addBox(-4.95F, -10.0F, -5.0F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.4F))
                .texOffs(0, 40).addBox(-3.0F, -8.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        bb_main.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(47, 0).addBox(0.0F, -2.5F, -2.5F, 0.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.5F, 0.0F, 0.7854F, 0.0F, -1.5708F));

        bb_main.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(47, 0).addBox(0.0F, -2.5F, -2.5F, 0.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.5F, 0.0F, 0.0F, -0.7854F, 0.0F));

        bb_main.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(47, 0).addBox(0.0F, -2.5F, -2.5F, 0.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.5F, 0.0F, 0.0F, 0.8727F, 0.0F));
        return LayerDefinition.create(modelData, 64, 64);
    }

    @Override
    public void renderFizzled(PoseStack matrixStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

        bb_main.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

}
