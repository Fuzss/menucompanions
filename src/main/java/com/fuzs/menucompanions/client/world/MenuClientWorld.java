package com.fuzs.menucompanions.client.world;

import com.fuzs.menucompanions.client.gui.EntityMenuContainer;
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
import net.minecraft.world.DimensionType;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Supplier;

public class MenuClientWorld extends ClientWorld implements IServerWorld {

    private EntityMenuContainer activeContainer;

    public MenuClientWorld(ClientPlayNetHandler connection, ClientWorldInfo worldInfo, RegistryKey<World> dimension, DimensionType dimensionType, Supplier<IProfiler> profiler, WorldRenderer worldRenderer) {

        super(connection, worldInfo, dimension, dimensionType, 0, profiler, worldRenderer, false, 0L);
    }

    @Override
    public void playSound(double x, double y, double z, @Nonnull SoundEvent soundIn, @Nonnull SoundCategory category, float volume, float pitch, boolean distanceDelay) {

        // prevent mob sounds from playing, mainly for ender dragon and blaze
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
        if (alwaysRender || particlestatus != ParticleStatus.MINIMAL) {

            this.activeContainer.particleManager.addParticle(particleData, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }

    private ParticleStatus calculateParticleLevel(boolean minimiseLevel) {

        ParticleStatus particlestatus = Minecraft.getInstance().gameSettings.particles;
        if (minimiseLevel && particlestatus == ParticleStatus.MINIMAL && this.rand.nextInt(10) == 0) {

            particlestatus = ParticleStatus.DECREASED;
        }

        if (particlestatus == ParticleStatus.DECREASED && this.rand.nextInt(3) == 0) {

            particlestatus = ParticleStatus.MINIMAL;
        }

        return particlestatus;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public ServerWorld getWorld() {

        return null;
    }

    public void setActiveContainer(EntityMenuContainer container) {

        this.activeContainer = container;
    }

}
