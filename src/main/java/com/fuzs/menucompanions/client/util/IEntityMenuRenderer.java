package com.fuzs.menucompanions.client.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;

public interface IEntityMenuRenderer {

    static void renderName(Entity entityIn, ITextComponent displayNameIn, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, EntityRendererManager renderManager) {

        float renderHeight = entityIn.getHeight() + 0.5F;
        float renderOffset = "deadmau5".equals(displayNameIn.getString()) ? -10 : 0;

        matrixStackIn.push();
        matrixStackIn.translate(0.0D, renderHeight, 0.0D);
        matrixStackIn.rotate(renderManager.getCameraOrientation());
        matrixStackIn.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix4f = matrixStackIn.getLast().getMatrix();

        float backgroundOpacity = Minecraft.getInstance().gameSettings.getTextBackgroundOpacity(0.25F);
        int alpha = (int)(backgroundOpacity * 255.0F) << 24;
        FontRenderer fontrenderer = renderManager.getFontRenderer();
        int textWidth = -fontrenderer.func_238414_a_(displayNameIn) / 2;
        fontrenderer.func_243247_a(displayNameIn, textWidth, renderOffset, 553648127, false, matrix4f, bufferIn, true, alpha, packedLightIn);
        fontrenderer.func_243247_a(displayNameIn, textWidth, renderOffset, -1, false, matrix4f, bufferIn, false, 0, packedLightIn);

        matrixStackIn.pop();
    }

}
