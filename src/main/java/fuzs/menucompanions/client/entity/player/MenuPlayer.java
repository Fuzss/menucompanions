package fuzs.menucompanions.client.entity.player;

import com.mojang.authlib.GameProfile;
import fuzs.menucompanions.mixin.client.accessor.EntityAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.world.level.GameType;

public class MenuPlayer extends RemotePlayer {
    private PlayerInfo playerInfo;

    public MenuPlayer(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Override
    protected PlayerInfo getPlayerInfo() {
        if (this.playerInfo == null) {
            this.playerInfo = new PlayerInfo(new ClientboundPlayerInfoPacket.PlayerUpdate(this.getGameProfile(), 0, GameType.SURVIVAL, null));
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
    public Component getName() {
        Component itextcomponent = this.getCustomName();
        return itextcomponent != null ? EntityAccessor.callRemoveAction(itextcomponent) : super.getName();
    }

    @Override
    public Component getDisplayName() {
        // override for compatibility with Fabrication mod
        return this.getName();
    }
}
