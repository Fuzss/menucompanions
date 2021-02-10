package com.fuzs.menucompanions.client.storage;

import com.fuzs.menucompanions.MenuCompanions;
import com.fuzs.menucompanions.client.element.MenuEntityElement;
import com.fuzs.menucompanions.client.util.IEntrySerializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.JSONUtils;

import javax.annotation.Nullable;
import java.util.Locale;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class MenuEntryBuilder {

    private EntityType<?> type = null;
    private String nbt = "";
    private byte data = new PropertyFlag.Builder().add(PropertyFlag.TICK).add(PropertyFlag.IN_WATER).add(PropertyFlag.ON_GROUND).get();
    private float scale = 1.0F;
    private int xOffset = 0;
    private int yOffset = 0;
    private boolean nameplate = false;
    private boolean particles = true;
    private int weight = 1;
    private float volume = 0.5F;
    private MenuEntityElement.MenuSide side = MenuEntityElement.MenuSide.BOTH;
    private String profile = "";
    private byte modelParts = 127;

    public EntityMenuEntry build() {

        CompoundNBT compound = type != null ? IEntrySerializer.deserializeNbt(this.nbt, this.type) : new CompoundNBT();
        this.weight = Math.max(1, this.weight);
        if (this.type != null) {

            if (this.scale <= 0.0F) {

                this.scale = EntityMenuEntry.getScale(this.type.getWidth(), this.type.getHeight());
            }
        } else {

            this.xOffset = 0;
            this.yOffset = 0;
        }

        if (this.type == EntityType.PLAYER) {

            return new PlayerMenuEntry(this.type, compound, this.data, this.scale, this.xOffset, this.yOffset, this.nameplate, this.particles, this.weight, this.volume, this.side, this.profile, this.modelParts);
        }

        return new EntityMenuEntry(this.type, compound, this.data, this.scale, this.xOffset, this.yOffset, this.nameplate, this.particles, this.weight, this.volume, this.side);
    }

    public MenuEntryBuilder setType(EntityType<?> type) {

        this.type = type;
        return this;
    }

    public MenuEntryBuilder setNbt(String nbt) {

        this.nbt = nbt;
        return this;
    }

    public MenuEntryBuilder setData(byte data) {

        this.data = data;
        return this;
    }

    public MenuEntryBuilder setData(PropertyFlag... flags) {

        this.data = new PropertyFlag.Builder().addAll(flags).get();
        return this;
    }

    public MenuEntryBuilder setParticles(boolean particles) {

        this.particles = particles;
        return this;
    }

    public MenuEntryBuilder hideParticles() {

        this.particles = false;
        return this;
    }

    public MenuEntryBuilder setScale(float scale) {

        this.scale = scale;
        return this;
    }

    public MenuEntryBuilder setXOffset(int xOffset) {

        this.xOffset = xOffset;
        return this;
    }

    public MenuEntryBuilder setYOffset(int yOffset) {

        this.yOffset = yOffset;
        return this;
    }

    private MenuEntryBuilder setNameplate(boolean nameplate) {

        this.nameplate = nameplate;
        return this;
    }

    public MenuEntryBuilder renderName() {

        this.nameplate = true;
        return this;
    }

    public MenuEntryBuilder setWeight(int weight) {

        this.weight = weight;
        return this;
    }

    public MenuEntryBuilder setVolume(float volume) {

        this.volume = volume;
        return this;
    }

    private MenuEntryBuilder setSide(MenuEntityElement.MenuSide side) {

        this.side = side;
        return this;
    }

    public MenuEntryBuilder setLeft() {

        this.side = MenuEntityElement.MenuSide.LEFT;
        return this;
    }

    public MenuEntryBuilder setRight() {

        this.side = MenuEntityElement.MenuSide.RIGHT;
        return this;
    }

    public MenuEntryBuilder setProfile(String profile) {

        this.profile = profile;
        return this;
    }

    public MenuEntryBuilder setModelParts(int modelParts) {

        this.modelParts = (byte) modelParts;
        return this;
    }

    @Nullable
    public static EntityMenuEntry deserialize(@Nullable JsonElement element) {

        if (element != null && element.isJsonObject() && element.getAsJsonObject().has("id")) {

            MenuEntryBuilder builder = new MenuEntryBuilder();
            JsonObject jsonobject = element.getAsJsonObject();
            JsonObject displayobject = JSONUtils.getJsonObject(jsonobject, EntityMenuEntry.DISPLAY_NAME);
            JsonObject dataobject = JSONUtils.getJsonObject(jsonobject, EntityMenuEntry.DATA_NAME);

            String id = JSONUtils.getString(jsonobject, "id");
            EntityType<?> type = null;
            if (!id.toLowerCase(Locale.ROOT).equals(IEntrySerializer.RANDOM)) {

                type = IEntrySerializer.readEntityType(id);
                if (type == null) {

                    MenuCompanions.LOGGER.warn("Unable to read entry with id {}", id);
                    return null;
                }
            }

            builder.setType(type);
            builder.setWeight(JSONUtils.getInt(jsonobject, "weight"));
            builder.setNameplate(JSONUtils.getBoolean(displayobject, "nameplate"));
            builder.setParticles(JSONUtils.getBoolean(displayobject, "particles"));
            builder.setVolume(JSONUtils.getFloat(displayobject, "volume"));
            builder.setSide(IEntrySerializer.deserializeEnum(displayobject, "side", MenuEntityElement.MenuSide.class, MenuEntityElement.MenuSide.BOTH));
            builder.setData((byte) IEntrySerializer.deserializeEnumProperties(dataobject, PropertyFlag.class, PropertyFlag::toString, PropertyFlag::getPropertyMask));
            if (type != null) {

                builder.setScale(JSONUtils.getFloat(displayobject, "scale"));
                builder.setXOffset(JSONUtils.getInt(displayobject, "xoffset"));
                builder.setYOffset(JSONUtils.getInt(displayobject, "yoffset"));
                builder.setNbt(JSONUtils.getString(dataobject, "nbt"));

                if (type == EntityType.PLAYER) {

                    JsonObject playerobject = JSONUtils.getJsonObject(jsonobject, EntityMenuEntry.PLAYER_NAME);
                    JsonObject modelobject = JSONUtils.getJsonObject(playerobject, EntityMenuEntry.MODEL_NAME);
                    builder.setProfile(JSONUtils.getString(playerobject, "profile"));
                    builder.setModelParts(IEntrySerializer.deserializeEnumProperties(modelobject, PlayerModelPart.class, PlayerModelPart::getPartName, PlayerModelPart::getPartMask));
                }
            }

            return builder.build();
        }

        return null;
    }

}
