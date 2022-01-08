package fuzs.menucompanions.client.particle;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import fuzs.menucompanions.mixin.client.accessor.ParticleEngineAccessor;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.ParticleOptions;
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
    private final ClientLevel level;
    private final TextureManager textureManager;
    private final ParticleEngine particleEngine;
    private final Map<ParticleRenderType, Queue<Particle>> particles = Maps.newIdentityHashMap();
    private final Queue<Particle> particlesToAdd = Queues.newArrayDeque();

    public MenuParticleManager(Minecraft minecraft, ClientLevel level) {
        this.level = level;
        this.textureManager = minecraft.textureManager;
        this.particleEngine = minecraft.particleEngine;
    }

    public void addParticle(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        Particle particle = this.makeParticle(particleData, x, y, z, xSpeed, ySpeed, zSpeed);
        if (particle != null) {
            this.addEffect(particle);
        }
    }

    @Nullable
    private <T extends ParticleOptions> Particle makeParticle(T particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        ParticleProvider<T> particleprovider = this.getParticleFactory(particleData);
        return particleprovider == null ? null : particleprovider.createParticle(particleData, this.level, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    @SuppressWarnings("unchecked")
    private <T extends ParticleOptions> ParticleProvider<T> getParticleFactory(T particleData) {
        return (ParticleProvider<T>) ((ParticleEngineAccessor) this.particleEngine).getProviders().get(ForgeRegistries.PARTICLE_TYPES.getKey(particleData.getType()));
    }

    public void addEffect(Particle effect) {
        this.particlesToAdd.add(effect);
    }

    @SuppressWarnings("UnstableApiUsage")
    public void tick() {
        this.particles.forEach((ParticleRenderType type, Queue<Particle> particleQueue) -> {
            this.level.getProfiler().push(type.toString());
            this.tickParticleList(particleQueue);
            this.level.getProfiler().pop();
        });
        Particle particle;
        if (!this.particlesToAdd.isEmpty()) {
            while((particle = this.particlesToAdd.poll()) != null) {
                this.particles.computeIfAbsent(particle.getRenderType(), (p_107347_) -> {
                    return EvictingQueue.create(16384);
                }).add(particle);
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

    private void tickParticle(Particle p_107394_) {
        try {
            p_107394_.tick();
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Ticking Particle");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Particle being ticked");
            crashreportcategory.setDetail("Particle", p_107394_::toString);
            crashreportcategory.setDetail("Particle Type", p_107394_.getRenderType()::toString);
            throw new ReportedException(crashreport);
        }
    }

    public void render(PoseStack poseStack, LightTexture lightTextureIn, Camera camera, float partialTicks) {
        lightTextureIn.turnOnLightLayer();
        RenderSystem.enableDepthTest();
        PoseStack posestack = RenderSystem.getModelViewStack();
        posestack.pushPose();
        posestack.mulPoseMatrix(poseStack.last().pose());
        RenderSystem.applyModelViewMatrix();
        for(ParticleRenderType particlerendertype : this.particles.keySet()) { // Forge: allow custom IParticleRenderType's
            if (particlerendertype == ParticleRenderType.NO_RENDER) continue;
            Iterable<Particle> iterable = this.particles.get(particlerendertype);
            if (iterable != null) {
                RenderSystem.setShader(GameRenderer::getParticleShader);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                Tesselator tesselator = Tesselator.getInstance();
                BufferBuilder bufferbuilder = tesselator.getBuilder();
                particlerendertype.begin(bufferbuilder, this.textureManager);
                for(Particle particle : iterable) {
                    try {
                        particle.render(bufferbuilder, camera, partialTicks);
                    } catch (Throwable throwable) {
                        CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering Particle");
                        CrashReportCategory crashreportcategory = crashreport.addCategory("Particle being rendered");
                        crashreportcategory.setDetail("Particle", particle::toString);
                        crashreportcategory.setDetail("Particle Type", particlerendertype::toString);
                        throw new ReportedException(crashreport);
                    }
                }
                particlerendertype.end(tesselator);
            }
        }
        posestack.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        lightTextureIn.turnOffLightLayer();
    }

    public void clearEffects() {
        this.particles.clear();
        this.particlesToAdd.clear();
    }
}