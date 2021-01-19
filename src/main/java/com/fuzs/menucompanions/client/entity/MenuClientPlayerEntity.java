package com.fuzs.menucompanions.client.entity;

import com.fuzs.menucompanions.mixin.client.accessor.IEntityAccessor;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.player.RemoteClientPlayerEntity;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.play.server.SPlayerListItemPacket;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.GameType;

import javax.annotation.Nonnull;

public class MenuClientPlayerEntity extends RemoteClientPlayerEntity {

    private NetworkPlayerInfo playerInfo;

    public MenuClientPlayerEntity(ClientWorld world, GameProfile profile) {

        super(world, profile);
    }

    @Override
    protected NetworkPlayerInfo getPlayerInfo() {

        if (this.playerInfo == null) {

            this.playerInfo = new NetworkPlayerInfo(new SPlayerListItemPacket().new AddPlayerData(this.getGameProfile(), 0, GameType.SURVIVAL, null));
        }

        return this.playerInfo;
    }

    @Override
    public boolean isSpectator() {

        return false;
    }

    @Override
    public boolean isCreative() {

        return false;
    }

    @Nonnull
    @Override
    public ITextComponent getName() {

        ITextComponent itextcomponent = this.getCustomName();
        return itextcomponent != null ? IEntityAccessor.unifyStyle(itextcomponent) : super.getName();
    }

}
