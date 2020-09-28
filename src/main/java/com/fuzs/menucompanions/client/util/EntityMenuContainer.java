package com.fuzs.menucompanions.client.util;

import com.fuzs.menucompanions.MenuCompanions;
import com.fuzs.menucompanions.client.handler.MenuEntityHandler;
import com.fuzs.menucompanions.client.particle.MenuParticleManager;
import com.fuzs.menucompanions.client.world.MenuClientWorld;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Consumer;

@SuppressWarnings("deprecation")
public class EntityMenuContainer {

    private final Minecraft mc;
    private final MenuClientWorld world;
    public final MenuParticleManager particleManager;

    private boolean enabled;
    private boolean valid;
    private boolean firstRender;

    private Entity entity;
    private Entity[] selfAndPassengers;
    private boolean tick;
    private float scale;
    private int xOffset;
    private int yOffset;
    private boolean nameplate;
    private boolean particles;

    public EntityMenuContainer(Minecraft mc, MenuClientWorld world) {

        this.mc = mc;
        this.world = world;
        this.particleManager = new MenuParticleManager(mc ,world);
    }

    public void createEntity(@Nonnull Entity entity, @Nonnull EntityMenuEntry entry, MenuSide side) {

        this.entity = entity;
        this.selfAndPassengers = entity.getSelfAndPassengers().toArray(Entity[]::new);
        this.tick = entry.isTick();
        this.scale = entry.getScale(entity);
        this.xOffset = (side == MenuSide.RIGHT ? -1 : 1) * entry.getXOffset();
        this.yOffset = -entry.getYOffset();
        this.nameplate = entry.showNameplate();
        this.particles = entry.showParticles();

        this.enabled = true;
        this.valid = true;
        this.firstRender = true;
        this.particleManager.clearEffects();
    }

    public void tick() {

        this.world.setActiveContainer(this);
        if (this.particles) {

            this.particleManager.tick();
        }

        for (Entity entity : this.selfAndPassengers) {

            entity.ticksExisted++;
            if (this.tick && entity instanceof LivingEntity) {

                this.safeRun(entity, safeEntity -> ((LivingEntity) safeEntity).livingTick());
            }

            if (entity.isPassenger()) {

                entity.getRidingEntity().updatePassenger(entity);
            }
        }
    }

    public void render(int posX, int posY, float scale, float mouseX, float mouseY, float partialTicks) {

        ActiveRenderInfo activerenderinfo = this.mc.gameRenderer.getActiveRenderInfo();
        // allows fire to be rendered on mobs as it requires an active render info object
        this.mc.getRenderManager().cacheActiveRenderInfo(this.world, activerenderinfo, this.entity);

        scale *= this.scale;
        posX += this.xOffset;
        posY += this.yOffset;
        mouseY -= this.entity.getEyeHeight() / 1.62F * 50.0F * this.scale;

        RenderSystem.pushMatrix();
        RenderSystem.scalef(1.0F, 1.0F, -1.0F);
        RenderSystem.translatef((float) posX, (float) posY, 50.0F);

        MatrixStack matrixstack = new MatrixStack();
        matrixstack.scale(scale, scale, scale);
        Quaternion quaternionZ = Vector3f.ZP.rotationDegrees(180.0F);
        Quaternion quaternionX = Vector3f.XP.rotationDegrees((float) Math.atan(mouseY / 40.0F) * 20.0F);
        quaternionZ.multiply(quaternionX);
        matrixstack.rotate(quaternionZ);

        this.renderParticles(matrixstack, partialTicks);
        for (Entity entity : this.selfAndPassengers) {

            Vector3d posVec = entity.getPositionVec().subtract(this.entity.getPositionVec());
            double eyeVec = entity.getPosYEye() - this.entity.getPosYEye();
            setRotationAngles(entity, mouseX, mouseY - (float) eyeVec / 1.62F * 50.0F * this.scale);
            if (this.firstRender) {

                setRotationAngles(entity, mouseX, mouseY + (float) posVec.getY() * scale);
            }

            this.safeRun(entity, renderEntity -> drawEntityOnScreen(matrixstack, posVec.getX(), posVec.getY(), posVec.getZ(), renderEntity, partialTicks));
        }

        this.firstRender = false;
        RenderSystem.popMatrix();
    }

    private void renderParticles(MatrixStack matrixstack, float partialTicks) {

        if (this.particles) {

            try {

                this.particleManager.renderParticles(matrixstack, this.mc.gameRenderer.getLightTexture(), this.mc.gameRenderer.getActiveRenderInfo(), partialTicks);
            } catch (Exception e) {

                MenuCompanions.LOGGER.warn("Exception rendering particle, skipping until reload");
                this.particles = false;
            }
        }
    }

    private void safeRun(Entity entity, Consumer<Entity> action) {

        try {

            action.accept(entity);
        } catch (Exception e) {

            MenuCompanions.LOGGER.error("Unable to handle Entity {}", entity.getDisplayName().getString(), e);
            MenuEntityHandler.addToBlacklist(Objects.requireNonNull(ForgeRegistries.ENTITIES.getKey(entity.getType())).toString());
            this.valid = false;
        }
    }

    public boolean isInvalid() {

        return !this.valid;
    }

    public void setEnabled(boolean enabled) {

        this.enabled = enabled;
    }

    public boolean isEnabled() {

        return this.enabled && this.entity != null;
    }

    private static void drawEntityOnScreen(MatrixStack matrixstack, double posX, double posY, double posZ, Entity entity, float partialTicks) {

        matrixstack.translate(posX, posY, posZ);
        EntityRendererManager entityrenderermanager = Minecraft.getInstance().getRenderManager();
        entityrenderermanager.setRenderShadow(false);
        IRenderTypeBuffer.Impl irendertypebuffer$impl = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        RenderSystem.runAsFancy(() -> entityrenderermanager.renderEntityStatic(entity, 0.0D, 0.0D, 0.0D, 0.0F, partialTicks, matrixstack, irendertypebuffer$impl, 15728880));

        irendertypebuffer$impl.finish();
        entityrenderermanager.setRenderShadow(true);
        matrixstack.translate(-posX, -posY, -posZ);
    }

    private static void setRotationAngles(Entity entity, float mouseX, float mouseY) {

        entity.prevRotationYaw = entity.rotationYaw;
        entity.prevRotationPitch = entity.rotationPitch;
        entity.rotationYaw = 180.0F + (float) Math.atan(mouseX / 40.0F) * 40.0F;
        entity.rotationPitch = -(float) Math.atan(mouseY / 40.0F) * 20.0F;
        if (entity instanceof LivingEntity) {

            LivingEntity livingEntity = (LivingEntity) entity;
            livingEntity.prevRenderYawOffset = livingEntity.renderYawOffset;
            livingEntity.prevRotationYawHead = livingEntity.rotationYawHead;
            livingEntity.renderYawOffset = 180.0F + (float) Math.atan(mouseX / 40.0F) * 20.0F;
            livingEntity.rotationYawHead = entity.rotationYaw;
        }
    }

}
