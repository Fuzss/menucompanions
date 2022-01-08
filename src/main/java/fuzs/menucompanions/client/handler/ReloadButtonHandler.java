package fuzs.menucompanions.client.handler;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import fuzs.menucompanions.MenuCompanions;
import fuzs.menucompanions.client.storage.MenuEntityProvider;
import fuzs.menucompanions.client.util.ReloadMode;
import fuzs.puzzleslib.json.JsonConfigFileUtil;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class ReloadButtonHandler {
    private static final ResourceLocation RELOAD_TEXTURES = new ResourceLocation(MenuCompanions.MOD_ID, "textures/gui/reload.png");

    @SubscribeEvent
    public void onGuiInit(final ScreenEvent.InitScreenEvent.Post evt) {
        if (evt.getScreen() instanceof PauseScreen) {
            final ReloadMode reloadMode = MenuCompanions.CONFIG.client().reloadMode;
            if (reloadMode != ReloadMode.NEVER) {
                this.addReloadButton(evt.getListenersList(), evt::addListener, reloadMode);
            }
        }
    }

    private void addReloadButton(List<GuiEventListener> widgets, Consumer<GuiEventListener> addWidget, ReloadMode reloadMode) {
        AbstractWidget parentWidget = this.getReloadParentWidget(widgets, reloadMode);
        if (parentWidget != null) {
            final int[] reloadButtonOffsets = MenuCompanions.CONFIG.client().reloadButtonOffsets;
            int posX = parentWidget.x + (reloadMode.left() ? -24 + reloadButtonOffsets[0] : parentWidget.getWidth() + 4 - reloadButtonOffsets[0]);
            int posY = parentWidget.y - reloadButtonOffsets[1];
            addWidget.accept(new ImageButton(posX, posY, 20, 20, 0, 0, 20, RELOAD_TEXTURES, 32, 64, button -> {
                MenuEntityHandler.INSTANCE.resetDisplayTicks();
                JsonConfigFileUtil.getAllAndLoad(MenuCompanions.MOD_ID, MenuEntityProvider::serialize, MenuEntityProvider::deserialize, MenuEntityProvider::clear);
                MenuCompanions.LOGGER.info("Reloaded config files at {}", MenuCompanions.MOD_ID);
                MenuEntityHandler.INSTANCE.setUpdateRequired();
            }, new TranslatableComponent("narrator.button.reload")) {
                @Override
                public void render(@Nonnull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
                    this.visible = reloadMode.withModifierKey() && Screen.hasControlDown() || reloadMode.isAlways();
                    super.render(matrixStack, mouseX, mouseY, partialTicks);
                }
            });
        }
    }

    @Nullable
    private AbstractWidget getReloadParentWidget(List<GuiEventListener> listeners, ReloadMode reloadMode) {
        List<String> leftSide = ImmutableList.of("narrator.button.language", "menu.options", "fml.menu.mods", "menu.multiplayer");
        List<String> rightSide = ImmutableList.of("narrator.button.accessibility", "menu.quit", "menu.online", "menu.multiplayer");
        for (String key : reloadMode.left() ? leftSide : rightSide) {
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
