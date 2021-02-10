package com.fuzs.menucompanions.mixin.client.accessor;

import net.minecraft.entity.Entity;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface IEntityAccessor {

    @Accessor
    void setInWater(boolean inWater);

}
