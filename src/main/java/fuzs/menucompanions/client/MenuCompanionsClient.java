package fuzs.menucompanions.client;

import fuzs.menucompanions.MenuCompanions;
import fuzs.menucompanions.client.data.MenuEntityProvider;
import fuzs.menucompanions.client.player.MenuPlayer;
import fuzs.menucompanions.client.handler.MenuMobHandler;
import fuzs.menucompanions.client.handler.ReloadButtonHandler;
import fuzs.puzzleslib.json.JsonConfigFileUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;

@Mod.EventBusSubscriber(modid = MenuCompanions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class MenuCompanionsClient {
    @SubscribeEvent
    public static void onConstructMod(final FMLConstructModEvent evt) {
        registerHandlers$construct();
    }

    private static void registerHandlers$construct() {
        final ReloadButtonHandler reloadButtonHandler = new ReloadButtonHandler();
        MinecraftForge.EVENT_BUS.addListener(reloadButtonHandler::onInitScreen);
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent evt) {
        MenuMobHandler.INSTANCE.loadMobData();
        if (!MenuMobHandler.INSTANCE.loadLevel()) return;
        registerHandlers$setup();
        registerModCompat();
    }

    private static void registerHandlers$setup() {
        MinecraftForge.EVENT_BUS.addListener(MenuMobHandler.INSTANCE::onScreenOpen);
        MinecraftForge.EVENT_BUS.addListener(MenuMobHandler.INSTANCE::onDrawScreen);
        MinecraftForge.EVENT_BUS.addListener(MenuMobHandler.INSTANCE::onMouseClicked);
        MinecraftForge.EVENT_BUS.addListener(MenuMobHandler.INSTANCE::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, MenuMobHandler.INSTANCE::onRenderNameplate);
    }

    private static void registerModCompat() {
        // will prevent other mods from hooking into the player renderer on the main menu
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (RenderPlayerEvent.Pre evt) -> {
            if (evt.getPlayer() instanceof MenuPlayer) evt.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, (RenderPlayerEvent.Pre evt) -> {
            if (evt.getPlayer() instanceof MenuPlayer) evt.setCanceled(false);
        });
    }
}
