package fuzs.menucompanions.mixin.client.accessor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor
    void setWasTouchingWater(boolean wasTouchingWater);

    @Invoker
    static Component callRemoveAction(Component itextcomponent) {
        throw new IllegalStateException();
    }
}
