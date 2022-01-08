package fuzs.menucompanions.client;

import fuzs.menucompanions.MenuCompanions;
import fuzs.menucompanions.client.entity.player.MenuPlayer;
import fuzs.menucompanions.client.handler.MenuEntityHandler;
import fuzs.menucompanions.client.handler.ReloadButtonHandler;
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
        MinecraftForge.EVENT_BUS.addListener(reloadButtonHandler::onGuiInit);
        final MenuEntityHandler menuEntityHandler = new MenuEntityHandler();
        MinecraftForge.EVENT_BUS.addListener(menuEntityHandler::onScreenOpen);
        MinecraftForge.EVENT_BUS.addListener(menuEntityHandler::onDrawScreen);
        MinecraftForge.EVENT_BUS.addListener(menuEntityHandler::onMouseClicked);
        MinecraftForge.EVENT_BUS.addListener(menuEntityHandler::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, menuEntityHandler::onRenderNameplate);
        registerModCompat();
    }

    private static void registerModCompat() {
        // TODO is this still needed now that we render on the pause screen?
        // will prevent other mods from hooking into the player renderer on the main menu
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (RenderPlayerEvent.Pre evt) -> {
            if (evt.getPlayer() instanceof MenuPlayer) {
                evt.setCanceled(true);
            }
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, (RenderPlayerEvent.Pre evt) -> {
            if (evt.getPlayer() instanceof MenuPlayer) {
                evt.setCanceled(false);
            }
        });
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent evt) {
    }
}
