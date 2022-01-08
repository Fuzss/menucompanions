package fuzs.menucompanions.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import fuzs.menucompanions.MenuCompanions;
import fuzs.menucompanions.client.handler.MenuEntityBlacklist;
import fuzs.menucompanions.client.particle.MenuParticleManager;
import fuzs.menucompanions.client.storage.entry.EntityMenuData;
import fuzs.menucompanions.client.world.MenuClientWorld;
import fuzs.menucompanions.mixin.client.accessor.IActiveRenderInfoAccessor;
import fuzs.menucompanions.mixin.client.accessor.ILivingEntityAccessor;
import fuzs.menucompanions.mixin.client.accessor.IMobEntityAccessor;
import com.fuzs.puzzleslib_mc.util.PuzzlesLibUtil;
import com.mojang.blaze3d.matrix.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import fuzs.puzzleslib.util.PuzzlesUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.Mth;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Mth;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Bee;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MenuEntityRenderer implements EntityMenuStateHolder {
    private final Minecraft minecraft;

    private MenuClientWorld level;
    public MenuParticleManager particleManager;
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

    public void setLevel(MenuClientWorld level) {
        this.level = level;
        this.particleManager = new MenuParticleManager(this.minecraft, level);
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

    public void createEntity(@Nonnull Entity entity, @Nonnull EntityMenuData entry, boolean isRightSide) {
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

    private void copyEntryData(EntityMenuData entry, Entity entity, boolean isRightSide) {

        this.tick = entry.tick();
        this.walking = entry.walking();
        this.inLove = entry.inLove();
        this.scale = entry.calculateScale(entity);
        this.xOffset = (isRightSide ? -1 : 1) * entry.getXOffset();
        this.yOffset = -entry.getYOffset();
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
            entity.ticksExisted++;
            if (entity instanceof LivingEntity) {
                this.updateLimbSwing((LivingEntity) entity, this.walking ? (entity.isCrouching() ? 0.18F : 0.6F) : 0.0F);
                this.spawnNectarParticles(entity);
                if (((LivingEntity) entity).hurtTime > 0) {
                    --((LivingEntity) entity).hurtTime;
                }
                if (this.tick) {
                    PuzzlesUtil.runOrElse(entity, safeEntity -> ((LivingEntity) safeEntity).livingTick(), safeEntity -> this.tick = false);
                }
            }
            this.spawnHeartParticles(entity);
            if (entity.isPassenger()) {
                entity.getRidingEntity().updatePassenger(entity);
            }
        }
    }

    private void updateLimbSwing(LivingEntity livingEntity, float amount) {
        livingEntity.prevLimbSwingAmount = livingEntity.limbSwingAmount;
        livingEntity.limbSwingAmount += (Mth.clamp(amount, 0.0F, 1.0F) - livingEntity.limbSwingAmount) * 0.4F;
        livingEntity.limbSwing += livingEntity.limbSwingAmount;
    }

    private void spawnNectarParticles(Entity entity) {

        if (entity instanceof Bee bee && bee.hasNectar() && this.level.randomom.nextFloat() < 0.05F) {

            for (int i = 0; i < this.level.random.nextInt(2) + 1; ++i) {

                double posX = Mth.lerp(this.level.random.nextDouble(), entity.getPosX() - 0.3, entity.getPosX() + 0.3);
                double posY = entity.getPosYHeight(0.5);
                double posZ = Mth.lerp(this.level.random.nextDouble(), entity.getPosZ() - 0.3, entity.getPosZ() + 0.3);
                this.level.addParticle(ParticleTypes.FALLING_NECTAR, posX, posY, posZ, 0.0, 0.0, 0.0);
            }
        }
    }

    private void spawnHeartParticles(Entity entity) {
        if (this.inLove && entity.ticksExisted % 10 == 0) {
            double speedX = this.level.random.nextGaussian() * 0.02;
            double speedY = this.level.random.nextGaussian() * 0.02;
            double speedZ = this.level.random.nextGaussian() * 0.02;
            this.level.addParticle(ParticleTypes.HEART, entity.getPosXRandom(1.0), entity.getPosYRandom() + 0.5, entity.getPosZRandom(1.0), speedX, speedY, speedZ);
        }
    }

    public void render(int posX, int posY, float scale, float mouseX, float mouseY, float partialTicks) {
        if (this.isNotEnabled()) return;
        ActiveRenderInfo activerenderinfo = this.minecraft.gameRenderer.getActiveRenderInfo();
        // allows fire to be rendered on mobs as it requires an active render info object
        this.minecraft.getRenderManager().cacheActiveRenderInfo(this.level, activerenderinfo, this.entity);
        ((IActiveRenderInfoAccessor) activerenderinfo).callSetPosition(Vector3d.ZERO);
        ((IActiveRenderInfoAccessor) activerenderinfo).callSetDirection(0.0F, 0.0F);

        scale *= this.scale;
        posX += this.xOffset;
        posY += this.yOffset;
        // only offset upwards, never downwards
        posY -= Math.max(0.0F, 0.9F - this.entity.getHeight() / 2.0F) * 30;
        mouseX += posX;
        mouseY += posY;
        mouseY -= this.entity.getEyeHeight() / 1.62F * 50.0F * this.scale;

        RenderSystem.disableLighting();
        RenderSystem.pushMatrix();
        RenderSystem.translatef((float) posX, (float) posY, 50.0F);
        RenderSystem.scalef(1.0F, 1.0F, -1.0F);

        PoseStack matrixstack = new PoseStack();
        matrixstack.scale(scale, scale, scale);
        Quaternion quaternionZ = Vector3f.ZP.rotationDegrees(180.0F);
        Quaternion quaternionX = Vector3f.XP.rotationDegrees((float) Math.atan(mouseY / 40.0F) * 20.0F);
        quaternionZ.multiply(quaternionX);
        matrixstack.rotate(quaternionZ);

        this.renderParticles(matrixstack, partialTicks);
        for (Entity entity : this.selfAndPassengers) {

            Vector3d posVec = entity.getPositionVec().subtract(this.entity.getPositionVec());
            double eyeVec = entity.getPosYEye() - this.entity.getPosYEye();
            if (this.setInitialAngles) {

                setRotationAngles(entity, mouseX, mouseY + (float) posVec.getY() * scale);
                // run single tick to update passenger position
                this.tick();
            }

            setRotationAngles(entity, mouseX, mouseY - (float) eyeVec / 1.62F * 50.0F * this.scale);
            drawEntityOnScreen(matrixstack, posVec.getX(), posVec.getY(), posVec.getZ(), partialTicks, entity, (irendertypebuffer, packedLightIn) -> {

                if (this.nameplate) {

                    matrixstack.pushPose();
                    float downscale = 1.0F / this.scale;
                    matrixstack.scale(downscale, downscale, downscale);
                    Consumer<Entity> render = safeEntity -> renderName(matrixstack, irendertypebuffer, packedLightIn, safeEntity, (safeEntity.getHeight() + 0.5F) * this.scale);
                    PuzzlesLibUtil.runOrElse(entity, render, safeEntity -> this.nameplate = false);
                    matrixstack.popPose();
                }
            }, this::setInvalid);
        }

        this.setInitialAngles = false;
        RenderSystem.popMatrix();
    }

    private void renderParticles(PoseStack matrixstack, float partialTicks) {
        if (this.particles) {
            try {
                matrixstack.pushPose();
                this.particleManager.render(matrixstack, this.minecraft.gameRenderer.lightTexture(), this.minecraft.gameRenderer.getMainCamera(), partialTicks);
                matrixstack.popPose();
            } catch (Exception e) {
                MenuCompanions.LOGGER.warn("Exception rendering particle, skipping until reload");
                this.particles = false;
            }
        }
    }

    public void interactWithEntity(SoundManager soundManager, boolean playAmbientSounds, boolean hurtEntity) {
        if (this.isNotEnabled()) return;
        this.level.setActiveRenderer(this);
        List<Entity> entities = Stream.of(this.selfAndPassengers).filter(entity -> entity instanceof LivingEntity).collect(Collectors.toList());
        if (entities.isEmpty()) {

            return;
        }

        LivingEntity livingEntity = (LivingEntity) entities.get(this.level.random.nextInt(entities.size()));
        if (playAmbientSounds && livingEntity instanceof Mob) {
            SoundEvent ambientSound = ((IMobEntityAccessor) livingEntity).callGetAmbientSound();
            if (this.playLivingSound(soundManager, livingEntity, ambientSound, this.volume)) return;
        }
        if (hurtEntity) {
            if (livingEntity.hurtTime == 0) {
                livingEntity.hurtTime = 10;
                livingEntity.limbSwingAmount = 1.5F;
                this.spawnDamageParticles(livingEntity);
                SoundEvent hurtSound = ((ILivingEntityAccessor) livingEntity).callGetHurtSound(DamageSource.GENERIC);
                this.playLivingSound(soundManager, livingEntity, hurtSound, this.volume);
            }
        } else if (!livingEntity.isSwingInProgress) {
            livingEntity.swingArm(Hand.MAIN_HAND);
        }
    }

    private boolean playLivingSound(SoundHandler handler, LivingEntity livingEntity, SoundEvent soundEvent, float volume) {

        if (soundEvent != null && !livingEntity.isSilent()) {

            float soundVolume = ((ILivingEntityAccessor) livingEntity).callGetSoundVolume() * volume;
            float soundPitch = ((ILivingEntityAccessor) livingEntity).callGetSoundPitch();
            handler.play(new SimpleSound(soundEvent.getName(), livingEntity.getSoundCategory(), soundVolume, soundPitch, false, 0,
                    ISound.AttenuationType.NONE, livingEntity.getPosX(), livingEntity.getPosY(), livingEntity.getPosZ(), true));

            return true;
        }

        return false;
    }

    private void spawnDamageParticles(LivingEntity livingEntity) {

        for(int i = 0; i < this.level.random.nextInt(5) + 1; i++) {

            double posX = livingEntity.getPosX() + this.level.random.nextGaussian() * 0.2;
            double posY = livingEntity.getPosYHeight(0.5);
            double posZ = livingEntity.getPosZ() - 0.3 + this.level.random.nextGaussian() * 0.2;
            double xSpeed = this.level.random.nextGaussian() * 0.02;
            double zSpeed = this.level.random.nextGaussian() * 0.02;
            this.level.addParticle(ParticleTypes.DAMAGE_INDICATOR, posX, posY, posZ, xSpeed, 0.0, zSpeed);
        }
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

    private static boolean copyAllEntityData(Entity[] source, @Nonnull Entity[] target) {

        if (source == null) {

            return false;
        }

        boolean successfulForAll = true;
        int bound = Math.min(source.length, target.length);
        for (int i = 0; i < bound; i++) {

            Pair<Entity, Entity> pair = Pair.of(source[i], target[i]);
            if (pair.getLeft().getType() == pair.getRight().getType()) {

                copyEntityData(pair.getLeft(), pair.getRight());
            } else {

                successfulForAll = false;
            }
        }

        return successfulForAll;
    }

    private static void copyEntityData(Entity source, @Nonnull Entity target) {

        if (source != null) {

            target.ticksExisted = source.ticksExisted;
            copyRotationAngles(source, target);
            if (source instanceof LivingEntity && target instanceof LivingEntity) {

                LivingEntity livingSource = (LivingEntity) source;
                LivingEntity livingTarget = (LivingEntity) target;
                livingTarget.prevLimbSwingAmount = livingSource.prevLimbSwingAmount;
                livingTarget.limbSwingAmount = livingSource.limbSwingAmount;
                livingTarget.limbSwing = livingSource.limbSwing;
                livingTarget.hurtTime = livingSource.hurtTime;
            }
        }
    }

    private static void copyRotationAngles(Entity source, Entity target) {
        target.prevRotationYaw = source.prevRotationYaw;
        target.prevRotationPitch = source.prevRotationPitch;
        target.rotationYaw = source.rotationYaw;
        target.rotationPitch = source.rotationPitch;
        if (source instanceof LivingEntity && target instanceof LivingEntity) {
            LivingEntity livingSource = (LivingEntity) source;
            LivingEntity livingTarget = (LivingEntity) target;
            livingTarget.prevRenderYawOffset = livingSource.prevRenderYawOffset;
            livingTarget.prevRotationYawHead = livingSource.prevRotationYawHead;
            livingTarget.renderYawOffset = livingSource.renderYawOffset;
            livingTarget.rotationYawHead = livingSource.rotationYawHead;
        }
    }

    private static void drawEntityOnScreen(PoseStack matrixstack, double posX, double posY, double posZ, float partialTicks, Entity entity, BiConsumer<IRenderTypeBuffer, Integer> renderName, Runnable invalidate) {

        matrixstack.pushPose();
        matrixstack.translate(posX, posY, posZ);
        EntityRendererManager rendererManager = Minecraft.getInstance().getRenderManager();
        rendererManager.setRenderShadow(false);
        IRenderTypeBuffer.Impl irendertypebuffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        RenderSystem.runAsFancy(() -> {
            Consumer<Entity> render = safeEntity -> rendererManager.renderEntityStatic(safeEntity, 0.0, 0.0, 0.0, 0.0F, partialTicks, matrixstack, irendertypebuffer, 15728880);
            Consumer<Entity> orElse = safeEntity -> {
                MenuEntityBlacklist.addToBlacklist(safeEntity.getType());
                invalidate.run();
            };
            PuzzlesUtil.runOrElse(entity, render, orElse);
            renderName.accept(irendertypebuffer, 15728880);
        });

        irendertypebuffer.finish();
        rendererManager.setRenderShadow(true);
        matrixstack.popPose();
    }

    private static void renderName(PoseStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, Entity entityIn, float renderHeight) {
        Component displayNameIn = entityIn.getDisplayName();
        float renderOffset = "deadmau5".equals(displayNameIn.getString()) ? -10 : 0;
        matrixStackIn.pushPose();
        matrixStackIn.translate(0.0, renderHeight, 0.0);
        matrixStackIn.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix4f = matrixStackIn.getLast().getMatrix();

        float backgroundOpacity = Minecraft.getInstance().gameSettings.getTextBackgroundOpacity(0.25F);
        int alpha = (int) (backgroundOpacity * 255.0F) << 24;
        FontRenderer fontrenderer = Minecraft.getInstance().fontRenderer;
        int textWidth = -fontrenderer.getStringPropertyWidth(displayNameIn) / 2;
        fontrenderer.func_243247_a(displayNameIn, textWidth, renderOffset, 553648127, false, matrix4f, bufferIn, true, alpha, packedLightIn);
        fontrenderer.func_243247_a(displayNameIn, textWidth, renderOffset, -1, false, matrix4f, bufferIn, false, 0, packedLightIn);
        matrixStackIn.popPose();
    }

}
