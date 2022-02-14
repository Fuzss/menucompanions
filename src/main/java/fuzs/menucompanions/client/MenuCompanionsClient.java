package fuzs.menucompanions.client;

import fuzs.menucompanions.MenuCompanions;
import fuzs.menucompanions.client.handler.MenuMobHandler;
import fuzs.menucompanions.client.handler.ReloadButtonHandler;
import fuzs.menucompanions.client.player.MenuPlayer;
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
        registerHandlers();
    }

    private static void registerHandlers() {
        final ReloadButtonHandler reloadButtonHandler = new ReloadButtonHandler();
        MinecraftForge.EVENT_BUS.addListener(reloadButtonHandler::onInitScreen);
        MinecraftForge.EVENT_BUS.addListener(MenuMobHandler.INSTANCE::onScreenOpen);
        MinecraftForge.EVENT_BUS.addListener(MenuMobHandler.INSTANCE::onWorldUnload);
        MinecraftForge.EVENT_BUS.addListener(MenuMobHandler.INSTANCE::onDrawScreen);
        MinecraftForge.EVENT_BUS.addListener(MenuMobHandler.INSTANCE::onMouseClicked);
        MinecraftForge.EVENT_BUS.addListener(MenuMobHandler.INSTANCE::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, MenuMobHandler.INSTANCE::onRenderNameplate);
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent evt) {
        MenuMobHandler.INSTANCE.loadMobData();
    }
}
