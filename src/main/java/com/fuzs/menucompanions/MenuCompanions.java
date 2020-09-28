package com.fuzs.menucompanions;

import com.fuzs.menucompanions.client.handler.MenuEntityHandler;
import com.fuzs.menucompanions.client.handler.MenuEntityProvider;
import com.fuzs.menucompanions.config.ConfigManager;
import com.fuzs.menucompanions.config.JSONConfigUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("unused")
@Mod(MenuCompanions.MODID)
public class MenuCompanions {

    public static final String MODID = "menucompanions";
    public static final String NAME = "Menu Companions";
    public static final Logger LOGGER = LogManager.getLogger(MenuCompanions.NAME);

    public static final String JSON_CONFIG_NAME = "mobs.json";
    private final MenuEntityHandler handler = new MenuEntityHandler();

    @SuppressWarnings("Convert2Lambda")
    public MenuCompanions() {

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onLoadComplete);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ConfigManager::onModConfig);

        // Forge doesn't like this being a lambda
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> new DistExecutor.SafeRunnable() {

            @Override
            public void run() {

                // this also creates the folder for the default Forge config
                JSONConfigUtil.load(JSON_CONFIG_NAME, MODID, MenuEntityProvider::serialize, MenuEntityProvider::deserialize);
                ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
                MenuCompanions.this.handler.setup(builder);
                ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, builder.build(), ConfigManager.configNameForFolder(ModConfig.Type.CLIENT, MODID));

                FMLJavaModLoadingContext.get().getModEventBus().addListener(MenuCompanions.this::onClientSetup);
            }

        });

        // clientSideOnly = true
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (remote, isServer) -> true));
    }

    private void onClientSetup(final FMLClientSetupEvent evt) {

        this.handler.load();
    }

    private void onLoadComplete(final FMLLoadCompleteEvent evt) {

        ConfigManager.sync();
        this.handler.createSides();
    }

}