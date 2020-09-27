package com.fuzs.menucompanions.client.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Queue;

public class DrawEntityUtil {

    @SuppressWarnings("deprecation")
    public static void drawEntityOnScreen(int posX, int posY, int scale, float mouseX, float mouseY, Entity entity, float partialTicks) {

        float f = (float)Math.atan(mouseX / 40.0F);
        float f1 = (float)Math.atan(mouseY / 40.0F);
        RenderSystem.pushMatrix();
        RenderSystem.translatef((float)posX, (float)posY, 0.0F);
        RenderSystem.scalef(1.0F, 1.0F, -1.0F);
        MatrixStack matrixstack = new MatrixStack();
        matrixstack.translate(0.0D, 0.0D, 0.0D);
        matrixstack.scale((float)scale, (float)scale, (float)scale);
        Quaternion quaternion = Vector3f.ZP.rotationDegrees(180.0F);
        Quaternion quaternion1 = Vector3f.XP.rotationDegrees(f1 * 20.0F);
        quaternion.multiply(quaternion1);
        matrixstack.rotate(quaternion);
        entity.prevRotationYaw = entity.rotationYaw;
        entity.prevRotationPitch = entity.rotationPitch;
        entity.rotationYaw = 180.0F + f * 40.0F;
        entity.rotationPitch = -f1 * 20.0F;
        if (entity instanceof LivingEntity) {

            LivingEntity livingEntity = (LivingEntity) entity;
            livingEntity.prevRenderYawOffset = livingEntity.renderYawOffset;
            livingEntity.prevRotationYawHead = livingEntity.rotationYawHead;
            livingEntity.renderYawOffset = 180.0F + f * 20.0F;
            livingEntity.rotationYawHead = entity.rotationYaw;
        }

        EntityRendererManager entityrenderermanager = Minecraft.getInstance().getRenderManager();
        quaternion1.conjugate();
        entityrenderermanager.setCameraOrientation(quaternion1);
        entityrenderermanager.setRenderShadow(false);
        IRenderTypeBuffer.Impl irendertypebuffer$impl = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        RenderSystem.runAsFancy(() -> entityrenderermanager.renderEntityStatic(entity, 0.0D, 0.0D, 0.0D, 0.0F, partialTicks, matrixstack, irendertypebuffer$impl, 15728880));

        irendertypebuffer$impl.finish();
        entityrenderermanager.setRenderShadow(true);
        RenderSystem.popMatrix();
    }

    public static void renderParticles(ParticleManager manager, MatrixStack matrixStackIn, LightTexture lightTextureIn, ActiveRenderInfo activeRenderInfoIn, TextureManager renderer, float partialTicks) {
        lightTextureIn.enableLightmap();
        Runnable enable = () -> {
            RenderSystem.enableAlphaTest();
            RenderSystem.defaultAlphaFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.enableFog();
            RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE2);
            RenderSystem.enableTexture();
            RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        };
        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(matrixStackIn.getLast().getMatrix());

        final Map<IParticleRenderType, Queue<Particle>> byType = ObfuscationReflectionHelper.getPrivateValue(ParticleManager.class, manager, "byType");
        for(IParticleRenderType iparticlerendertype : byType.keySet()) { // Forge: allow custom IParticleRenderType's
            if (iparticlerendertype == IParticleRenderType.NO_RENDER) continue;
            enable.run(); //Forge: MC-168672 Make sure all render types have the correct GL state.
            Iterable<Particle> iterable = byType.get(iparticlerendertype);
            if (iterable != null) {
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder bufferbuilder = tessellator.getBuffer();
                iparticlerendertype.beginRender(bufferbuilder, renderer);

                for(Particle particle : iterable) {
                    try {
                        particle.renderParticle(bufferbuilder, activeRenderInfoIn, partialTicks);
                    } catch (Throwable throwable) {
                        CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Rendering Particle");
                        CrashReportCategory crashreportcategory = crashreport.makeCategory("Particle being rendered");
                        crashreportcategory.addDetail("Particle", particle::toString);
                        crashreportcategory.addDetail("Particle Type", iparticlerendertype::toString);
                        throw new ReportedException(crashreport);
                    }
                }

                iparticlerendertype.finishRender(tessellator);
            }
        }

        RenderSystem.popMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(515);
        RenderSystem.disableBlend();
        RenderSystem.defaultAlphaFunc();
        lightTextureIn.disableLightmap();
        RenderSystem.disableFog();
    }

}
