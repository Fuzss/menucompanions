package com.fuzs.menucompanions.mixin;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class MenuCompanionsConnector implements IMixinConnector {

    @Override
    public void connect() {

        Mixins.addConfiguration("META-INF/mixins.json");
    }

}
