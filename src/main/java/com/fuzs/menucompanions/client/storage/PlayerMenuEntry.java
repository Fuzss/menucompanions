package com.fuzs.menucompanions.client.storage;

import com.fuzs.menucompanions.client.element.MenuEntityElement;
import com.fuzs.menucompanions.client.util.CreateEntityUtil;
import com.fuzs.menucompanions.client.util.IEntrySerializer;
import com.fuzs.menucompanions.client.world.MenuClientWorld;
import com.fuzs.menucompanions.mixin.client.accessor.IEntityAccessor;
import com.fuzs.menucompanions.mixin.client.accessor.IPlayerEntityAccessor;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.nbt.CompoundNBT;

import javax.annotation.Nullable;

public class PlayerMenuEntry extends EntityMenuEntry {

    private final String profile;
    private final byte modelParts;
    private final boolean crouching;

    public PlayerMenuEntry(@Nullable EntityType<?> type, CompoundNBT compound, byte data, float scale, int xOffset, int yOffset, boolean nameplate, boolean particles, int weight, MenuEntityElement.MenuSide side, String profile, byte modelParts, boolean crouching) {

        super(type, compound, data, scale, xOffset, yOffset, nameplate, particles, weight, side);
        this.profile = profile;
        this.modelParts = modelParts;
        this.crouching = crouching;
    }

    @Nullable
    public Entity create(MenuClientWorld worldIn) {

        CreateEntityUtil.setGameProfile(this.profile);
        return CreateEntityUtil.loadEntity(EntityType.PLAYER, this.compound, worldIn, entity -> {

            entity.setOnGround(this.readProperty(PropertyFlags.ON_GROUND));
            ((IEntityAccessor) entity).setInWater(this.readProperty(PropertyFlags.IN_WATER));
            CreateEntityUtil.readMobData(entity, this.compound);
            entity.getDataManager().set(IPlayerEntityAccessor.getPlayerModelFlag(), this.modelParts);
            if (this.crouching) {

                entity.setPose(Pose.CROUCHING);
            }

            return entity;
        });
    }

    @Override
    public JsonElement serialize() {

        JsonObject jsonobject = super.serialize().getAsJsonObject();
        jsonobject.add(PLAYER_NAME, this.serializePlayer());

        return jsonobject;
    }

    private JsonObject serializePlayer() {

        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("profile", this.profile);
        jsonobject.addProperty("crouching", this.crouching);
        jsonobject.add(MODEL_NAME, this.serializeModel());

        return jsonobject;
    }

    private JsonObject serializeModel() {

        JsonObject jsonobject = new JsonObject();
        IEntrySerializer.serializeEnumProperties(jsonobject, PlayerModelPart.class, this.modelParts, PlayerModelPart::getPartName, PlayerModelPart::getPartMask);

        return jsonobject;
    }

}
