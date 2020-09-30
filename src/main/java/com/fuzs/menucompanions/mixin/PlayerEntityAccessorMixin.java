package com.fuzs.menucompanions.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.datasync.DataParameter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEntity.class)
public interface PlayerEntityAccessorMixin {

    @Accessor("PLAYER_MODEL_FLAG")
    static DataParameter<Byte> getPlayerModelFlag() {

        throw new IllegalStateException();
    }

}
