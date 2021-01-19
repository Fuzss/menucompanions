package com.fuzs.menucompanions.mixin;

import com.fuzs.menucompanions.MenuCompanions;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

@SuppressWarnings("unused")
public class MixinConnector implements IMixinConnector {

    @Override
    public void connect() {

        Mixins.addConfiguration("META-INF/" + MenuCompanions.MODID + ".mixins.json");
    }

}
