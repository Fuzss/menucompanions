package fuzs.menucompanions.mixin.client.accessor;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Invoker
    void callSetRotation(float pitchIn, float yawIn);

    @Invoker
    void callSetPosition(Vec3 posIn);
}
