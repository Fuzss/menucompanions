package com.fuzs.menucompanions.client.world;

import com.fuzs.menucompanions.client.util.EntityMenuContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.settings.ParticleStatus;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.particles.IParticleData;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Supplier;

public class MenuClientWorld extends ClientWorld {

    private final Minecraft mc = Minecraft.getInstance();

    private EntityMenuContainer activeContainer;

    public MenuClientWorld(ClientPlayNetHandler p_i242067_1_, ClientWorldInfo p_i242067_2_, RegistryKey<World> p_i242067_3_, DimensionType p_i242067_4_, int p_i242067_5_, Supplier<IProfiler> p_i242067_6_, WorldRenderer p_i242067_7_, boolean p_i242067_8_, long p_i242067_9_) {

        super(p_i242067_1_, p_i242067_2_, p_i242067_3_, p_i242067_4_, p_i242067_5_, p_i242067_6_, p_i242067_7_, p_i242067_8_, p_i242067_9_);
    }

    @Override
    public void playSound(double x, double y, double z, @Nonnull SoundEvent soundIn, @Nonnull SoundCategory category, float volume, float pitch, boolean distanceDelay) {

        // prevent mob sounds from playing for ender dragon and blaze
    }

    public void addParticle(@Nonnull IParticleData particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {

        this.addParticle(particleData, particleData.getType().getAlwaysShow(), x, y, z, xSpeed, ySpeed, zSpeed);
    }

    public void addParticle(@Nonnull IParticleData particleData, boolean forceAlwaysRender, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {

        this.addParticle(particleData, particleData.getType().getAlwaysShow() || forceAlwaysRender, false, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    public void addOptionalParticle(@Nonnull IParticleData particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {

        this.addParticle(particleData, false, true, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    public void addOptionalParticle(@Nonnull IParticleData particleData, boolean ignoreRange, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {

        this.addParticle(particleData, particleData.getType().getAlwaysShow() || ignoreRange, true, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    public void addParticle(IParticleData particleData, boolean ignoreRange, boolean minimizeLevel, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {

        try {

            this.addParticleUnchecked(particleData, ignoreRange, minimizeLevel, x, y, z, xSpeed, ySpeed, zSpeed);
        } catch (Throwable throwable) {

            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Exception while adding particle");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Particle being added");
            crashreportcategory.addDetail("ID", Objects.requireNonNull(ForgeRegistries.PARTICLE_TYPES.getKey(particleData.getType())));
            crashreportcategory.addDetail("Parameters", particleData.getParameters());
            crashreportcategory.addDetail("Position", () -> CrashReportCategory.getCoordinateInfo(x, y, z));
            throw new ReportedException(crashreport);
        }
    }

    private void addParticleUnchecked(IParticleData particleData, boolean alwaysRender, boolean minimizeLevel, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {

        ParticleStatus particlestatus = this.calculateParticleLevel(minimizeLevel);
        if (alwaysRender) {

            this.activeContainer.particleManager.addParticle(particleData, x, y, z, xSpeed, ySpeed, zSpeed);
        } else if (Vector3d.ZERO.squareDistanceTo(x, y, z) <= 1024.0D) {

            // distance to zero will do as everything happens almost at the origin in this world
            if (particlestatus != ParticleStatus.MINIMAL) {

                this.activeContainer.particleManager.addParticle(particleData, x, y, z, xSpeed, ySpeed, zSpeed);
            }
        }
    }

    private ParticleStatus calculateParticleLevel(boolean minimiseLevel) {

        ParticleStatus particlestatus = this.mc.gameSettings.particles;
        if (minimiseLevel && particlestatus == ParticleStatus.MINIMAL && this.rand.nextInt(10) == 0) {

            particlestatus = ParticleStatus.DECREASED;
        }

        if (particlestatus == ParticleStatus.DECREASED && this.rand.nextInt(3) == 0) {

            particlestatus = ParticleStatus.MINIMAL;
        }

        return particlestatus;
    }

    public void setActiveContainer(EntityMenuContainer container) {

        this.activeContainer = container;
    }

}
