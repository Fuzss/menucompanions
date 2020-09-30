package com.fuzs.menucompanions.client.gui;

import com.fuzs.menucompanions.MenuCompanions;
import com.fuzs.menucompanions.client.handler.MenuEntityHandler;
import com.fuzs.menucompanions.client.particle.MenuParticleManager;
import com.fuzs.menucompanions.client.storage.EntityMenuEntry;
import com.fuzs.menucompanions.client.world.MenuClientWorld;
import com.fuzs.menucompanions.mixin.ActiveRenderInfoAccessorMixin;
import com.fuzs.menucompanions.mixin.LivingEntityAccessorMixin;
import com.fuzs.menucompanions.mixin.MobEntityAccessorMixin;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.passive.IFlyingAnimal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        this.particleManager = new MenuParticleManager(mc, world);
    }

    public void createEntity(@Nonnull Entity entity, @Nonnull EntityMenuEntry entry, MenuEntityHandler.MenuSide side) {

        this.entity = entity;
        this.selfAndPassengers = entity.getSelfAndPassengers().toArray(Entity[]::new);
        this.tick = entry.isTick();
        this.scale = entry.getScale(entity);
        this.xOffset = (side == MenuEntityHandler.MenuSide.RIGHT ? -1 : 1) * entry.getXOffset();
        this.yOffset = -entry.getYOffset();
        this.nameplate = entry.showNameplate();
        this.particles = entry.showParticles();

        this.enabled = true;
        this.valid = true;
        this.firstRender = true;
        this.particleManager.clearEffects();
    }

    public void tick() {

        if (!this.enabled || this.entity == null) {

            return;
        }

        this.world.setActiveContainer(this);
        if (this.particles) {

            this.particleManager.tick();
        }

        for (Entity entity : this.selfAndPassengers) {

            entity.ticksExisted++;
            if (entity instanceof LivingEntity) {

                if (entity instanceof PlayerEntity) {

                    LivingEntity livingEntity = (LivingEntity) entity;
                    livingEntity.func_233629_a_(livingEntity, livingEntity instanceof IFlyingAnimal);
                    if (livingEntity.hurtTime > 0) {

                        --livingEntity.hurtTime;
                    }
                }

                if (this.tick) {

                    this.tick = MenuEntityHandler.runOrElse(entity, safeEntity -> ((LivingEntity) safeEntity).livingTick(), safeEntity -> {});
                }
            }

            if (entity.isPassenger()) {

                Objects.requireNonNull(entity.getRidingEntity()).updatePassenger(entity);
            }
        }
    }

    public void render(int posX, int posY, float scale, float mouseX, float mouseY, float partialTicks) {

        if (!this.enabled || this.entity == null) {

            return;
        }

        ActiveRenderInfo activerenderinfo = this.mc.gameRenderer.getActiveRenderInfo();
        // allows fire to be rendered on mobs as it requires an active render info object
        this.mc.getRenderManager().cacheActiveRenderInfo(this.world, activerenderinfo, this.entity);
        ((ActiveRenderInfoAccessorMixin) activerenderinfo).callSetPosition(Vector3d.ZERO);
        ((ActiveRenderInfoAccessorMixin) activerenderinfo).callSetDirection(0.0F, 0.0F);

        scale *= this.scale;
        posX += this.xOffset;
        posY += this.yOffset;
        posY -= Math.max(0.0F, 0.9F - this.entity.getHeight() / 2.0F) * 30;
        mouseX += posX;
        mouseY += posY;
        mouseY -= this.entity.getEyeHeight() / 1.62F * 50.0F * this.scale;

        RenderSystem.pushMatrix();
        RenderSystem.translatef((float) posX, (float) posY, 50.0F);
        RenderSystem.scalef(1.0F, 1.0F, -1.0F);

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

            this.valid = MenuEntityHandler.runOrElse(entity, renderEntity -> drawEntityOnScreen(matrixstack, posVec.getX(), posVec.getY(), posVec.getZ(), partialTicks, renderEntity, (irendertypebuffer, packedLightIn) -> {

                if (this.nameplate) {

                    matrixstack.push();
                    float downscale = 1.0F / this.scale;
                    matrixstack.scale(downscale, downscale, downscale);
                    renderName(matrixstack, irendertypebuffer, packedLightIn, entity, (entity.getHeight() + 0.5F) * this.scale);
                    matrixstack.pop();
                }
            }), renderEntity -> MenuEntityHandler.addToBlacklist(renderEntity.getType()));
        }

        this.firstRender = false;
        RenderSystem.popMatrix();
    }

    private void renderParticles(MatrixStack matrixstack, float partialTicks) {

        if (this.particles) {

            try {

                matrixstack.push();
                this.particleManager.renderParticles(matrixstack, this.mc.gameRenderer.getLightTexture(), this.mc.gameRenderer.getActiveRenderInfo(), partialTicks);
                matrixstack.pop();
            } catch (Exception e) {

                MenuCompanions.LOGGER.warn("Exception rendering particle, skipping until reload");
                this.particles = false;
            }
        }
    }

    public void playLivingSound(SoundHandler handler, float volume, boolean hurtPlayer) {

        if (!this.enabled || this.entity == null) {

            return;
        }

        List<Entity> entities = Stream.of(this.selfAndPassengers).filter(entity -> entity instanceof LivingEntity).collect(Collectors.toList());
        if (entities.isEmpty()) {

            return;
        }

        Collections.shuffle(entities);
        LivingEntity livingEntity = ((LivingEntity) entities.get((int) (entities.size() * Math.random())));
        if (livingEntity instanceof MobEntity) {

            SoundEvent ambientSound = ((MobEntityAccessorMixin) livingEntity).callGetAmbientSound();
            if (this.playLivingSound(handler, livingEntity, ambientSound, volume)) {

                return;
            }
        }

        if (hurtPlayer && livingEntity instanceof PlayerEntity && livingEntity.hurtTime == 0) {

            livingEntity.hurtTime = 10;
            livingEntity.limbSwingAmount = 1.5F;
            for(int i = 0; i < 2; i++) {

                double posX = livingEntity.getPosX() + this.world.rand.nextGaussian() * 0.2;
                double posY = livingEntity.getPosYHeight(0.5);
                double posZ = livingEntity.getPosZ() - 0.3 + this.world.rand.nextGaussian() * 0.2;
                double xSpeed = this.world.rand.nextGaussian() * 0.02;
                double zSpeed = this.world.rand.nextGaussian() * 0.02;
                this.world.addParticle(ParticleTypes.DAMAGE_INDICATOR, posX, posY, posZ, xSpeed, 0.0, zSpeed);
            }

            SoundEvent hurtSound = ((LivingEntityAccessorMixin) livingEntity).callGetHurtSound(DamageSource.GENERIC);
            this.playLivingSound(handler, livingEntity, hurtSound, volume);
        }
    }

    private boolean playLivingSound(SoundHandler handler, LivingEntity livingEntity, SoundEvent soundEvent, float volume) {

        if (soundEvent != null && !livingEntity.isSilent()) {

            float soundVolume = ((LivingEntityAccessorMixin) livingEntity).callGetSoundVolume() * volume;
            float soundPitch = ((LivingEntityAccessorMixin) livingEntity).callGetSoundPitch();
            handler.play(new SimpleSound(soundEvent.getName(), livingEntity.getSoundCategory(), soundVolume, soundPitch, false, 0,
                    ISound.AttenuationType.NONE, livingEntity.getPosX(), livingEntity.getPosY(), livingEntity.getPosZ(), true));

            return true;
        }

        return false;
    }

    public boolean isInvalid() {

        return !this.valid;
    }

    public void setEnabled(boolean enabled) {

        this.enabled = enabled;
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

    private static void drawEntityOnScreen(MatrixStack matrixstack, double posX, double posY, double posZ, float partialTicks, Entity entity, BiConsumer<IRenderTypeBuffer, Integer> renderName) {

        matrixstack.push();
        matrixstack.translate(posX, posY, posZ);
        EntityRendererManager entityrenderermanager = Minecraft.getInstance().getRenderManager();
        entityrenderermanager.setRenderShadow(false);
        IRenderTypeBuffer.Impl irendertypebuffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        RenderSystem.runAsFancy(() -> {

            entityrenderermanager.renderEntityStatic(entity, 0.0, 0.0, 0.0, 0.0F, partialTicks, matrixstack, irendertypebuffer, 15728880);
            renderName.accept(irendertypebuffer, 15728880);
        });

        irendertypebuffer.finish();
        entityrenderermanager.setRenderShadow(true);
        matrixstack.pop();
    }

    private static void renderName(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, Entity entityIn, float renderHeight) {

        ITextComponent displayNameIn = entityIn.getDisplayName();
        float renderOffset = "deadmau5".equals(displayNameIn.getString()) ? -10 : 0;

        matrixStackIn.push();
        matrixStackIn.translate(0.0, renderHeight, 0.0);
        matrixStackIn.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix4f = matrixStackIn.getLast().getMatrix();

        float backgroundOpacity = Minecraft.getInstance().gameSettings.getTextBackgroundOpacity(0.25F);
        int alpha = (int) (backgroundOpacity * 255.0F) << 24;
        FontRenderer fontrenderer = Minecraft.getInstance().fontRenderer;
        int textWidth = -fontrenderer.func_238414_a_(displayNameIn) / 2;
        fontrenderer.func_243247_a(displayNameIn, textWidth, renderOffset, 553648127, false, matrix4f, bufferIn, true, alpha, packedLightIn);
        fontrenderer.func_243247_a(displayNameIn, textWidth, renderOffset, -1, false, matrix4f, bufferIn, false, 0, packedLightIn);

        matrixStackIn.pop();
    }

}
