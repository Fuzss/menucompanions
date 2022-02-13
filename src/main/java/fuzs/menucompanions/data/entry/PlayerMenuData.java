package fuzs.menucompanions.data.entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fuzs.menucompanions.util.EntityFactory;
import fuzs.menucompanions.util.EntrySerializer;
import fuzs.menucompanions.client.multiplayer.MenuClientLevel;
import fuzs.menucompanions.config.ClientConfig;
import fuzs.menucompanions.mixin.client.accessor.EntityAccessor;
import fuzs.menucompanions.mixin.client.accessor.PlayerAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;

import javax.annotation.Nullable;

public class PlayerMenuData extends MobMenuData {
    private final String profile;
    private final byte modelParts;

    public PlayerMenuData(@Nullable EntityType<?> type, CompoundTag compound, byte data, float scale, int xOffset, int yOffset, boolean nameplate, boolean particles, int weight, float volume, ClientConfig.MenuSide side, String profile, byte modelParts) {
        super(type, compound, data, scale, xOffset, yOffset, nameplate, particles, weight, volume, side);
        this.profile = profile;
        this.modelParts = modelParts;
    }

    @Override
    @Nullable
    public Entity create(MenuClientLevel worldIn) {
        EntityFactory.setGameProfile(this.profile);
        return EntityFactory.loadEntity(EntityType.PLAYER, this.compound, worldIn, entity -> {
            entity.setOnGround(MobDataFlag.readProperty(this.data, MobDataFlag.ON_GROUND));
            ((EntityAccessor) entity).setWasTouchingWater(MobDataFlag.readProperty(this.data, MobDataFlag.IN_WATER));
            if (MobDataFlag.readProperty(this.data, MobDataFlag.CROUCH)) {
                entity.setPose(Pose.CROUCHING);
            }
            EntityFactory.readMobData(entity, this.compound);
            entity.getEntityData().set(PlayerAccessor.getPlayerModelData(), this.modelParts);
            return entity;
        });
    }

    @Override
    public JsonElement serialize() {
        JsonObject jsonobject = super.serialize().getAsJsonObject();
        jsonobject.add(PLAYER_FLAG, this.serializePlayer());
        return jsonobject;
    }

    private JsonObject serializePlayer() {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("profile", this.profile);
        jsonobject.add(MODEL_FLAG, this.serializeModel());
        return jsonobject;
    }

    private JsonObject serializeModel() {
        JsonObject jsonobject = new JsonObject();
        EntrySerializer.serializeEnumProperties(jsonobject, PlayerModelPart.class, this.modelParts, PlayerModelPart::getId, PlayerModelPart::getMask);
        return jsonobject;
    }
}
