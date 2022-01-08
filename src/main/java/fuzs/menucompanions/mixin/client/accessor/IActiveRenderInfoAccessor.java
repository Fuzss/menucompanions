package fuzs.menucompanions.mixin.client.accessor;

import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ActiveRenderInfo.class)
public interface IActiveRenderInfoAccessor {

    @Invoker
    void callSetPosition(Vector3d posIn);

    @Invoker
    void callSetDirection(float pitchIn, float yawIn);

}
