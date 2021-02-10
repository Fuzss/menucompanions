package com.fuzs.puzzleslib_mc;

import com.fuzs.puzzleslib_mc.capability.CapabilityController;
import com.fuzs.puzzleslib_mc.element.registry.ElementRegistry;
import com.fuzs.puzzleslib_mc.network.NetworkHandler;
import com.fuzs.puzzleslib_mc.registry.RegistryManager;
import com.fuzs.puzzleslib_mc.util.PuzzlesLibUtil;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("unused")
//@Mod(PuzzlesLib.MODID)
public class PuzzlesLib {

    public static final String MODID = "puzzleslib";
    public static final String NAME = "Puzzles Lib";
    public static final Logger LOGGER = LogManager.getLogger(PuzzlesLib.NAME);

    private static RegistryManager registryManager;
    private static NetworkHandler networkHandler;
    private static CapabilityController capabilityController;

    public PuzzlesLib() {

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onServerSetup);
    }

    protected void onCommonSetup(final FMLCommonSetupEvent evt) {

        ElementRegistry.load(evt);
    }

    protected void onClientSetup(final FMLClientSetupEvent evt) {

        ElementRegistry.load(evt);
    }

    protected void onServerSetup(final FMLDedicatedServerSetupEvent evt) {

        ElementRegistry.load(evt);
    }

    /**
     * set mod to only be required on one side, server or client
     * works like <code>clientSideOnly</code> back in 1.12
     */
    protected final void setSideSideOnly() {

        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (remote, isServer) -> true));
    }

    /**
     * @return registry manager for puzzles lib mods
     */
    public static RegistryManager getRegistryManager() {

        return PuzzlesLibUtil.getOrElse(registryManager, RegistryManager::new, instance -> registryManager = instance);
    }

    /**
     * @return network handler for puzzles lib mods
     */
    public static NetworkHandler getNetworkHandler() {

        return PuzzlesLibUtil.getOrElse(networkHandler, NetworkHandler::new, instance -> networkHandler = instance);
    }

    /**
     * @return capability controller for puzzles lib mods
     */
    public static CapabilityController getCapabilityController() {

        return PuzzlesLibUtil.getOrElse(capabilityController, CapabilityController::new, instance -> capabilityController = instance);
    }

}
