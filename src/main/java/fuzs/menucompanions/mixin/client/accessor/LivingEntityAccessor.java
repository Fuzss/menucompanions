package fuzs.menucompanions.mixin.client.accessor;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker
    float callGetSoundVolume();

    @Invoker
    float callGetVoicePitch();

    @Invoker
    SoundEvent callGetHurtSound(DamageSource damageSourceIn);
}
