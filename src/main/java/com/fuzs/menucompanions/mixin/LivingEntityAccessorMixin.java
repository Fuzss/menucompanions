package com.fuzs.menucompanions.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessorMixin {

    @Invoker
    float callGetSoundVolume();

    @Invoker
    float callGetSoundPitch();

    @Invoker
    SoundEvent callGetHurtSound(DamageSource damageSourceIn);

}
