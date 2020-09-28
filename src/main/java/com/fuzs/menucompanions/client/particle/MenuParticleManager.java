package com.fuzs.menucompanions.client.particle;

import com.fuzs.menucompanions.mixin.ParticleManagerAccessorMixin;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.particles.IParticleData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;

@OnlyIn(Dist.CLIENT)
public class MenuParticleManager {

    private final ClientWorld world;
    private final TextureManager renderer;
    private final ParticleManagerAccessorMixin particles;
    private final Map<IParticleRenderType, Queue<Particle>> byType = Maps.newIdentityHashMap();
    private final Queue<Particle> queue = Queues.newArrayDeque();

    public MenuParticleManager(Minecraft mc, ClientWorld world) {

        this.world = world;
        this.renderer = mc.textureManager;
        this.particles = (ParticleManagerAccessorMixin) mc.particles;
    }

    public void addParticle(IParticleData particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {

        Particle particle = this.makeParticle(particleData, x, y, z, xSpeed, ySpeed, zSpeed);
        if (particle != null) {

            this.addEffect(particle);
        }
    }

    @Nullable
    private <T extends IParticleData> Particle makeParticle(T particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {

        IParticleFactory<T> iparticlefactory = this.getParticleFactory(particleData);
        return iparticlefactory == null ? null : iparticlefactory.makeParticle(particleData, this.world, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    @SuppressWarnings("unchecked")
    private <T extends IParticleData> IParticleFactory<T> getParticleFactory(T particleData) {

        return (IParticleFactory<T>) this.particles.getFactories().get(ForgeRegistries.PARTICLE_TYPES.getKey(particleData.getType()));
    }

    public void addEffect(Particle effect) {

        this.queue.add(effect);
    }

    @SuppressWarnings("UnstableApiUsage")
    public void tick() {

        this.byType.forEach((p_228347_1_, p_228347_2_) -> {

            this.world.getProfiler().startSection(p_228347_1_.toString());
            this.tickParticleList(p_228347_2_);
            this.world.getProfiler().endSection();
        });

        Particle particle;
        if (!this.queue.isEmpty()) {

            while ((particle = this.queue.poll()) != null) {

                this.byType.computeIfAbsent(particle.getRenderType(), (p_228346_0_) -> EvictingQueue.create(16384)).add(particle);
            }
        }

    }

    private void tickParticleList(Collection<Particle> particlesIn) {

        if (!particlesIn.isEmpty()) {

            Iterator<Particle> iterator = particlesIn.iterator();
            while (iterator.hasNext()) {

                Particle particle = iterator.next();
                this.tickParticle(particle);
                if (!particle.isAlive()) {

                    iterator.remove();
                }
            }
        }
    }

    private void tickParticle(Particle particle) {

        try {

            particle.tick();
        } catch (Throwable throwable) {

            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Ticking Particle");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Particle being ticked");
            crashreportcategory.addDetail("Particle", particle::toString);
            crashreportcategory.addDetail("Particle Type", particle.getRenderType()::toString);
            throw new ReportedException(crashreport);
        }
    }

    @SuppressWarnings("deprecation")
    public void renderParticles(MatrixStack matrixStackIn, LightTexture lightTextureIn, ActiveRenderInfo activeRenderInfoIn, float partialTicks) {

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
        for (IParticleRenderType iparticlerendertype : this.byType.keySet()) { // Forge: allow custom IParticleRenderType's

            if (iparticlerendertype == IParticleRenderType.NO_RENDER) continue;
            enable.run(); //Forge: MC-168672 Make sure all render types have the correct GL state.
            Iterable<Particle> iterable = this.byType.get(iparticlerendertype);
            if (iterable != null) {

                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder bufferbuilder = tessellator.getBuffer();
                iparticlerendertype.beginRender(bufferbuilder, this.renderer);
                for (Particle particle : iterable) {

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

    public void clearEffects() {

        this.byType.clear();
        this.queue.clear();
    }

}