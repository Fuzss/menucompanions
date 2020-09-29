package com.fuzs.menucompanions.client.handler;

import com.fuzs.menucompanions.MenuCompanions;
import com.fuzs.menucompanions.client.util.EntityMenuContainer;
import com.fuzs.menucompanions.client.util.EntityMenuEntry;
import com.fuzs.menucompanions.client.util.MenuSide;
import com.fuzs.menucompanions.client.world.MenuClientWorld;
import com.fuzs.menucompanions.config.ConfigManager;
import com.fuzs.menucompanions.config.EntryCollectionBuilder;
import com.fuzs.menucompanions.config.JSONConfigUtil;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;

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
    private boolean playSounds;
    private static Set<EntityType<?>> blacklist;
    private static ForgeConfigSpec.ConfigValue<List<String>> blacklistSpec;

    private MenuClientWorld renderWorld;
    private final EntityMenuContainer[] sides = new EntityMenuContainer[2];
    private final int size = 60;

    public void setup(ForgeConfigSpec.Builder builder) {

        this.addListener(this::onGuiInit);
        this.addListener(this::onGuiOpen);
        this.addListener(this::onDrawScreen);
        this.addListener(this::onClientTick);
        this.addListener(this::onRenderNameplate);
        this.setupConfig(builder);
    }

    private <T extends Event> void addListener(Consumer<T> consumer) {

        this.events.add(consumer);
    }

    public void load() {

        this.events.forEach(MinecraftForge.EVENT_BUS::addListener);
    }

    private void setupConfig(ForgeConfigSpec.Builder builder) {

        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("When to show reload button on main menu. By default requires the control key to be pressed.").defineEnum("Reload Button", ReloadMode.CONTROL), v -> this.reloadMode = v);
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Which side entities can be shown at.").defineEnum("Entity Side", MenuSide.BOTH), v -> {

            this.menuSide = v;
            this.setEnabled();
        });
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Offset on x-axis from original position on left side.").defineInRange("Left X-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.offsets[0] = v);
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Offset on y-axis from original position on left side.").defineInRange("Left Y-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.offsets[1] = v);
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Offset on x-axis from original position on right side.").defineInRange("Right X-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.offsets[2] = v);
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Offset on y-axis from original position on right side.").defineInRange("Right Y-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.offsets[3] = v);
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Play ambient sounds when clicking on menu mobs.").define("Play Sounds", true), v -> this.playSounds = v);
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Volume of ambient sounds.").defineInRange("Sound Volume", 0.5, 0.0, 1.0), v -> {

            if (this.renderWorld != null) {

                this.renderWorld.setSoundVolume(v.floatValue());
            }
        });
        blacklistSpec = builder.comment("Blacklist for excluding entities. Entries will be added automatically when problematic entities are detected.").define("Blacklist", Lists.newArrayList("minecraft:ender_dragon", "minecraft:minecart", "minecraft:furnace_minecart", "minecraft:chest_minecart", "minecraft:spawner_minecart", "minecraft:hopper_minecart", "minecraft:command_block_minecart", "minecraft:tnt_minecart", "minecraft:evoker_fangs", "minecraft:falling_block", "minecraft:area_effect_cloud", "minecraft:item", "minecraft:fishing_bobber"));
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, blacklistSpec, v -> blacklist = new EntryCollectionBuilder<>(ForgeRegistries.ENTITIES, MenuCompanions.LOGGER).buildEntrySet(v));
    }

    public void createSides() {

        try {

            GameProfile profileIn = this.mc.getSession().getProfile();
            @SuppressWarnings("ConstantConditions")
            ClientPlayNetHandler clientPlayNetHandler = new ClientPlayNetHandler(this.mc, null, null, profileIn);
            ClientWorld.ClientWorldInfo worldInfo = new ClientWorld.ClientWorldInfo(Difficulty.HARD, false, false);
            DimensionType dimensionType = DynamicRegistries.func_239770_b_().func_230520_a_().func_243576_d(DimensionType.THE_NETHER);
            this.renderWorld = new MenuClientWorld(clientPlayNetHandler, worldInfo, World.THE_NETHER, dimensionType, this.mc::getProfiler, this.mc.worldRenderer);
        } catch (Exception e) {

            MenuCompanions.LOGGER.error("Unable to create rendering world: {}", e.getMessage());
            this.events.forEach(MinecraftForge.EVENT_BUS::unregister);

            return;
        }

        this.sides[0] = new EntityMenuContainer(this.mc, this.renderWorld);
        this.sides[1] = new EntityMenuContainer(this.mc, this.renderWorld);
        this.setEnabled();
    }

    private void setEnabled() {

        // is also called when the config reloads, that might happen at some point before everything is create here, who knows
        Optional.ofNullable(this.sides[0]).ifPresent(container -> container.setEnabled(this.menuSide != MenuSide.RIGHT));
        Optional.ofNullable(this.sides[1]).ifPresent(container -> container.setEnabled(this.menuSide != MenuSide.LEFT));
    }

    private void onGuiInit(final GuiScreenEvent.InitGuiEvent.Post evt) {

        if (evt.getGui() instanceof MainMenuScreen) {

            evt.addWidget(new ImageButton(evt.getGui().width / 2 + 104 + 24, evt.getGui().height / 4 + 48 + 72 + 12, 20, 20, 0, 0, 20, RELOAD_TEXTURES, 32, 64, button -> {

                JSONConfigUtil.load(MenuCompanions.JSON_CONFIG_NAME, MenuCompanions.MODID, MenuEntityProvider::serialize, MenuEntityProvider::deserialize);
                MenuCompanions.LOGGER.info("Reloaded config file at {}", MenuCompanions.JSON_CONFIG_NAME);

                this.setMenuSide(MenuSide.LEFT);
                this.setMenuSide(MenuSide.RIGHT);
            }, new TranslationTextComponent("narrator.button.reload")) {

                @Override
                public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {

                    this.visible = MenuEntityHandler.this.reloadMode == ReloadMode.CONTROL && Screen.hasControlDown() || MenuEntityHandler.this.reloadMode == ReloadMode.ALWAYS;
                    super.render(matrixStack, mouseX, mouseY, partialTicks);
                }

            });

            evt.addWidget(new Button(0,  0, this.size, 3 * 24 + 8, StringTextComponent.EMPTY, button -> this.sides[0].playAmbientSound()) {

                @Override
                public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {

                    this.active = MenuEntityHandler.this.playSounds;
                    this.x = (evt.getGui().width / 2 - 96) / 2 - MenuEntityHandler.this.size / 2 + MenuEntityHandler.this.offsets[0];
                    this.y = evt.getGui().height / 4 + 48 - MenuEntityHandler.this.offsets[1];
                }

                @Override
                public void playDownSound(@Nonnull SoundHandler handler) {

                }

            });

            evt.addWidget(new Button(0,  0, this.size, 3 * 24 + 8, StringTextComponent.EMPTY, button -> this.sides[1].playAmbientSound()) {

                @Override
                public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {

                    this.active = MenuEntityHandler.this.playSounds;
                    this.x = evt.getGui().width - ((evt.getGui().width / 2 - 96) / 2 + MenuEntityHandler.this.offsets[2] + MenuEntityHandler.this.size / 2);
                    this.y = evt.getGui().height / 4 + 48 - MenuEntityHandler.this.offsets[3];
                }

                @Override
                public void playDownSound(@Nonnull SoundHandler handler) {

                }

            });
        }
    }

    private void onGuiOpen(final GuiOpenEvent evt) {

        if (evt.getGui() instanceof MainMenuScreen) {

            this.setMenuSide(MenuSide.LEFT);
            this.setMenuSide(MenuSide.RIGHT);
        }
    }

    private void onDrawScreen(final GuiScreenEvent.DrawScreenEvent.Post evt) {

        if (evt.getGui() instanceof MainMenuScreen) {

            for (int i = 0; i < this.sides.length; i++) {

                EntityMenuContainer container = this.sides[i];
                int xOffset = (evt.getGui().width / 2 - 96) / 2 + this.offsets[i * 2];
                int posX = i == 0 ? xOffset : evt.getGui().width - xOffset;
                int posY = evt.getGui().height / 4 + 116 - this.offsets[i * 2 + 1];
                container.render(posX, posY, this.size / 2.0F, -evt.getMouseX(), -evt.getMouseY(), evt.getRenderPartialTicks());
            }

            this.runCleanup();
        }
    }

    private void onClientTick(final TickEvent.ClientTickEvent evt) {

        if (evt.phase != TickEvent.Phase.END && this.mc.currentScreen instanceof MainMenuScreen) {

            Stream.of(this.sides).forEach(EntityMenuContainer::tick);
            this.runCleanup();
        }
    }

    private void onRenderNameplate(final RenderNameplateEvent evt) {

        if (this.mc.currentScreen instanceof MainMenuScreen) {

            // we'll handle rendering nameplates ourselves (doesn't work for living entities anyways like that)
            evt.setResult(Event.Result.DENY);
        }
    }

    private void setMenuSide(@Nonnull MenuSide side) {

        if (this.menuSide == side.inverse()) {

            return;
        }

        // limit max number of attempts
        for (int i = 0; i < 5; i++) {

            EntityMenuEntry entry = MenuEntityProvider.getRandomEntry(side);
            // entries for side are empty
            if (entry == null) {

                return;
            }

            Entity entity = entry.create(this.renderWorld);
            if (entity != null) {

                this.sides[side.ordinal()].createEntity(entity, entry, side);
                return;
            }
        }
    }

    private void runCleanup() {

        for (int i = 0; i < this.sides.length; i++) {

            if (this.sides[i].isInvalid()) {

                this.setMenuSide(MenuSide.values()[i]);
            }
        }
    }

    public static boolean runOrElse(Entity entity, Consumer<Entity> action, Consumer<Entity> orElse) {

        try {

            action.accept(entity);
        } catch (Exception e) {

            MenuCompanions.LOGGER.error("Unable to handle Entity {}", entity.getDisplayName().getString(), e);
            orElse.accept(entity);

            return false;
        }

        return true;
    }

    public static boolean isAllowed(EntityType<?> type) {

        return blacklist == null || !blacklist.contains(type);
    }

    public static void addToBlacklist(String type) {

        ResourceLocation key = ResourceLocation.tryCreate(type);
        if (key == null || !ForgeRegistries.ENTITIES.containsKey(key)) {

            return;
        }

        addToBlacklist(Objects.requireNonNull(ForgeRegistries.ENTITIES.getValue(key)));
    }

    public static void addToBlacklist(EntityType<?> type) {

        if (blacklistSpec == null) {

            return;
        }

        if (isAllowed(type)) {

            blacklistSpec.set(Stream.of(blacklistSpec.get(),
                    Collections.singleton(Objects.requireNonNull(ForgeRegistries.ENTITIES.getKey(type)).toString()))
                    .flatMap(Collection::stream).collect(Collectors.toList()));
        }
    }

    @SuppressWarnings("unused")
    private enum ReloadMode {

        NEVER, CONTROL, ALWAYS
    }

}
