package fuzs.menucompanions.client.handler;

import com.mojang.authlib.GameProfile;
import fuzs.menucompanions.MenuCompanions;
import fuzs.menucompanions.client.gui.MenuEntityRenderer;
import fuzs.menucompanions.client.data.MenuEntityProvider;
import fuzs.menucompanions.client.data.entry.MobMenuData;
import fuzs.menucompanions.client.multiplayer.MenuClientLevel;
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

public class MenuMobHandler {
    public static final MenuMobHandler INSTANCE = new MenuMobHandler(Minecraft.getInstance());

    private final Minecraft minecraft;
    private final MenuEntityRenderer[] entityRenderers = new MenuEntityRenderer[2];
    private MenuClientLevel level;
    private int displayTicks;

    public MenuMobHandler(Minecraft minecraft) {
        this.minecraft = minecraft;
        // create sides without level, has to be set later
        for (int i = 0; i < this.entityRenderers.length; i++) {
            this.entityRenderers[i] = new MenuEntityRenderer(minecraft);
        }
    }

    public void loadMobData() {
        JsonConfigFileUtil.getAllAndLoad(MenuCompanions.MOD_ID, MenuEntityProvider::serialize, MenuEntityProvider::deserialize, MenuEntityProvider::clear);
    }

    public boolean loadLevel() {
        this.level = this.createLevel();
        if (this.level != null) {
            for (MenuEntityRenderer entityRenderer : this.entityRenderers) {
                entityRenderer.setLevel(this.level);
            }
            return true;
        }
        return false;
    }

    private MenuClientLevel createLevel() {
        try {
            GameProfile profileIn = this.minecraft.getUser().getGameProfile();
            final Connection connection = new Connection(PacketFlow.CLIENTBOUND);
            connection.setProtocol(ConnectionProtocol.PLAY);
            ClientPacketListener packetListener = new ClientPacketListener(this.minecraft, null, connection, profileIn, this.minecraft.createTelemetryManager());
            connection.setListener(packetListener);
            ClientLevel.ClientLevelData levelData = new ClientLevel.ClientLevelData(Difficulty.HARD, false, false);
            // use nether dimension so we can be piglin safe
            DimensionType dimensionType = RegistryAccess.builtin().registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).getOrThrow(DimensionType.NETHER_LOCATION);
            return new MenuClientLevel(packetListener, levelData, Level.NETHER, dimensionType, this.minecraft::getProfiler, this.minecraft.levelRenderer);
        } catch (Exception e) {
            MenuCompanions.LOGGER.error("Unable to create dummy level for rendering mobs", e);
        }
        return null;
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
                int posX = (screen.width / 2 - 96) / 2 + MenuCompanions.CONFIG.client().mobOffsets[side.index()];
                if (side == ClientConfig.MenuSide.RIGHT) {
                    posX = screen.width - posX;
                }
                int posY = screen.height / 4 + 116 - MenuCompanions.CONFIG.client().mobOffsets[side.index() + 1];
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
        int posX = (width / 2 - 96) / 2 - entitySize / 2 + MenuCompanions.CONFIG.client().mobOffsets[0];
        int posY = height / 4 + 48 + 80 - entitySize * 4 / 3 - MenuCompanions.CONFIG.client().mobOffsets[1];
        return this.interactAtSide(ClientConfig.MenuSide.LEFT, posX, posY, mouseX, mouseY, entitySize);
    }

    private boolean clickRightSide(int width, int height, double mouseX, double mouseY, int entitySize) {
        int posX = width - ((width / 2 - 96) / 2 + entitySize / 2 + MenuCompanions.CONFIG.client().mobOffsets[2]);
        int posY = height / 4 + 48 + 80 - entitySize * 4 / 3 - MenuCompanions.CONFIG.client().mobOffsets[3];
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
        if (evt.phase != TickEvent.Phase.START) return;
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
            MobMenuData entry = MenuEntityProvider.getRandomEntry(side);
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
