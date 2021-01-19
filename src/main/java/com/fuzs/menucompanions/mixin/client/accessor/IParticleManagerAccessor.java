package com.fuzs.menucompanions.mixin.client.accessor;

import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ParticleManager.class)
public interface IParticleManagerAccessor {

    @Accessor
    Map<ResourceLocation, IParticleFactory<?>> getFactories();

}
