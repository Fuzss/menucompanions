package fuzs.menucompanions.client.multiplayer;

import fuzs.menucompanions.client.gui.MenuEntityRenderer;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Supplier;

public class MenuClientLevel extends ClientLevel implements ServerLevelAccessor {
    private MenuEntityRenderer activeRenderer;

    public MenuClientLevel(ClientPacketListener connection, ClientLevel.ClientLevelData worldInfo, ResourceKey<Level> dimension, DimensionType dimensionType, Supplier<ProfilerFiller> profiler, LevelRenderer worldRenderer) {
        super(connection, worldInfo, dimension, dimensionType, 0, 0, profiler, worldRenderer, false, 0L);
    }

    @Override
    public void playLocalSound(double x, double y, double z, @Nonnull SoundEvent soundIn, @Nonnull SoundSource category, float volume, float pitch, boolean distanceDelay) {
        // prevent mob sounds from playing, mainly for ender dragon and blaze
    }

    @Override
    public void addParticle(@Nonnull ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        this.addParticle(particleData, particleData.getType().getOverrideLimiter(), x, y, z, xSpeed, ySpeed, zSpeed);
    }

    @Override
    public void addParticle(@Nonnull ParticleOptions particleData, boolean forceAlwaysRender, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        this.addParticle(particleData, particleData.getType().getOverrideLimiter() || forceAlwaysRender, false, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    @Override
    public void addAlwaysVisibleParticle(@Nonnull ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        this.addParticle(particleData, false, true, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    @Override
    public void addAlwaysVisibleParticle(@Nonnull ParticleOptions particleData, boolean ignoreRange, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        this.addParticle(particleData, particleData.getType().getOverrideLimiter() || ignoreRange, true, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    // copied from LevelRenderer
    private void addParticle(ParticleOptions particleData, boolean ignoreRange, boolean minimizeLevel, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        try {
            this.addParticleInternal(particleData, ignoreRange, minimizeLevel, x, y, z, xSpeed, ySpeed, zSpeed);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Exception while adding particle");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Particle being added");
            crashreportcategory.setDetail("ID", Objects.requireNonNull(ForgeRegistries.PARTICLE_TYPES.getKey(particleData.getType())));
            crashreportcategory.setDetail("Parameters", particleData.writeToString());
            crashreportcategory.setDetail("Position", () -> CrashReportCategory.formatLocation(this, x, y, z));
            throw new ReportedException(crashreport);
        }
    }

    // copied from LevelRenderer
    private void addParticleInternal(ParticleOptions particleData, boolean alwaysRender, boolean minimizeLevel, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        ParticleStatus particlestatus = this.calculateParticleLevel(minimizeLevel);
        if (alwaysRender || particlestatus != ParticleStatus.MINIMAL) {
            this.activeRenderer.particleManager.addParticle(particleData, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }

    // copied from LevelRenderer
    private ParticleStatus calculateParticleLevel(boolean minimiseLevel) {
        ParticleStatus particlestatus = Minecraft.getInstance().options.particles;
        if (minimiseLevel && particlestatus == ParticleStatus.MINIMAL && this.random.nextInt(10) == 0) {
            particlestatus = ParticleStatus.DECREASED;
        }
        if (particlestatus == ParticleStatus.DECREASED && this.random.nextInt(3) == 0) {
            particlestatus = ParticleStatus.MINIMAL;
        }
        return particlestatus;
    }

    @Override
    public ServerLevel getLevel() {
        return null;
    }

    public void setActiveRenderer(MenuEntityRenderer container) {
        this.activeRenderer = container;
    }
}
