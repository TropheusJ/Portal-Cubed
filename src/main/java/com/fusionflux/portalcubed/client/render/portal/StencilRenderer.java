package com.fusionflux.portalcubed.client.render.portal;

import com.fusionflux.portalcubed.accessor.RenderTargetExt;
import com.fusionflux.portalcubed.entity.Portal;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;

import static org.lwjgl.opengl.GL11.*;

public class StencilRenderer extends PortalRendererImpl {
    private int portalLayer = 0;

    @Override
    public boolean enabled(Portal portal) {
        return portalLayer < MAX_PORTAL_LAYER && portal.getActive();
    }

    @Override
    public void preRender(Portal portal, float tickDelta, PoseStack poseStack, MultiBufferSource bufferSource) {
        ((MultiBufferSource.BufferSource)bufferSource).endBatch();
        // TODO: PortingLib compat
        ((RenderTargetExt)Minecraft.getInstance().getMainRenderTarget()).setStencilBufferEnabled(true);
        glEnable(GL_STENCIL_TEST);
        RenderSystem.clear(GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);
        RenderSystem.stencilMask(0xff);
        RenderSystem.stencilFunc(GL_NEVER, 0, 0xff);
        RenderSystem.stencilOp(GL_INCR, GL_KEEP, GL_KEEP);
    }

    @Override
    public void postRender(Portal portal, float tickDelta, PoseStack poseStack, MultiBufferSource bufferSource) {
        ((MultiBufferSource.BufferSource)bufferSource).endBatch();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.stencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        RenderSystem.stencilFunc(GL_NEVER, 123, 0xff);

        portalLayer++;
        renderWorld(portal, tickDelta);
        portalLayer--;

        glDisable(GL_STENCIL_TEST);
    }

    @Override
    public PortalRenderPhase targetPhase() {
        return PortalRenderPhase.FINAL;
    }
}
