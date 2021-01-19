package com.fuzs.menucompanions.mixin.client.accessor;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.datasync.DataParameter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEntity.class)
public interface IPlayerEntityAccessor {

    @Accessor("PLAYER_MODEL_FLAG")
    static DataParameter<Byte> getPlayerModelFlag() {

        throw new IllegalStateException();
    }

}
