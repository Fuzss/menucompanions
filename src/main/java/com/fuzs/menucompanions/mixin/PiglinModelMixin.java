package com.fuzs.menucompanions.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.renderer.entity.model.PiglinModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.monster.piglin.AbstractPiglinEntity;
import net.minecraft.entity.monster.piglin.PiglinAction;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("unused")
@Mixin(PiglinModel.class)
public abstract class PiglinModelMixin<T extends MobEntity> extends PlayerModel<T> {

    public PiglinModelMixin(float modelSize, boolean smallArmsIn) {

        super(modelSize, smallArmsIn);
    }

    @Redirect(method = "setRotationAngles(Lnet/minecraft/entity/MobEntity;FFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/monster/piglin/AbstractPiglinEntity;func_234424_eM_()Lnet/minecraft/entity/monster/piglin/PiglinAction;"))
    public PiglinAction getPiglinAction(AbstractPiglinEntity entity) {

        // piglins do otherwise use an item tag not registered yet
        if (Minecraft.getInstance().currentScreen instanceof MainMenuScreen) {

            return entity.isAggressive() && entity.canEquip(Items.CROSSBOW) ? PiglinAction.CROSSBOW_HOLD : PiglinAction.DEFAULT;
        }

        return entity.func_234424_eM_();
    }

}
