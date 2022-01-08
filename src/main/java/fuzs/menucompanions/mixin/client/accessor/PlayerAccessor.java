package fuzs.menucompanions.mixin.client.accessor;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Player.class)
public interface PlayerAccessor {
    @Accessor("DATA_PLAYER_MODE_CUSTOMISATION")
    static EntityDataAccessor<Byte> getPlayerModelData() {
        throw new IllegalStateException();
    }
}
