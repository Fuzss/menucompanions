package com.fuzs.menucompanions;

import com.fuzs.puzzleslib_mc.PuzzlesLib;
import com.fuzs.puzzleslib_mc.config.ConfigManager;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("unused")
@Mod(MenuCompanions.MODID)
public class MenuCompanions extends PuzzlesLib {

    public static final String MODID = "menucompanions";
    public static final String NAME = "Menu Companions";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    public MenuCompanions() {

        super();
        MenuCompanionsElements.setup(MODID);
        ConfigManager.builder().moveToFolder(MODID);
        ConfigManager.get().load();
        this.setClientSideOnly();
    }

}