package fuzs.menucompanions.client.handler;

import com.mojang.authlib.GameProfile;
import fuzs.menucompanions.MenuCompanions;
import fuzs.menucompanions.client.gui.MenuEntityRenderer;
import fuzs.menucompanions.client.storage.MenuEntityProvider;
import fuzs.menucompanions.client.storage.entry.EntityMenuData;
import fuzs.menucompanions.client.world.MenuClientWorld;
import fuzs.menucompanions.config.ClientConfig;
import fuzs.puzzleslib.json.JsonConfigFileUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MenuEntityHandler {
    public static final MenuEntityHandler INSTANCE = new MenuEntityHandler();

    private final Minecraft minecraft = Minecraft.getInstance();
    private final MenuEntityRenderer[] entityRenderers;
    private MenuClientWorld level;
    private int displayTicks;

    public MenuEntityHandler() {
        // create sides without world, has to be set later
        this.entityRenderers = new MenuEntityRenderer[]{new MenuEntityRenderer(this.minecraft), new MenuEntityRenderer(this.minecraft)};
    }

    public void loadClient() {
        JsonConfigFileUtil.getAllAndLoad(MenuCompanions.MOD_ID, MenuEntityProvider::serialize, MenuEntityProvider::deserialize, MenuEntityProvider::clear);
        if (this.createMenuLevel()) {
            for (MenuEntityRenderer entityRenderer : this.entityRenderers) {
                entityRenderer.setLevel(this.level);
            }
        }
    }

    private boolean createMenuLevel() {
        try {
            GameProfile profileIn = this.minecraft.player.getGameProfile();
            final Connection connection = new Connection(PacketFlow.CLIENTBOUND);
            connection.setProtocol(ConnectionProtocol.PLAY);
            ClientPacketListener packetListener = new ClientPacketListener(this.minecraft, null, connection, profileIn, this.minecraft.createTelemetryManager());
            connection.setListener(packetListener);
            ClientLevel.ClientLevelData levelData = new ClientLevel.ClientLevelData(Difficulty.HARD, false, false);
            // use nether dimension so we can be piglin safe
            DimensionType dimensionType = RegistryAccess.builtin().registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).getOrThrow(DimensionType.NETHER_LOCATION);
            this.level = new MenuClientWorld(packetListener, levelData, Level.NETHER, dimensionType, this.minecraft::getProfiler, this.minecraft.levelRenderer);
        } catch (Exception e) {
            MenuCompanions.LOGGER.error("unable to create dummy level for rendering menu companions", e);
            return false;
        }
        return true;
    }

    @SubscribeEvent
    public void onScreenOpen(final ScreenOpenEvent evt) {
        if (evt.getScreen() instanceof PauseScreen) {
            this.setUpdateRequired();
        }
    }

    public void setUpdateRequired() {
        for (MenuEntityRenderer entityRenderer : this.entityRenderers) {
            entityRenderer.setUpdateRequired();
        }
    }

    @SubscribeEvent
    public void onDrawScreen(final ScreenEvent.DrawScreenEvent.Post evt) {
        final Screen screen = evt.getScreen();
        if (screen instanceof PauseScreen) {
            for (ClientConfig.MenuSide side : ClientConfig.MenuSide.sides()) {
                int sideIndex = side == ClientConfig.MenuSide.RIGHT ? 2 : 0;
                int xOffset = (screen.width / 2 - 96) / 2 + MenuCompanions.CONFIG.client().menuButtonOffsets[sideIndex];
                int posX = sideIndex == 0 ? xOffset : screen.width - xOffset;
                int posY = screen.height / 4 + 116 - MenuCompanions.CONFIG.client().menuButtonOffsets[sideIndex + 1];
                this.entityRenderers[side.ordinal()].render(posX, posY, MenuCompanions.CONFIG.client().entitySize / 2.0F, -evt.getMouseX(), -evt.getMouseY(), evt.getPartialTicks());
            }
        }
    }

    @SubscribeEvent
    public void onMouseClicked(final ScreenEvent.MouseClickedEvent.Post evt) {
        // left mouse button, same as when activating buttons
        final Screen screen = evt.getScreen();
        if (screen instanceof PauseScreen && evt.getButton() == 0) {
            final int entitySize = MenuCompanions.CONFIG.client().entitySize;
            if (this.clickLeftSide(screen.width, screen.height, evt.getMouseX(), evt.getMouseY(), entitySize)) {
                evt.setCanceled(true);
            } else if (this.clickRightSide(screen.width, screen.height, evt.getMouseX(), evt.getMouseY(), entitySize)) {
                evt.setCanceled(true);
            }
        }
    }

    private boolean clickLeftSide(int width, int height, double mouseX, double mouseY, int entitySize) {
        int posX = (width / 2 - 96) / 2 - entitySize / 2 + MenuCompanions.CONFIG.client().menuButtonOffsets[0];
        int posY = height / 4 + 48 + 80 - entitySize * 4 / 3 - MenuCompanions.CONFIG.client().menuButtonOffsets[1];
        return this.interactAtSide(ClientConfig.MenuSide.LEFT, posX, posY, mouseX, mouseY, entitySize);
    }

    private boolean clickRightSide(int width, int height, double mouseX, double mouseY, int entitySize) {
        int posX = width - ((width / 2 - 96) / 2 + entitySize / 2 + MenuCompanions.CONFIG.client().menuButtonOffsets[2]);
        int posY = height / 4 + 48 + 80 - entitySize * 4 / 3 - MenuCompanions.CONFIG.client().menuButtonOffsets[3];
        return this.interactAtSide(ClientConfig.MenuSide.RIGHT, posX, posY, mouseX, mouseY, entitySize);
    }

    private boolean interactAtSide(ClientConfig.MenuSide side, int posX, int posY, double mouseX, double mouseY, int entitySize) {
        if (isPointInRegion(posX, posY, entitySize, entitySize * 4 / 3, mouseX, mouseY)) {
            this.entityRenderers[side.ordinal()].interactWithEntity(this.minecraft.getSoundManager(), MenuCompanions.CONFIG.client().playAmbientSounds, MenuCompanions.CONFIG.client().hurtEntity);
            return true;
        }
        return false;
    }

    private static boolean isPointInRegion(int x, int y, int width, int height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @SubscribeEvent
    public void onClientTick(final TickEvent.ClientTickEvent evt) {
        if (evt.phase == TickEvent.Phase.END) return;
        if (this.minecraft.screen instanceof PauseScreen) {
            int displayTime = MenuCompanions.CONFIG.client().displayTime;
            if (displayTime > 0) {
                this.displayTicks++;
                this.displayTicks %= displayTime;
            }
            for (ClientConfig.MenuSide side : ClientConfig.MenuSide.sides()) {
                if (displayTime > 0 && this.displayTicks == 0 || this.entityRenderers[side.ordinal()].isInvalid()) {
                    this.createEntityAtSide(side);
                }
            }
            for (MenuEntityRenderer menuEntityRenderer : this.entityRenderers) {
                menuEntityRenderer.tick();
            }
        }
    }

    @SubscribeEvent
    public void onRenderNameplate(final RenderNameplateEvent evt) {
        if (this.minecraft.screen instanceof PauseScreen) {
            // we'll handle rendering nameplates ourselves (doesn't work for living entities anyways like that)
            evt.setResult(Event.Result.DENY);
        }
    }

    private void createEntityAtSide(ClientConfig.MenuSide side) {
        MenuEntityRenderer entityRenderer = this.entityRenderers[side.ordinal()];
        // check if disabled via config
        if (entityRenderer.isDisabled()) return;
        while (true) {
            EntityMenuData entry = MenuEntityProvider.getRandomEntry(side);
            // entries for side are empty
            if (entry == null) break;
            Entity entity = entry.create(this.level);
            if (entity != null) {
                entityRenderer.createEntity(entity, entry, side == ClientConfig.MenuSide.RIGHT);
                return;
            }
            // entity from entry can't be created, so remove the entry
            MenuEntityProvider.removeEntry(entry);
        }
        entityRenderer.setBroken();
    }

    public void resetDisplayTicks() {
        this.displayTicks = 0;
    }

    public void enableEntityRenderer(ClientConfig.MenuSide side, boolean enabled) {
        this.entityRenderers[side.ordinal()].setEnabled(enabled);
    }
}
