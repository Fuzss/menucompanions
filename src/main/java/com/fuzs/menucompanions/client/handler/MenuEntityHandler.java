package com.fuzs.menucompanions.client.handler;

import com.fuzs.menucompanions.MenuCompanions;
import com.fuzs.menucompanions.client.util.DrawEntityUtil;
import com.fuzs.menucompanions.client.util.MenuEntityEntry;
import com.fuzs.menucompanions.config.ConfigManager;
import com.fuzs.menucompanions.config.JSONConfigUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.RemoteClientPlayerEntity;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.network.play.server.SPlayerListItemPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
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
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MenuEntityHandler {

    private static final ResourceLocation RELOAD_TEXTURES = new ResourceLocation(MenuCompanions.MODID, "textures/gui/reload.png");

    private final Minecraft mc = Minecraft.getInstance();
    private final List<Consumer<? extends Event>> events = Lists.newArrayList();
    private final int maxAttempts = 10;

    private ReloadMode reloadMode;
    private MenuEntityEntry.MenuSide menuSide;
    private ForgeConfigSpec.ConfigValue<List<String>> blacklist;

    private ClientWorld renderWorld;
    private final EnumMap<MenuEntityEntry.MenuSide, Pair<Entity, Float>> renderEntities = Maps.newEnumMap(MenuEntityEntry.MenuSide.class);
    private final Map<Entity, Boolean> renderNameplates = Maps.newHashMap();

    private LivingEntity renderEntity;

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
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Which side entities can be shown at.").defineEnum("Entity Side", MenuEntityEntry.MenuSide.BOTH), v -> {

            this.menuSide = v;
            this.populateSides();
        });
        this.blacklist = builder.comment("Blacklist for excluding entities. Entries will be added automatically when problematic entities are detected.").define("Blacklist", Lists.newArrayList("hopp", "hupp", "hipp"));
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
            DimensionType dimensionType = DynamicRegistries.func_239770_b_().func_230520_a_().func_243576_d(DimensionType.OVERWORLD);
            return new ClientWorld(clientPlayNetHandler, worldInfo, World.OVERWORLD, dimensionType, 3, this.mc::getProfiler, this.mc.worldRenderer, false, 0);
        } catch (Exception e) {

            MenuCompanions.LOGGER.error("Unable to create rendering world: {}", e.getMessage());
        }

        this.unregister();
        return null;
    }

    private void onGuiInit(final GuiScreenEvent.InitGuiEvent.Post evt) {

        if (evt.getGui() instanceof MainMenuScreen) {

            // TODO
            this.blacklist.set(Lists.newArrayList("hi", "ho", "ha"));

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

    private void onGuiOpen(final GuiOpenEvent evt) {

        if (evt.getGui() instanceof MainMenuScreen) {

            this.populateSides();
        }
    }

    private void populateSides() {

        this.renderNameplates.clear();
        if (this.menuSide != MenuEntityEntry.MenuSide.RIGHT) {

            this.setMenuSide(MenuEntityEntry.MenuSide.LEFT);
        }

        if (this.menuSide != MenuEntityEntry.MenuSide.LEFT) {

            this.setMenuSide(MenuEntityEntry.MenuSide.RIGHT);
        }
    }

    private void onDrawScreen(final GuiScreenEvent.DrawScreenEvent.Post evt) {

        if (evt.getGui() instanceof MainMenuScreen) {

            this.runForSides((side, pair) -> {

                Entity entity = pair.getLeft();
                int scale = (int) (pair.getRight() * 30);
                int posX = (int) (evt.getGui().width * (side == MenuEntityEntry.MenuSide.LEFT ? 1.0F : 6.0F) / 7.0F);
                int posY = evt.getGui().height / 4 + 5 * 24;
                DrawEntityUtil.drawEntityOnScreen(posX, posY, scale, -evt.getMouseX() + posX, -evt.getMouseY() + posY - 50, entity);

                for (Entity passenger : entity.getRecursivePassengers()) {

                    posY -= scale * (passenger.getPosY() - passenger.getRidingEntity().getPosY());
                    DrawEntityUtil.drawEntityOnScreen(posX, posY, scale, -evt.getMouseX() + posX, -evt.getMouseY() + posY - 50, passenger);
                }
            });
        }
    }

    private void onClientTick(final TickEvent.ClientTickEvent evt) {

        if (evt.phase != TickEvent.Phase.END && this.mc.currentScreen instanceof MainMenuScreen) {

            this.runForSides((side, pair) -> {

                Entity entity = pair.getLeft();
                // ensures animations run properly since ageInTicks relies on it
                entity.ticksExisted++;
                if (entity.isBeingRidden()) {

                    for (Entity passenger : entity.getRecursivePassengers()) {

                        passenger.ticksExisted++;
                        // some mobs like chicken shift their rider around on x and z axis depending on their facing direction
                        passenger.getRidingEntity().updatePassenger(passenger);
                    }
                }
            });
        }
    }

    private void onRenderNameplate(final RenderNameplateEvent evt) {

        if (this.renderNameplates.containsKey(evt.getEntity())) {

            evt.setResult(Event.Result.DENY);
            if (this.renderNameplates.get(evt.getEntity())) {

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

    private void setMenuSide(MenuEntityEntry.MenuSide side) {

        for (int i = 0; i < this.maxAttempts; i++) {

            MenuEntityEntry entry = MenuEntityProvider.getRandomEntry(side);
            try {

                if (entry != null && this.getRenderWorld().isPresent()) {

                    Entity entity = entry.create(this.getRenderWorld().get());
                    if (entity != null) {

                        this.renderEntities.put(side, Pair.of(entity, entry.getScale(entity)));
                        entity.getSelfAndPassengers().forEach(passenger -> this.renderNameplates.put(passenger, entry.showNameplate()));
                        return;
                    } else {

                        MenuCompanions.LOGGER.warn("Unable to create entity: {}", NullPointerException.class.getSimpleName());
                    }
                }
            } catch (Exception e) {

                MenuCompanions.LOGGER.warn("Unable to create entity: {}", e.getMessage());
            }
        }

        this.renderEntities.put(side, null);
    }

    private void setRenderEntityPlayer() {

        this.renderEntity = new RemoteClientPlayerEntity(this.getRenderWorld().get(), this.mc.getSession().getProfile()) {

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

    private void runForSides(BiConsumer<MenuEntityEntry.MenuSide, Pair<Entity, Float>> action) {

        this.renderEntities.forEach((key, value) -> {

            try {

                // might be null when side is disabled
                if (value != null) {

                    action.accept(key, value);
                }
            } catch (Exception e) {

                MenuCompanions.LOGGER.error("Unable to run for side \"{}\": {}", key, e.getMessage());
                this.setMenuSide(key);
            }
        });
    }

    private enum ReloadMode {

        NEVER, CONTROL, ALWAYS
    }

}
