package com.fuzs.menucompanions.client.handler;

import com.fuzs.menucompanions.MenuCompanions;
import com.fuzs.menucompanions.client.gui.EntityMenuContainer;
import com.fuzs.menucompanions.client.storage.EntityMenuEntry;
import com.fuzs.menucompanions.client.storage.MenuEntityProvider;
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
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDirection;
import net.minecraft.tags.TagRegistryManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MenuEntityHandler implements IEventHandler {

    private static final ResourceLocation RELOAD_TEXTURES = new ResourceLocation(MenuCompanions.MODID, "textures/gui/reload.png");

    private final Minecraft mc = Minecraft.getInstance();

    private ReloadMode reloadMode;
    private final int[] offsets = new int[6];
    private int size;
    private double volume;
    private boolean hurtPlayer;
    private static Set<EntityType<?>> blacklist;
    private static ForgeConfigSpec.ConfigValue<List<String>> blacklistSpec;

    private MenuClientWorld renderWorld;
    private final EntityMenuContainer[] sides = new EntityMenuContainer[2];
    private final Widget[] widgets = new Widget[2];

    public void setup(ForgeConfigSpec.Builder builder) {

        // will prevent other mods from hooking into the player renderer on the main menu
        this.addListener(this::onRenderPlayer1, EventPriority.HIGHEST);
        this.addListener(this::onRenderPlayer2, EventPriority.LOWEST, true);

        this.addListener(this::onGuiInit);
        this.addListener(this::onGuiOpen);
        this.addListener(this::onDrawScreen);
        this.addListener(this::onClientTick);
        this.addListener(this::onRenderNameplate, EventPriority.HIGHEST);

        this.setupConfig(builder);
    }

    public void load() {

        EVENTS.forEach(EventStorage::register);
    }

    private void setupConfig(ForgeConfigSpec.Builder builder) {

        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("When to show reload button on main menu. By default requires the control key to be pressed.").defineEnum("Reload Button", ReloadMode.RIGHT_CONTROL), v -> this.reloadMode = v);
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Reload button offset on x-axis from original position.").defineInRange("Button X-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.offsets[4] = v);
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Reload button offset on y-axis from original position.").defineInRange("Button Y-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.offsets[5] = v);
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Which side entities can be shown at.").defineEnum("Entity Side", MenuSide.BOTH), v -> {

            acceptIfPresent(this.sides[0], container -> container.setEnabled(v != MenuSide.RIGHT));
            acceptIfPresent(this.sides[1], container -> container.setEnabled(v != MenuSide.LEFT));
        });
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Size of menu companions.").defineInRange("Size", 60, 0, Integer.MAX_VALUE), v -> {

            for (Widget widget : this.widgets) {

                acceptIfPresent(widget, button -> {

                    button.x += this.size / 2;
                    button.x -= v / 2;
                    button.y += this.size * 4 / 3;
                    button.y -= v * 4 / 3;
                    button.setWidth(v);
                    button.setHeight(v * 4 / 3);
                });
            }

            this.size = v;
        });
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Offset on x-axis from original position on left side.").defineInRange("Left X-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> {

            acceptIfPresent(this.widgets[0], widget -> {

                widget.x -= this.offsets[0];
                widget.x += v;
            });

            this.offsets[0] = v;
        });
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Offset on y-axis from original position on left side.").defineInRange("Left Y-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> {

            acceptIfPresent(this.widgets[0], widget -> {

                widget.y += this.offsets[1];
                widget.y -= v;
            });

            this.offsets[1] = v;
        });
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Offset on x-axis from original position on right side.").defineInRange("Right X-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> {

            acceptIfPresent(this.widgets[1], widget -> {

                widget.x += this.offsets[2];
                widget.x -= v;
            });

            this.offsets[2] = v;
        });
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Offset on y-axis from original position on right side.").defineInRange("Right Y-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> {

            acceptIfPresent(this.widgets[1], widget -> {

                widget.y += this.offsets[3];
                widget.y -= v;
            });

            this.offsets[3] = v;
        });
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Play ambient sounds when clicking on menu mobs.").define("Play Sounds", true), v -> {

            acceptIfPresent(this.widgets[0], widget -> widget.active = v);
            acceptIfPresent(this.widgets[1], widget -> widget.active = v);
        });
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Volume of ambient sounds.").defineInRange("Sound Volume", 0.5, 0.0, 1.0), v -> this.volume = v);
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, builder.comment("Hurt player when clicked.").define("Hurt Player", true), v -> this.hurtPlayer = v);
        blacklistSpec = builder.comment("Blacklist for excluding entities. Entries will be added automatically when problematic entities are detected.").define("Blacklist", Lists.newArrayList("minecraft:ender_dragon", "minecraft:evoker_fangs", "minecraft:falling_block", "minecraft:area_effect_cloud", "minecraft:item", "minecraft:fishing_bobber"));
        ConfigManager.registerEntry(ModConfig.Type.CLIENT, blacklistSpec, v -> blacklist = new EntryCollectionBuilder<>(ForgeRegistries.ENTITIES, MenuCompanions.LOGGER).buildEntrySet(v));
    }

    public void createSides() {

        try {

            GameProfile profileIn = this.mc.getSession().getProfile();
            @SuppressWarnings("ConstantConditions")
            ClientPlayNetHandler clientPlayNetHandler = new ClientPlayNetHandler(this.mc, null,  new NetworkManager(PacketDirection.CLIENTBOUND), profileIn);
            ClientWorld.ClientWorldInfo worldInfo = new ClientWorld.ClientWorldInfo(Difficulty.HARD, false, false);
            DimensionType dimensionType = DynamicRegistries.func_239770_b_().func_230520_a_().func_243576_d(DimensionType.THE_NETHER);
            this.renderWorld = new MenuClientWorld(clientPlayNetHandler, worldInfo, World.THE_NETHER, dimensionType, this.mc::getProfiler, this.mc.worldRenderer);
        } catch (Exception e) {

            MenuCompanions.LOGGER.error("Unable to create rendering world: {}", e.getMessage());
            EVENTS.forEach(EventStorage::unregister);

            return;
        }

        // init tags as some entities such as piglins and minecarts depend on it
        TagRegistryManager.func_242191_a();
        this.sides[0] = new EntityMenuContainer(this.mc, this.renderWorld);
        this.sides[1] = new EntityMenuContainer(this.mc, this.renderWorld);
    }

    private void onRenderPlayer1(final RenderPlayerEvent.Pre evt) {

        if (this.mc.currentScreen instanceof MainMenuScreen) {

            evt.setCanceled(true);
        }
    }

    private void onRenderPlayer2(final RenderPlayerEvent.Pre evt) {

        if (this.mc.currentScreen instanceof MainMenuScreen) {

            evt.setCanceled(false);
        }
    }

    private void onGuiInit(final GuiScreenEvent.InitGuiEvent.Post evt) {

        if (evt.getGui() instanceof MainMenuScreen) {

            if (this.reloadMode != ReloadMode.NEVER) {

                this.addReloadButton(evt.getWidgetList(), evt::addWidget);
            }

            this.widgets[0] = new Button((evt.getGui().width / 2 - 96) / 2 - this.size / 2 + this.offsets[0],
                    evt.getGui().height / 4 + 48 + 80 - this.size * 4 / 3 - this.offsets[1], this.size, this.size * 4 / 3, StringTextComponent.EMPTY, button -> {}) {

                @Override
                public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {

                }

                @Override
                public void playDownSound(@Nonnull SoundHandler handler) {

                    MenuEntityHandler.this.sides[0].playLivingSound(handler, (float) MenuEntityHandler.this.volume, MenuEntityHandler.this.hurtPlayer);
                }

            };

            this.widgets[1] = new Button(evt.getGui().width - ((evt.getGui().width / 2 - 96) / 2 + this.size / 2 + this.offsets[2]),
                    evt.getGui().height / 4 + 48 + 80 - this.size * 4 / 3 - this.offsets[3], this.size, this.size * 4 / 3, StringTextComponent.EMPTY, button -> {}) {

                @Override
                public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {

                }

                @Override
                public void playDownSound(@Nonnull SoundHandler handler) {

                    MenuEntityHandler.this.sides[1].playLivingSound(handler, (float) MenuEntityHandler.this.volume, MenuEntityHandler.this.hurtPlayer);
                }

            };

            Stream.of(this.widgets).forEach(evt::addWidget);
        }
    }

    private void addReloadButton(List<Widget> widgets, Consumer<Widget> addWidget) {

        final List<String> leftSide = Lists.newArrayList("narrator.button.language", "menu.options", "fml.menu.mods", "menu.multiplayer");
        final List<String> rightSide = Lists.newArrayList("narrator.button.accessibility", "menu.quit", "menu.online", "menu.multiplayer");

        boolean isLeft = this.reloadMode.isLeft();
        for (String key : isLeft ? leftSide : rightSide) {

            Optional<Widget> found = Optional.empty();
            for (Widget widget : widgets) {

                ITextComponent message = widget.getMessage();
                if (message instanceof TranslationTextComponent) {

                    if (((TranslationTextComponent) message).getKey().equals(key)) {

                        found = Optional.of(widget);
                        break;
                    }
                }
            }

            if (found.isPresent()) {

                int x = found.get().x + (isLeft ? -24 + this.offsets[4]: found.get().getWidth() + 4 - this.offsets[4]);
                int y = found.get().y - this.offsets[5];
                addWidget.accept(new ImageButton(x, y, 20, 20, 0, 0, 20, RELOAD_TEXTURES, 32, 64, button -> {

                    JSONConfigUtil.load(MenuCompanions.JSON_CONFIG_NAME, MenuCompanions.MODID, MenuEntityProvider::serialize, MenuEntityProvider::deserialize);
                    MenuCompanions.LOGGER.info("Reloaded config file at {}", MenuCompanions.JSON_CONFIG_NAME);
                    Stream.of(this.sides).forEach(EntityMenuContainer::invalidate);
                }, new TranslationTextComponent("narrator.button.reload")) {

                    @Override
                    public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {

                        this.visible = MenuEntityHandler.this.reloadMode.requiresControl() && Screen.hasControlDown() || MenuEntityHandler.this.reloadMode.isAlways();
                        super.render(matrixStack, mouseX, mouseY, partialTicks);
                    }

                });

                return;
            }
        }
    }

    private void onGuiOpen(final GuiOpenEvent evt) {

        if (evt.getGui() instanceof MainMenuScreen) {

            Stream.of(this.sides).forEach(EntityMenuContainer::invalidate);
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
        }
    }

    private void onClientTick(final TickEvent.ClientTickEvent evt) {

        if (evt.phase != TickEvent.Phase.END && this.mc.currentScreen instanceof MainMenuScreen) {

            for (int i = 0; i < this.sides.length; i++) {

                if (this.sides[i].invalid()) {

                    this.setMenuSide(MenuSide.values()[i]);
                }
            }

            Stream.of(this.sides).forEach(EntityMenuContainer::tick);
        }
    }

    private void onRenderNameplate(final RenderNameplateEvent evt) {

        if (this.mc.currentScreen instanceof MainMenuScreen) {

            // we'll handle rendering nameplates ourselves (doesn't work for living entities anyways like that)
            evt.setResult(Event.Result.DENY);
        }
    }

    private void setMenuSide(@Nonnull MenuSide side) {

        EntityMenuContainer container = this.sides[side.ordinal()];
        // check if disabled via config
        if (container.disabled()) {

            return;
        }

        // limit max number of attempts
        for (int i = 0; i < 5; i++) {

            EntityMenuEntry entry = MenuEntityProvider.getRandomEntry(side);
            if (entry == null) {

                // entries for side are empty
                break;
            }

            Entity entity = entry.create(this.renderWorld);
            if (entity != null) {

                container.createEntity(entity, entry, side);
                return;
            }
        }

        container.setBroken();
    }

    public static boolean runOrElse(Entity entity, Consumer<Entity> action, Consumer<Entity> orElse) {

        try {

            action.accept(entity);
        } catch (Exception e) {

            MenuCompanions.LOGGER.error("Unable to handle entity {}: {}", entity.getDisplayName().getString(), e.getMessage());
            orElse.accept(entity);

            return false;
        }

        return true;
    }

    public static <T> void acceptIfPresent(T object, Consumer<T> action) {

        Optional.ofNullable(object).ifPresent(action);
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
            // manually update blacklist as refreshing the config takes too long
            blacklist = new EntryCollectionBuilder<>(ForgeRegistries.ENTITIES, MenuCompanions.LOGGER).buildEntrySet(blacklistSpec.get());
        }
    }

    public enum MenuSide {

        LEFT, RIGHT, BOTH

    }

    @SuppressWarnings("unused")
    private enum ReloadMode {

        NEVER(false, false, false),
        RIGHT_CONTROL(false, true, false),
        RIGHT_ALWAYS(false, false, true),
        LEFT_CONTROL(true, true, false),
        LEFT_ALWAYS(true, false, true);

        private final boolean left;
        private final boolean control;
        private final boolean always;

        ReloadMode(boolean left, boolean control, boolean always) {

            this.left = left;
            this.control = control;
            this.always = always;
        }

        public boolean isLeft() {

            return this.left;
        }

        public boolean requiresControl() {

            return this.control;
        }

        public boolean isAlways() {

            return this.always;
        }

    }

}
