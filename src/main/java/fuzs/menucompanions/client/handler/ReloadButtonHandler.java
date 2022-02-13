package fuzs.menucompanions.client.handler;

import com.google.common.collect.ImmutableList;
import fuzs.menucompanions.MenuCompanions;
import fuzs.menucompanions.data.MenuEntityProvider;
import fuzs.menucompanions.config.ClientConfig;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class ReloadButtonHandler {
    private static final ResourceLocation RELOAD_TEXTURES = new ResourceLocation(MenuCompanions.MOD_ID, "textures/gui/reload.png");

    @SubscribeEvent
    public void onInitScreen(final ScreenEvent.InitScreenEvent.Post evt) {
        if (MenuCompanions.CONFIG.client().reloadMode == ClientConfig.ReloadMode.NO_BUTTON) return;
        if (evt.getScreen() instanceof PauseScreen) {
            this.addReloadButton(evt.getListenersList(), evt::addListener, MenuCompanions.CONFIG.client().reloadMode);
        }
    }

    private void addReloadButton(List<GuiEventListener> widgets, Consumer<GuiEventListener> addWidget, ClientConfig.ReloadMode reloadMode) {
        AbstractWidget parentWidget = this.getReloadParentWidget(widgets, reloadMode);
        if (parentWidget == null) return;
        int posX = parentWidget.x + (reloadMode == ClientConfig.ReloadMode.LEFT ? -24 + MenuCompanions.CONFIG.client().reloadOffsets[0] : parentWidget.getWidth() + 4 - MenuCompanions.CONFIG.client().reloadOffsets[0]);
        int posY = parentWidget.y - MenuCompanions.CONFIG.client().reloadOffsets[1];
        addWidget.accept(new ImageButton(posX, posY, 20, 20, 0, 0, 20, RELOAD_TEXTURES, 32, 64, button -> {
            MenuMobHandler.INSTANCE.resetDisplayTicks();
            MenuMobHandler.INSTANCE.loadMobData();
            MenuCompanions.LOGGER.info("Reloaded menu companions config files");
            MenuMobHandler.INSTANCE.setUpdateRequired();
        }, new TranslatableComponent("narrator.button.reload")));
    }

    @Nullable
    private AbstractWidget getReloadParentWidget(List<GuiEventListener> listeners, ClientConfig.ReloadMode reloadMode) {
        List<String> leftSide = ImmutableList.of("menu.options", "menu.sendFeedback", "gui.advancements", "menu.returnToMenu", "menu.disconnect");
        List<String> rightSide = ImmutableList.of("menu.shareToLan", "menu.reportBugs", "gui.stats", "menu.returnToMenu", "menu.disconnect");
        for (String key : reloadMode == ClientConfig.ReloadMode.LEFT ? leftSide : rightSide) {
            AbstractWidget parentWidget = this.getWidgetByTranslation(listeners, key);
            if (parentWidget != null) {
                return parentWidget;
            }
        }
        return null;
    }

    @Nullable
    private AbstractWidget getWidgetByTranslation(List<GuiEventListener> listeners, String key) {
        for (GuiEventListener listener : listeners) {
            if (listener instanceof AbstractWidget widget && widget.getMessage() instanceof TranslatableComponent component) {
                if (component.getKey().equals(key)) {
                    return widget;
                }
            }
        }
        return null;
    }
}
