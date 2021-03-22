package com.fuzs.menucompanions.client.element;

import com.fuzs.menucompanions.MenuCompanions;
import com.fuzs.menucompanions.MenuCompanionsElements;
import com.fuzs.menucompanions.client.gui.EntityMenuContainer;
import com.fuzs.menucompanions.client.storage.entry.EntityMenuEntry;
import com.fuzs.menucompanions.client.storage.MenuEntityProvider;
import com.fuzs.menucompanions.client.util.ReloadMode;
import com.fuzs.menucompanions.client.world.MenuClientWorld;
import com.fuzs.puzzleslib_mc.config.ConfigManager;
import com.fuzs.puzzleslib_mc.config.ConfigValueData;
import com.fuzs.puzzleslib_mc.config.json.JsonConfigFileUtil;
import com.fuzs.puzzleslib_mc.element.AbstractElement;
import com.fuzs.puzzleslib_mc.element.side.IClientElement;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDirection;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class MenuEntityElement extends AbstractElement implements IClientElement {

    private static final ResourceLocation RELOAD_TEXTURES = new ResourceLocation(MenuCompanions.MODID, "textures/gui/reload.png");

    private final Minecraft mc = Minecraft.getInstance();
    private final EnumMap<MenuSide, EntityMenuContainer> menuSides = new EnumMap<>(MenuSide.class);
    private MenuClientWorld renderWorld;
    private int displayTimeCounter;
    private Consumer<UnaryOperator<List<String>>> addToBlacklist;

    // config settings
    public int displayTime;
    private int entitySize;
    /**
     * 0: left side x offset
     * 1: left side y offset
     * 2: right side x offset
     * 3: right side y offset
     * 4: button reload x offset
     * 5: button reload y offset
     */
    private final int[] buttonOffsets = new int[6];
    private boolean playAmbientSounds;
    private boolean hurtEntity;
    private Set<EntityType<?>> entityBlacklist;
    private ReloadMode reloadMode;

    @Override
    public String getDescription() {

        return "Cute little mobs to fill the emptiness of the main menu.";
    }

    @Override
    public void setupClient() {

        // create sides without world, has to be set later
        Stream.of(MenuSide.values())
                .filter(side -> side != MenuSide.BOTH)
                .forEach(side -> this.menuSides.put(side, new EntityMenuContainer(this.mc)));

        this.addListener(this::onGuiInit);
        this.addListener(this::onGuiOpen);
        this.addListener(this::onDrawScreen);
        this.addListener(this::onMouseClicked);
        this.addListener(this::onClientTick);
        this.addListener(this::onRenderNameplate, EventPriority.HIGHEST);

        // will prevent other mods from hooking into the player renderer on the main menu
        this.addListener((RenderPlayerEvent.Pre evt1) -> {

            if (this.mc.currentScreen instanceof MainMenuScreen) {

                evt1.setCanceled(true);
            }
        }, EventPriority.HIGHEST);
        this.addListener((RenderPlayerEvent.Pre evt) -> {

            if (this.mc.currentScreen instanceof MainMenuScreen) {

                evt.setCanceled(false);
            }
        }, EventPriority.LOWEST, true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void loadClient() {

        ConfigManager.get().getConfigDataAtPath(this.getRegistryName(), "Entity Blacklist")
                .map(data -> (ConfigValueData<ForgeConfigSpec.ConfigValue<List<String>>, List<String>, ?>) data)
                .ifPresent(data -> this.addToBlacklist = data::modifyConfigValue);

        JsonConfigFileUtil.getAllAndLoad(MenuCompanions.MODID, MenuEntityProvider::serialize, MenuEntityProvider::deserialize, MenuEntityProvider::clear);
        if (this.createRenderWorld()) {

            this.menuSides.values().forEach(container -> container.setWorld(this.renderWorld));
        }
    }

    private boolean createRenderWorld() {

        try {

            GameProfile profileIn = this.mc.getSession().getProfile();
            @SuppressWarnings("ConstantConditions")
            ClientPlayNetHandler clientPlayNetHandler = new ClientPlayNetHandler(this.mc, null,  new NetworkManager(PacketDirection.CLIENTBOUND), profileIn);
            WorldSettings worldSettings = new WorldSettings(0L, GameType.SURVIVAL, false, false, WorldType.DEFAULT);
            this.renderWorld = new MenuClientWorld(clientPlayNetHandler, worldSettings, DimensionType.THE_NETHER, this.mc.getProfiler(), this.mc.worldRenderer);
        } catch (Exception e) {

            MenuCompanions.LOGGER.error("Unable to create rendering world: {}", e.getMessage());
            this.forceDisable();

            return false;
        }

        return true;
    }

    @Override
    public void setupClientConfig(ForgeConfigSpec.Builder builder) {

        addToConfig(builder.comment("Time in seconds an entity will be shown for. Set to 0 to never change entities.").defineInRange("Display Time", 0, 0, Integer.MAX_VALUE), v -> this.displayTime = v, v -> v * 20);
        addToConfig(builder.comment("Size of menu companions.").defineInRange("Entity Size", 60, 0, Integer.MAX_VALUE), v -> this.entitySize = v);
        addToConfig(builder.comment("Offset on x-axis from original position on left side.").defineInRange("Left X-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.buttonOffsets[0] = v);
        addToConfig(builder.comment("Offset on y-axis from original position on left side.").defineInRange("Left Y-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.buttonOffsets[1] = v);
        addToConfig(builder.comment("Offset on x-axis from original position on right side.").defineInRange("Right X-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.buttonOffsets[2] = v);
        addToConfig(builder.comment("Offset on y-axis from original position on right side.").defineInRange("Right Y-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.buttonOffsets[3] = v);
        addToConfig(builder.comment("Play ambient sounds when clicking on menu mobs.").define("Play Sounds", true), v -> this.playAmbientSounds = v);
        addToConfig(builder.comment("Hurt entity when clicked and there is no ambient sound to play.").define("Hurt Entity", false), v -> this.hurtEntity = v);
        addToConfig(builder.comment("Blacklist to prevent certain entities form rendering. Problematic entities will be added automatically upon being detected.").define("Entity Blacklist", ConfigManager.get().getKeyList(EntityType.ENDER_DRAGON, EntityType.EVOKER_FANGS, EntityType.FALLING_BLOCK, EntityType.AREA_EFFECT_CLOUD, EntityType.ITEM, EntityType.FISHING_BOBBER)), v -> this.entityBlacklist = v, v -> deserializeToSet(v, ForgeRegistries.ENTITIES));
        addToConfig(builder.comment("When to show reload button on main menu. By default requires the control key to be pressed.").defineEnum("Reload Button", ReloadMode.RIGHT_ALWAYS), v -> this.reloadMode = v);
        addToConfig(builder.comment("Reload button offset on x-axis from original position.").defineInRange("Reload X-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.buttonOffsets[4] = v);
        addToConfig(builder.comment("Reload button offset on y-axis from original position.").defineInRange("Reload Y-Offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.buttonOffsets[5] = v);
        addToConfig(builder.comment("Which side entities can be shown at.").defineEnum("Entity Side", MenuSide.BOTH), v -> {

            this.menuSides.get(MenuSide.LEFT).setEnabled(v != MenuSide.RIGHT);
            this.menuSides.get(MenuSide.RIGHT).setEnabled(v != MenuSide.LEFT);
        });
    }

    private void onGuiInit(final GuiScreenEvent.InitGuiEvent.Post evt) {

        if (evt.getGui() instanceof MainMenuScreen && this.reloadMode != ReloadMode.NEVER) {

            this.addReloadButton(evt.getWidgetList(), evt::addWidget);
        }
    }

    private void addReloadButton(List<Widget> widgets, Consumer<Widget> addWidget) {

        Widget parentWidget = this.getReloadParentWidget(widgets);
        if (parentWidget != null) {

            int posX = parentWidget.x + (this.reloadMode.isLeft() ? -24 + this.buttonOffsets[4] : parentWidget.getWidth() + 4 - this.buttonOffsets[4]);
            int posY = parentWidget.y - this.buttonOffsets[5];
            addWidget.accept(new ImageButton(posX, posY, 20, 20, 0, 0, 20, RELOAD_TEXTURES, 32, 64, button -> {

                this.displayTimeCounter = 0;
                JsonConfigFileUtil.getAllAndLoad(MenuCompanions.MODID, MenuEntityProvider::serialize, MenuEntityProvider::deserialize, MenuEntityProvider::clear);
                MenuCompanions.LOGGER.info("Reloaded config files at {}", MenuCompanions.MODID);
                this.menuSides.values().forEach(EntityMenuContainer::setUpdateRequired);
            }, new TranslationTextComponent("narrator.button.reload").getUnformattedComponentText()) {

                @Override
                public void render(int mouseX, int mouseY, float partialTicks) {

                    this.visible = MenuEntityElement.this.reloadMode.requiresControl() && Screen.hasControlDown() || MenuEntityElement.this.reloadMode.isAlways();
                    super.render(mouseX, mouseY, partialTicks);
                }

            });
        }
    }
    
    @Nullable
    private Widget getReloadParentWidget(List<Widget> widgets) {

        final List<String> leftSide = Lists.newArrayList("narrator.button.language", "menu.options", "fml.menu.mods", "menu.multiplayer");
        final List<String> rightSide = Lists.newArrayList("narrator.button.accessibility", "menu.quit", "menu.online", "menu.multiplayer");

        for (String key : this.reloadMode.isLeft() ? leftSide : rightSide) {

            Widget parentWidget = this.getWidgetByTranslation(widgets, key);
            if (parentWidget != null) {
                
                return parentWidget;
            }
        }
        
        return null;
    }

    @Nullable
    private Widget getWidgetByTranslation(List<Widget> widgets, String key) {

        for (Widget widget : widgets) {

            String message = widget.getMessage();
            if (new TranslationTextComponent(key).getFormattedText().equals(message)) {

                return widget;
            }
        }
        
        return null;
    }

    private void onGuiOpen(final GuiOpenEvent evt) {

        if (evt.getGui() instanceof MainMenuScreen) {

            this.menuSides.values().forEach(EntityMenuContainer::setUpdateRequired);
        }
    }

    private void onDrawScreen(final GuiScreenEvent.DrawScreenEvent.Post evt) {

        if (evt.getGui() instanceof MainMenuScreen) {

            this.menuSides.forEach((side, container) -> {

                int sideIndex = side == MenuSide.RIGHT ? 2 : 0;
                int xOffset = (evt.getGui().width / 2 - 96) / 2 + this.buttonOffsets[sideIndex];
                int posX = sideIndex == 0 ? xOffset : evt.getGui().width - xOffset;
                int posY = evt.getGui().height / 4 + 116 - this.buttonOffsets[sideIndex + 1];
                container.render(posX, posY, this.entitySize / 2.0F, -evt.getMouseX(), -evt.getMouseY(), evt.getRenderPartialTicks());
            });
        }
    }

    private void onMouseClicked(final GuiScreenEvent.MouseClickedEvent.Post evt) {

        // left mouse button, same as when activating buttons
        if (evt.getGui() instanceof MainMenuScreen && evt.getButton() == 0) {

            // check if mouse button has been pressed in a certain area
            Consumer<EntityMenuContainer> interaction = container -> container.interactWithEntity(this.mc.getSoundHandler(), this.playAmbientSounds, this.hurtEntity);
            if (this.clickLeftSide(evt.getGui().width, evt.getGui().height, evt.getMouseX(), evt.getMouseY(), interaction) ||
                    this.clickRightSide(evt.getGui().width, evt.getGui().height, evt.getMouseX(), evt.getMouseY(), interaction)) {

                evt.setCanceled(true);
            }
        }
    }

    private boolean clickLeftSide(int width, int height, double mouseX, double mouseY, Consumer<EntityMenuContainer> interaction) {

        int posX = (width / 2 - 96) / 2 - this.entitySize / 2 + this.buttonOffsets[0];
        int posY = height / 4 + 48 + 80 - this.entitySize * 4 / 3 - this.buttonOffsets[1];

        return this.interactAtSide(MenuSide.LEFT, posX, posY, mouseX, mouseY, interaction);
    }

    private boolean clickRightSide(int width, int height, double mouseX, double mouseY, Consumer<EntityMenuContainer> interaction) {

        int posX = width - ((width / 2 - 96) / 2 + this.entitySize / 2 + this.buttonOffsets[2]);
        int posY = height / 4 + 48 + 80 - this.entitySize * 4 / 3 - this.buttonOffsets[3];

        return this.interactAtSide(MenuSide.RIGHT, posX, posY, mouseX, mouseY, interaction);
    }

    private boolean interactAtSide(MenuSide side, int posX, int posY, double mouseX, double mouseY, Consumer<EntityMenuContainer> interaction) {

        if (isPointInRegion(posX, posY, this.entitySize, this.entitySize * 4 / 3, mouseX, mouseY)) {

            interaction.accept(this.menuSides.get(side));

            return true;
        }

        return false;
    }

    private static boolean isPointInRegion(int x, int y, int width, int height, double mouseX, double mouseY) {

        return mouseX >= (x - 1) && mouseX < (x + width + 1) && mouseY >= (y - 1) && mouseY < (y + height + 1);
    }

    private void onClientTick(final TickEvent.ClientTickEvent evt) {

        if (evt.phase != TickEvent.Phase.END && this.mc.currentScreen instanceof MainMenuScreen) {

            if (this.displayTime > 0) {

                this.displayTimeCounter++;
                this.displayTimeCounter %= this.displayTime;
            }

            this.menuSides.entrySet().stream()
                    .filter(entry -> this.displayTime > 0 && this.displayTimeCounter == 0 || entry.getValue().isInvalid())
                    .forEach(entry -> MenuEntityElement.this.setMenuSide(entry.getKey()));

            this.menuSides.values().forEach(EntityMenuContainer::tick);
        }
    }

    private void onRenderNameplate(final RenderNameplateEvent evt) {

        if (this.mc.currentScreen instanceof MainMenuScreen) {

            // we'll handle rendering nameplates ourselves (doesn't work for living entities anyways like that)
            evt.setResult(Event.Result.DENY);
        }
    }

    private void setMenuSide(MenuSide side) {

        EntityMenuContainer container = this.menuSides.get(side);

        // check if disabled via config
        if (container.isDisabled()) {

            return;
        }

        while (true) {

            EntityMenuEntry entry = MenuEntityProvider.getRandomEntry(side);
            if (entry == null) {

                // entries for side are empty
                break;
            }

            Entity entity = entry.create(this.renderWorld);
            if (entity != null) {

                container.createEntity(entity, entry, side == MenuSide.RIGHT);
                return;
            }

            // entity from entry can't be created, so remove the entry
            MenuEntityProvider.removeEntry(entry);
        }

        container.setBroken();
    }

    public boolean isAllowed(EntityType<?> type) {

        return this.entityBlacklist == null || !this.entityBlacklist.contains(type);
    }

    public void addToBlacklist(String type) {

        ResourceLocation location = ResourceLocation.tryCreate(type);
        if (location != null && ForgeRegistries.ENTITIES.containsKey(location)) {

            this.addToBlacklist(ForgeRegistries.ENTITIES.getValue(location));
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void addToBlacklist(EntityType<?> type) {

        if (this.addToBlacklist != null && this.isAllowed(type)) {

            this.addToBlacklist.accept(list -> {

                list.add(type.getRegistryName().toString());
                return list;
            });
        }
    }

    public static MenuEntityElement get() {

        return MenuCompanionsElements.MENU_ENTITY.getAs(MenuEntityElement.class);
    }

    public enum MenuSide {

        LEFT, BOTH, RIGHT

    }

}
