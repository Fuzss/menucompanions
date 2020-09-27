package com.fuzs.menucompanions.client.handler;

import com.fuzs.menucompanions.MenuCompanions;
import com.fuzs.menucompanions.client.util.DrawEntityUtil;
import com.fuzs.menucompanions.client.util.EntityMenuEntry;
import com.fuzs.menucompanions.client.util.MenuSide;
import com.fuzs.menucompanions.config.ConfigManager;
import com.fuzs.menucompanions.config.EntryCollectionBuilder;
import com.fuzs.menucompanions.config.JSONConfigUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.RemoteClientPlayerEntity;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.network.play.server.SPlayerListItemPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DimensionType;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MenuEntityHandler {

    private static final ResourceLocation RELOAD_TEXTURES = new ResourceLocation(MenuCompanions.MODID, "textures/gui/reload.png");

    private final Minecraft mc = Minecraft.getInstance();
    private final List<Consumer<? extends Event>> events = Lists.newArrayList();

    private ReloadMode reloadMode;
    private MenuSide menuSide;
    private final int[] offsets = new int[4];
    private static Set<EntityType<?>> blacklist;
    private static ForgeConfigSpec.ConfigValue<List<String>> blacklistSpec;

    private ClientWorld renderWorld;
    private final EnumMap<MenuSide, Pair<Entity, Vector3f>> renderEntities = Maps.newEnumMap(MenuSide.class);
    private final Set<Entity> renderNameplates = Sets.newHashSet();

    public void setup(ForgeConfigSpec.Builder builder) {

        this.addListener(this::onGuiInit);
        this.addListener(this::onGuiOpen);
        this.addListener(this::onDrawScreen);
        this.addListener(this::onClientTick);
        this.addListener(this::onRenderNameplate);
        this.setupConfig(builder);
    }

    public void load() {

        this.events.forEach(MinecraftForge.EVENT_BUS::addListener);
    }

    private <T extends Event> void addListener(Consumer<T> consumer) {

        this.events.add(consumer);
    }

    private void unregister() {

        this.events.forEach(MinecraftForge.EVENT_BUS::unregister);
    }

    private void setupConfig(ForgeConfigSpec.Builder builder) {

        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("When to show reload button on main menu. Requires a the control key  to be pressed by default.").defineEnum("Reload Button", ReloadMode.CONTROL), v -> this.reloadMode = v);
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Which side entities can be shown at.").defineEnum("Entity Side", MenuSide.BOTH), v -> {

            this.menuSide = v;
            if (this.mc.currentScreen instanceof MainMenuScreen) {

                this.populateSides();
            }
        });
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Offset on x-axis from original position on left side.").defineInRange("Left X-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.offsets[0] = v);
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Offset on y-axis from original position on left side.").defineInRange("Left Y-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.offsets[1] = v);
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Offset on x-axis from original position on right side.").defineInRange("Right X-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.offsets[2] = -v);
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Offset on y-axis from original position on right side.").defineInRange("Right Y-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.offsets[3] = v);
        blacklistSpec = builder.comment("Blacklist for excluding entities. Entries will be added automatically when problematic entities are detected.").define("Blacklist", Lists.newArrayList("minecraft:ender_dragon"));
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, blacklistSpec, v -> blacklist = new EntryCollectionBuilder<>(ForgeRegistries.ENTITIES, MenuCompanions.LOGGER).buildEntrySet(v));
    }

    private Optional<ClientWorld> getRenderWorld() {

        if (this.renderWorld == null) {

            this.renderWorld = this.createRenderWorld();
        }

        return Optional.ofNullable(this.renderWorld);
    }

    private ClientWorld createRenderWorld() {

        try {

            GameProfile profileIn = this.mc.getSession().getProfile();
            @SuppressWarnings("ConstantConditions")
            ClientPlayNetHandler clientPlayNetHandler = new ClientPlayNetHandler(this.mc, null, null, profileIn);
            ClientWorld.ClientWorldInfo worldInfo = new ClientWorld.ClientWorldInfo(Difficulty.HARD, false, false);
            DimensionType dimensionType = DynamicRegistries.func_239770_b_().func_230520_a_().func_243576_d(DimensionType.THE_NETHER);
            return new ClientWorld(clientPlayNetHandler, worldInfo, World.THE_NETHER, dimensionType, 3, this.mc::getProfiler, this.mc.worldRenderer, false, 0) {

                @Override
                public void playSound(double x, double y, double z, @Nonnull SoundEvent soundIn, @Nonnull SoundCategory category, float volume, float pitch, boolean distanceDelay) {

                    // prevent mob sounds from playing for ender dragon and blaze
                }

            };
        } catch (Exception e) {

            MenuCompanions.LOGGER.error("Unable to create rendering world: {}", e.getMessage());
        }

        this.unregister();
        return null;
    }

    private void onGuiInit(final GuiScreenEvent.InitGuiEvent.Post evt) {

        if (evt.getGui() instanceof MainMenuScreen) {

            evt.addWidget(new ImageButton(evt.getGui().width / 2 + 104 + 24, evt.getGui().height / 4 + 48 + 72 + 12, 20, 20, 0, 0, 20, RELOAD_TEXTURES, 32, 64, (p_213088_1_) -> {

                JSONConfigUtil.load(MenuCompanions.JSON_CONFIG_NAME, MenuCompanions.MODID, MenuEntityProvider::serialize, MenuEntityProvider::deserialize);
                MenuCompanions.LOGGER.info("Reloaded config file at {}", MenuCompanions.JSON_CONFIG_NAME);

                this.populateSides();
            }, new TranslationTextComponent("narrator.button.reload")) {

                @Override
                public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {

                    this.visible = MenuEntityHandler.this.reloadMode == ReloadMode.CONTROL && Screen.hasControlDown() || MenuEntityHandler.this.reloadMode == ReloadMode.ALWAYS;
                    super.render(matrixStack, mouseX, mouseY, partialTicks);
                }

            });
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void onGuiOpen(final GuiOpenEvent evt) {

        if (evt.getGui() instanceof MainMenuScreen) {

            // allows fire to be rendered on mobs as it requires an active render info object
            this.mc.getRenderManager().cacheActiveRenderInfo(this.renderWorld, this.mc.gameRenderer.getActiveRenderInfo(), null);
            ObfuscationReflectionHelper.setPrivateValue(ActiveRenderInfo.class, this.mc.gameRenderer.getActiveRenderInfo(), true, "valid");
            this.mc.particles.clearEffects(this.getRenderWorld().get());
            this.populateSides();
        }
    }

    private void populateSides() {

        this.renderEntities.clear();
        this.renderNameplates.clear();
        if (this.menuSide != MenuSide.RIGHT) {

            this.setMenuSide(MenuSide.LEFT);
        }

        if (this.menuSide != MenuSide.LEFT) {

            this.setMenuSide(MenuSide.RIGHT);
        }
    }

    private void onDrawScreen(final GuiScreenEvent.DrawScreenEvent.Post evt) {

        if (evt.getGui() instanceof MainMenuScreen) {

            this.renderEntities.forEach((key, value) -> {

                Entity entity = value.getLeft();
                int scale = (int) (value.getRight().getZ() * 30);
                int posX = (int) (evt.getGui().width * (key == MenuSide.LEFT ? 1.0F : 6.0F) / 7.0F) + this.getXOffset(key) + (int) value.getRight().getX();
                int posY = evt.getGui().height / 4 + 5 * 24 + this.getYOffset(key) + (int) value.getRight().getY();

                RenderSystem.translatef(posX, posY - entity.getHeight() * scale, 0.0F);
                RenderSystem.scalef(30.F, 30.F, 30.0F);
                DrawEntityUtil.renderParticles(this.mc.particles, evt.getMatrixStack(), this.mc.gameRenderer.getLightTexture(), this.mc.gameRenderer.getActiveRenderInfo(), this.mc.textureManager, evt.getRenderPartialTicks());

                RenderSystem.scalef(1 / 30.F, 1 / 30.F, 1 / 30.0F);
                RenderSystem.translatef(-posX, -posY + entity.getHeight() * scale, 0.0F);
                final int finalPosY = posY;
                this.safeRunForSide(key, entity, entity1 ->
                        DrawEntityUtil.drawEntityOnScreen(posX, finalPosY, scale, -evt.getMouseX() + posX, -evt.getMouseY() + finalPosY - 50, entity1, evt.getRenderPartialTicks()));

                for (Entity passenger : entity.getRecursivePassengers()) {

                    posY -= scale * (passenger.getPosY() - Objects.requireNonNull(passenger.getRidingEntity()).getPosY());
                    final int finalPosY1 = posY;
                    this.safeRunForSide(key, passenger, passenger1 ->
                            DrawEntityUtil.drawEntityOnScreen(posX, finalPosY1, scale, -evt.getMouseX() + posX, -evt.getMouseY() + finalPosY1 - 50, passenger1, evt.getRenderPartialTicks()));
                }
            });
        }
    }

    private void onClientTick(final TickEvent.ClientTickEvent evt) {

        if (evt.phase != TickEvent.Phase.END && this.mc.currentScreen instanceof MainMenuScreen) {

            this.mc.particles.tick();
            this.renderEntities.forEach((key, value) -> {

                Entity entity = value.getLeft();
                // ensures animations run properly since ageInTicks relies on it
                this.safeRunForSide(key, entity, entity1 -> {

                    entity1.ticksExisted++;
                    if (entity1 instanceof LivingEntity)
                    ((LivingEntity) entity1).livingTick();
                });
                if (entity.isBeingRidden()) {

                    for (Entity passenger : entity.getRecursivePassengers()) {

                        this.safeRunForSide(key, passenger, passenger1 -> {

                            passenger1.ticksExisted++;
                            if (passenger1 instanceof LivingEntity)
                                ((LivingEntity) passenger1).livingTick();
                            // some mobs like chicken shift their rider around on x and z axis depending on their facing direction
                            Objects.requireNonNull(passenger1.getRidingEntity()).updatePassenger(passenger1);
                        });
                    }
                }
            });
        }
    }

    private void onRenderNameplate(final RenderNameplateEvent evt) {

        if (this.mc.currentScreen instanceof MainMenuScreen) {

            // rendering nameplates uses client player for a proximity check which is still null here
            evt.setResult(Event.Result.DENY);
            if (this.renderNameplates.contains(evt.getEntity())) {

                this.renderName(evt.getEntity(), evt.getContent(), evt.getMatrixStack(), evt.getRenderTypeBuffer(), evt.getPackedLight(), evt.getEntityRenderer().getRenderManager());
            }
        }
    }

    protected void renderName(Entity entityIn, ITextComponent displayNameIn, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, EntityRendererManager renderManager) {

        float renderHeight = entityIn.getHeight() + 0.5F;
        float renderOffset = "deadmau5".equals(displayNameIn.getString()) ? -10 : 0;

        matrixStackIn.push();
        matrixStackIn.translate(0.0D, renderHeight, 0.0D);
        matrixStackIn.rotate(renderManager.getCameraOrientation());
        matrixStackIn.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix4f = matrixStackIn.getLast().getMatrix();

        float backgroundOpacity = Minecraft.getInstance().gameSettings.getTextBackgroundOpacity(0.25F);
        int alpha = (int)(backgroundOpacity * 255.0F) << 24;
        FontRenderer fontrenderer = renderManager.getFontRenderer();
        int textWidth = -fontrenderer.func_238414_a_(displayNameIn) / 2;
        fontrenderer.func_243247_a(displayNameIn, textWidth, renderOffset, 553648127, false, matrix4f, bufferIn, true, alpha, packedLightIn);
        fontrenderer.func_243247_a(displayNameIn, textWidth, renderOffset, -1, false, matrix4f, bufferIn, false, 0, packedLightIn);

        matrixStackIn.pop();
    }

    private void setMenuSide(MenuSide side) {

        for (int i = 0; i < 10; i++) {

            EntityMenuEntry entry = MenuEntityProvider.getRandomEntry(side);
            if (entry == null || !this.getRenderWorld().isPresent()) {

                // entries for side are empty, world should always be present
                return;
            }

            Entity entity = entry.create(this.getRenderWorld().get());
            if (entity != null) {

                Vector3f vec = entry.getRenderVec(entity);
                this.renderEntities.put(side, Pair.of(entity, vec));
                this.setInitialAngles(entity, side, vec.getX(), vec.getY());
                if (entry.showNameplate()) {

                    entity.getSelfAndPassengers().forEach(this.renderNameplates::add);
                }

                break;
            }
        }
    }

    private void setRenderEntityPlayer() {

        LivingEntity renderEntity = new RemoteClientPlayerEntity(this.getRenderWorld().get(), this.mc.getSession().getProfile()) {

            private NetworkPlayerInfo playerInfo;

            @Override
            protected NetworkPlayerInfo getPlayerInfo() {

                if (this.playerInfo == null) {

                     this.playerInfo = new NetworkPlayerInfo(new SPlayerListItemPacket().new AddPlayerData(MenuEntityHandler.this.mc.getSession().getProfile(), 1, GameType.SURVIVAL, null));
                }

                return this.playerInfo;
            }

            @Override
            public boolean isSpectator() {

                return false;
            }

            @Override
            public boolean isCreative() {

                return false;
            }

            @Override
            public boolean isWearing(@Nonnull PlayerModelPart part) {

                return MenuEntityHandler.this.mc.gameSettings.getModelParts().contains(part);
            }

        };
    }

    private void safeRunForSide(MenuSide side, Entity entity, Consumer<Entity> action) {

        try {

            action.accept(entity);
        } catch (Exception e) {

            MenuCompanions.LOGGER.error("Unable to handle Entity {}", entity.getDisplayName().getString(), e);
            addToBlacklist(Objects.requireNonNull(ForgeRegistries.ENTITIES.getKey(entity.getType())).toString());
            this.setMenuSide(side);
        }
    }

    private int getXOffset(MenuSide side) {

        return this.offsets[side.getOffsetPos() * 2];
    }

    private int getYOffset(MenuSide side) {

        return this.offsets[side.getOffsetPos() * 2 + 1];
    }

    private void setInitialAngles(Entity entity, MenuSide side, float xOffset, float yOffset) {

        Minecraft mc = Minecraft.getInstance();
        int mouseX = (int) (mc.mouseHelper.getMouseX() * (double) mc.getMainWindow().getScaledWidth() / (double) mc.getMainWindow().getWidth());
        int mouseY = (int) (mc.mouseHelper.getMouseY() * (double) mc.getMainWindow().getScaledHeight() / (double) mc.getMainWindow().getHeight());
        mouseX += (int) (mc.getMainWindow().getScaledWidth() * (side == MenuSide.LEFT ? 1.0F : 6.0F) / 7.0F) + this.getXOffset(side) + (int) xOffset;
        mouseY += mc.getMainWindow().getScaledHeight() / 4 + 5 * 24 + this.getYOffset(side) + (int) yOffset;
        float rotX = (float) Math.atan(mouseX / 40.0F);
        float rotY = (float) Math.atan(mouseY / 40.0F);

        entity.rotationYaw = 180.0F + rotX * 40.0F;
        entity.rotationPitch = -rotY * 20.0F;
        entity.prevRotationYaw = entity.rotationYaw;
        entity.prevRotationPitch = entity.rotationPitch;

        if (entity instanceof LivingEntity) {

            LivingEntity livingEntity = (LivingEntity) entity;
            livingEntity.renderYawOffset = 180.0F + rotX * 20.0F;
            livingEntity.rotationYawHead = entity.rotationYaw;
            livingEntity.prevRenderYawOffset = livingEntity.renderYawOffset;
            livingEntity.prevRotationYawHead = livingEntity.rotationYawHead;
        }
    }

    public static boolean isAllowed(EntityType<?> type) {

        return blacklist == null || !blacklist.contains(type);
    }

    public static void addToBlacklist(String type) {

        ResourceLocation key = ResourceLocation.tryCreate(type);
        if (blacklistSpec == null || key == null || !ForgeRegistries.ENTITIES.containsKey(key)) {

            return;
        }

        if (isAllowed(ForgeRegistries.ENTITIES.getValue(key))) {

            List<String> newBlacklist = Stream.of(blacklistSpec.get(), Collections.singleton(key.toString()))
                    .flatMap(Collection::stream).collect(Collectors.toList());
            blacklistSpec.set(newBlacklist);
        }
    }

    @SuppressWarnings("unused")
    private enum ReloadMode {

        NEVER, CONTROL, ALWAYS
    }

}
