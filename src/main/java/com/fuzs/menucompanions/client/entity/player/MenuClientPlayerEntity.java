package com.fuzs.menucompanions.client.entity.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.player.RemoteClientPlayerEntity;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.play.server.SPlayerListItemPacket;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.GameType;

@SuppressWarnings("NullableProblems")
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

    @Override
    public ITextComponent getName() {

        ITextComponent itextcomponent = this.getCustomName();
        if (itextcomponent != null) {

            ITextComponent itextcomponent1 = itextcomponent.deepCopy();
            removeClickEvents(itextcomponent1);
            return itextcomponent1;
        } else {

            return super.getName();
        }
    }

    @Override
    public ITextComponent getDisplayName() {

        // override for compatibility with Fabrication mod
        return this.getName();
    }

    @SuppressWarnings("ConstantConditions")
    private static void removeClickEvents(ITextComponent itextcomponent) {

        itextcomponent.applyTextStyle((p_213318_0_) -> p_213318_0_.setClickEvent(null)).getSiblings()
                .forEach(MenuClientPlayerEntity::removeClickEvents);
    }

}
