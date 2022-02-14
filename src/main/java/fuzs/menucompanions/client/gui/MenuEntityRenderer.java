package fuzs.menucompanions.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import fuzs.menucompanions.MenuCompanions;
import fuzs.menucompanions.client.handler.MenuEntityBlacklist;
import fuzs.menucompanions.client.multiplayer.MenuClientLevel;
import fuzs.menucompanions.client.particle.MenuParticleEngine;
import fuzs.menucompanions.data.entry.MobMenuData;
import fuzs.menucompanions.mixin.client.accessor.LivingEntityAccessor;
import fuzs.menucompanions.mixin.client.accessor.MobAccessor;
import fuzs.puzzleslib.util.PuzzlesUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MenuEntityRenderer implements EntityMenuStateHolder {
    private final Minecraft minecraft;
    private final Camera camera = new Camera();

    private MenuClientLevel level;
    public MenuParticleEngine particleManager;
    private EntityMenuState entityMenuState = this.getDefaultState();
    private boolean setInitialAngles;

    private Entity entity;
    private Entity[] selfAndPassengers;
    private boolean tick;
    private boolean walking;
    private boolean inLove;
    private float scale;
    private int xOffset;
    private int yOffset;
    private boolean nameplate;
    private boolean particles;
    private float volume;

    public MenuEntityRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public void setLevel(MenuClientLevel level) {
        this.level = level;
        this.particleManager = new MenuParticleEngine(this.minecraft, level);
    }

    @Override
    public EntityMenuState getState() {
        return this.entityMenuState;
    }

    @Override
    public void setState(EntityMenuState entityMenuState) {
        this.entityMenuState = entityMenuState;
    }

    @Override
    public void setEnabled(boolean enable) {
        EntityMenuStateHolder.super.setEnabled(enable);
        if (this.entity == null) {
            this.setInvalid();
        }
    }

    public void createEntity(@Nonnull Entity entity, @Nonnull MobMenuData entry, boolean isRightSide) {
        this.entity = entity;
        this.copyEntryData(entry, entity, isRightSide);
        this.selfAndPassengers = this.getSelfAndPassengers(entity);
        this.setEnabled(true);
        this.setInitialAngles = true;
    }

    private Entity[] getSelfAndPassengers(Entity entity) {
        Entity[] selfAndPassengers = entity.getSelfAndPassengers().toArray(Entity[]::new);
        if (!copyAllEntityData(this.selfAndPassengers, selfAndPassengers)) {
            this.particleManager.clearEffects();
        }
        return selfAndPassengers;
    }

    private void copyEntryData(MobMenuData entry, Entity entity, boolean isRightSide) {
        this.tick = entry.tick();
        this.walking = entry.walking();
        this.inLove = entry.inLove();
        this.scale = entry.calculateScale(entity);
        this.xOffset = (isRightSide ? -1 : 1) * entry.getOffsetX();
        this.yOffset = -entry.getOffsetY();
        this.nameplate = entry.showNameplate();
        this.particles = entry.showParticles();
        this.volume = entry.getSoundVolume();
    }

    public void tick() {
        if (this.isNotEnabled()) return;
        this.level.setActiveRenderer(this);
        if (this.particles) {
            this.particleManager.tick();
        }
        for (Entity entity : this.selfAndPassengers) {
            entity.tickCount++;
            if (entity instanceof LivingEntity) {
                this.calculateEntityAnimation((LivingEntity) entity, this.walking ? (entity.isCrouching() ? 0.18F : 0.6F) : 0.0F);
                this.spawnNectarParticles(entity);
                if (((LivingEntity) entity).hurtTime > 0) {
                    --((LivingEntity) entity).hurtTime;
                }
                if (this.tick) {
                    PuzzlesUtil.runOrElse(entity, safeEntity -> ((LivingEntity) safeEntity).aiStep(), safeEntity -> this.tick = false);
                }
            }
            this.spawnHeartParticles(entity);
            if (entity.isPassenger()) {
                entity.getVehicle().positionRider(entity);
            }
        }
    }

    private void calculateEntityAnimation(LivingEntity livingEntity, float amount) {
        livingEntity.animationSpeedOld = livingEntity.animationSpeed;
        livingEntity.animationSpeed += (Mth.clamp(amount, 0.0F, 1.0F) - livingEntity.animationSpeed) * 0.4F;
        livingEntity.animationPosition += livingEntity.animationSpeed;
    }

    private void spawnNectarParticles(Entity entity) {
        if (entity instanceof Bee bee && bee.hasNectar() && this.level.random.nextFloat() < 0.05F) {
            for (int i = 0; i < this.level.random.nextInt(2) + 1; ++i) {
                double posX = Mth.lerp(this.level.random.nextDouble(), entity.getX() - 0.3, entity.getX() + 0.3);
                double posY = entity.getY(0.5);
                double posZ = Mth.lerp(this.level.random.nextDouble(), entity.getZ() - 0.3, entity.getZ() + 0.3);
                this.level.addParticle(ParticleTypes.FALLING_NECTAR, posX, posY, posZ, 0.0, 0.0, 0.0);
            }
        }
    }

    private void spawnHeartParticles(Entity entity) {
        if (this.inLove && entity.tickCount % 10 == 0) {
            double speedX = this.level.random.nextGaussian() * 0.02;
            double speedY = this.level.random.nextGaussian() * 0.02;
            double speedZ = this.level.random.nextGaussian() * 0.02;
            this.level.addParticle(ParticleTypes.HEART, entity.getRandomX(1.0), entity.getRandomY() + 0.5, entity.getRandomZ(1.0), speedX, speedY, speedZ);
        }
    }

    public void render(int posX, int posY, float scale, float mouseX, float mouseY, float partialTicks) {
        if (this.isNotEnabled()) return;
        // allows fire to be rendered on mobs as it requires an active render info object
        this.minecraft.getEntityRenderDispatcher().prepare(this.level, this.camera, this.entity);
        scale *= this.scale;
        posX += this.xOffset;
        posY += this.yOffset;
        // only offset upwards, never downwards
        posY -= Math.max(0.0F, 0.9F - this.entity.getBbHeight() / 2.0F) * 30;
        mouseX += posX;
        mouseY += posY;
        mouseY -= this.entity.getEyeHeight() / 1.62F * 50.0F * this.scale;
        PoseStack posestack = RenderSystem.getModelViewStack();
        posestack.pushPose();
        posestack.translate((float) posX, (float) posY, 50.0F);
        posestack.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();
        PoseStack matrixstack = new PoseStack();
        matrixstack.scale(scale, scale, scale);
        Quaternion quaternionZ = Vector3f.ZP.rotationDegrees(180.0F);
        Quaternion quaternionX = Vector3f.XP.rotationDegrees((float) Math.atan(mouseY / 40.0F) * 20.0F);
        quaternionZ.mul(quaternionX);
        matrixstack.mulPose(quaternionZ);
        this.renderParticles(matrixstack, partialTicks);
        Lighting.setupForEntityInInventory();
        for (Entity entity : this.selfAndPassengers) {
            Vec3 pos = entity.position().subtract(this.entity.position());
            double eyeHeight = entity.getEyeY() - this.entity.getEyeY();
            if (this.setInitialAngles) {
                setRotationAngles(entity, mouseX, mouseY + (float) pos.y() * scale);
                // run single tick to update passenger position
                this.tick();
            }
            setRotationAngles(entity, mouseX, mouseY - (float) eyeHeight / 1.62F * 50.0F * this.scale);
            drawEntityOnScreen(matrixstack, pos.x(), pos.y(), pos.z(), partialTicks, entity, (irendertypebuffer, packedLightIn) -> {
                if (this.nameplate) {
                    matrixstack.pushPose();
                    float downscale = 1.0F / this.scale;
                    matrixstack.scale(downscale, downscale, downscale);
                    Consumer<Entity> render = safeEntity -> renderName(matrixstack, irendertypebuffer, packedLightIn, safeEntity, (safeEntity.getBbHeight() + 0.5F) * this.scale);
                    PuzzlesUtil.runOrElse(entity, render, safeEntity -> this.nameplate = false);
                    matrixstack.popPose();
                }
            }, this::setInvalid);
        }
        this.setInitialAngles = false;
        posestack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
    }

    private void renderParticles(PoseStack matrixstack, float partialTicks) {
        if (!this.particles) return;
        try {
            matrixstack.pushPose();
            this.particleManager.render(matrixstack, this.minecraft.gameRenderer.lightTexture(), this.minecraft.gameRenderer.getMainCamera(), partialTicks);
            matrixstack.popPose();
        } catch (Exception e) {
            MenuCompanions.LOGGER.warn("Exception rendering particle, skipping until reload");
            this.particles = false;
        }
    }

    public void interactWithEntity(SoundManager soundManager, boolean playAmbientSounds, boolean hurtEntity) {
        if (this.isNotEnabled()) return;
        this.level.setActiveRenderer(this);
        List<Entity> entities = Stream.of(this.selfAndPassengers).filter(entity -> entity instanceof LivingEntity).collect(Collectors.toList());
        if (entities.isEmpty()) return;
        LivingEntity livingEntity = (LivingEntity) entities.get(this.level.random.nextInt(entities.size()));
        if (playAmbientSounds && livingEntity instanceof Mob) {
            SoundEvent ambientSound = ((MobAccessor) livingEntity).callGetAmbientSound();
            if (this.playLivingSound(soundManager, livingEntity, ambientSound, this.volume)) return;
        }
        if (hurtEntity) {
            if (livingEntity.hurtTime == 0) {
                livingEntity.hurtTime = 10;
                livingEntity.animationSpeed = 1.5F;
                this.spawnDamageParticles(livingEntity);
                SoundEvent hurtSound = ((LivingEntityAccessor) livingEntity).callGetHurtSound(DamageSource.GENERIC);
                this.playLivingSound(soundManager, livingEntity, hurtSound, this.volume);
            }
        } else if (!livingEntity.swinging) {
            livingEntity.swing(InteractionHand.MAIN_HAND);
        }
    }

    private boolean playLivingSound(SoundManager manager, LivingEntity livingEntity, SoundEvent soundEvent, float volume) {
        if (soundEvent != null && !livingEntity.isSilent()) {
            float soundVolume = ((LivingEntityAccessor) livingEntity).callGetSoundVolume() * volume;
            float soundPitch = ((LivingEntityAccessor) livingEntity).callGetVoicePitch();
            manager.play(new SimpleSoundInstance(soundEvent.getLocation(), livingEntity.getSoundSource(), soundVolume, soundPitch, false, 0, SoundInstance.Attenuation.NONE, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), true));
            return true;
        }
        return false;
    }

    private void spawnDamageParticles(LivingEntity livingEntity) {
        for (int i = 0; i < this.level.random.nextInt(5) + 1; i++) {
            double posX = livingEntity.getX() + this.level.random.nextGaussian() * 0.2;
            double posY = livingEntity.getY(0.5);
            double posZ = livingEntity.getZ() - 0.3 + this.level.random.nextGaussian() * 0.2;
            double xSpeed = this.level.random.nextGaussian() * 0.02;
            double zSpeed = this.level.random.nextGaussian() * 0.02;
            this.level.addParticle(ParticleTypes.DAMAGE_INDICATOR, posX, posY, posZ, xSpeed, 0.0, zSpeed);
        }
    }

    private static void setRotationAngles(Entity entity, float mouseX, float mouseY) {
        entity.yRotO = entity.getYRot();
        entity.xRotO = entity.getXRot();
        entity.setYRot(180.0F + (float) Math.atan(mouseX / 40.0F) * 40.0F);
        entity.setXRot(-(float) Math.atan(mouseY / 40.0F) * 20.0F);
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.yBodyRotO = livingEntity.yBodyRot;
            livingEntity.yHeadRotO = livingEntity.yHeadRot;
            livingEntity.yBodyRot = 180.0F + (float) Math.atan(mouseX / 40.0F) * 20.0F;
            livingEntity.yHeadRot = entity.getYRot();
        }
    }

    private static boolean copyAllEntityData(Entity[] source, @Nonnull Entity[] target) {
        if (source == null) return false;
        boolean allSuccessful = true;
        int bound = Math.min(source.length, target.length);
        for (int i = 0; i < bound; i++) {
            Pair<Entity, Entity> pair = Pair.of(source[i], target[i]);
            if (pair.getLeft().getType() == pair.getRight().getType()) {
                copyEntityData(pair.getLeft(), pair.getRight());
            } else {
                allSuccessful = false;
            }
        }
        return allSuccessful;
    }

    private static void copyEntityData(Entity source, @Nonnull Entity target) {
        if (source == null) return;
        target.tickCount = source.tickCount;
        copyRotationAngles(source, target);
        if (source instanceof LivingEntity livingSource && target instanceof LivingEntity livingTarget) {
            livingTarget.animationSpeedOld = livingSource.animationSpeedOld;
            livingTarget.animationSpeed = livingSource.animationSpeed;
            livingTarget.animationPosition = livingSource.animationPosition;
            livingTarget.hurtTime = livingSource.hurtTime;
        }
    }

    private static void copyRotationAngles(Entity source, Entity target) {
        target.yRotO = source.yRotO;
        target.xRotO = source.xRotO;
        target.setYRot(source.getYRot());
        target.setXRot(source.getXRot());
        if (source instanceof LivingEntity livingSource && target instanceof LivingEntity livingTarget) {
            livingTarget.yBodyRotO = livingSource.yBodyRotO;
            livingTarget.yHeadRotO = livingSource.yHeadRotO;
            livingTarget.yBodyRot = livingSource.yBodyRot;
            livingTarget.yHeadRot = livingSource.yHeadRot;
        }
    }

    private static void drawEntityOnScreen(PoseStack matrixstack, double posX, double posY, double posZ, float partialTicks, Entity entity, BiConsumer<MultiBufferSource.BufferSource, Integer> renderName, Runnable invalidate) {
        matrixstack.pushPose();
        matrixstack.translate(posX, posY, posZ);
        EntityRenderDispatcher rendererManager = Minecraft.getInstance().getEntityRenderDispatcher();
        rendererManager.setRenderShadow(false);
        MultiBufferSource.BufferSource irendertypebuffer = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.runAsFancy(() -> {
            Consumer<Entity> render = safeEntity -> rendererManager.render(safeEntity, 0.0, 0.0, 0.0, 0.0F, partialTicks, matrixstack, irendertypebuffer, 15728880);
            Consumer<Entity> orElse = safeEntity -> {
                MenuEntityBlacklist.addToBlacklist(safeEntity.getType());
                invalidate.run();
            };
            PuzzlesUtil.runOrElse(entity, render, orElse);
            renderName.accept(irendertypebuffer, 15728880);
        });
        irendertypebuffer.endBatch();
        rendererManager.setRenderShadow(true);
        matrixstack.popPose();
    }

    private static void renderName(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, Entity entityIn, float renderHeight) {
        Component displayNameIn = entityIn.getDisplayName();
        float renderOffset = "deadmau5".equals(displayNameIn.getString()) ? -10 : 0;
        for (int i = 0; i < 4; i++) {
            matrixStackIn.pushPose();
            matrixStackIn.translate(0.0, renderHeight, 0.0);
            matrixStackIn.scale(-0.025F, -0.025F, 0.025F);
            Matrix4f matrix4f = matrixStackIn.last().pose();
            float backgroundOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
            int alpha = (int) (backgroundOpacity * 255.0F) << 24;
            Font font = Minecraft.getInstance().font;
            int textWidth = -font.width(displayNameIn) / 2;
            font.drawInBatch(displayNameIn, textWidth, renderOffset - i * 15, 553648127, false, matrix4f, bufferIn, true, alpha, packedLightIn);
            font.drawInBatch(displayNameIn, textWidth, renderOffset - i * 15, -1, false, matrix4f, bufferIn, false, 0, packedLightIn);
            matrixStackIn.popPose();
        }
    }
}
