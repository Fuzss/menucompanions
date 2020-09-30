package com.fuzs.menucompanions.client.storage;

import com.fuzs.menucompanions.MenuCompanions;
import com.fuzs.menucompanions.client.handler.MenuEntityHandler;
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
    private byte data = 7;
    private float scale = 1.0F;
    private int xOffset = 0;
    private int yOffset = 0;
    private boolean nameplate = false;
    private boolean particles = true;
    private int weight = 1;
    private MenuEntityHandler.MenuSide side = MenuEntityHandler.MenuSide.BOTH;
    private String profile = "";
    private byte modelParts = 127;
    private boolean crouching = false;

    public EntityMenuEntry build() {

        CompoundNBT compound = type != null ? IEntrySerializer.deserializeNbt(this.nbt, this.type) : new CompoundNBT();
        this.weight = Math.max(1, this.weight);
        if (this.type != null) {

            this.scale = this.scale != 1.0F ? this.scale : EntityMenuEntry.getScale(this.type.getWidth(), this.type.getHeight());
        } else {

            this.scale = 1.0F;
            this.xOffset = 0;
            this.yOffset = 0;
        }

        if (this.type == EntityType.PLAYER) {

            return new PlayerMenuEntry(this.type, compound, this.data, this.scale, this.xOffset, this.yOffset, this.nameplate, this.particles, this.weight, this.side, this.profile, this.modelParts, this.crouching);
        }

        return new EntityMenuEntry(this.type, compound, this.data, this.scale, this.xOffset, this.yOffset, this.nameplate, this.particles, this.weight, this.side);
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

    private MenuEntryBuilder setSide(MenuEntityHandler.MenuSide side) {

        this.side = side;
        return this;
    }

    public MenuEntryBuilder setLeft() {

        this.side = MenuEntityHandler.MenuSide.LEFT;
        return this;
    }

    public MenuEntryBuilder setRight() {

        this.side = MenuEntityHandler.MenuSide.RIGHT;
        return this;
    }

    private MenuEntryBuilder setCrouching(boolean crouching) {

        this.crouching = crouching;
        return this;
    }

    public MenuEntryBuilder setProfile(String profile) {

        this.profile = profile;
        return this;
    }

    public MenuEntryBuilder setModelParts(byte modelParts) {

        this.modelParts = modelParts;
        return this;
    }

    public MenuEntryBuilder setCrouching() {

        this.crouching = true;
        return this;
    }

    @Nullable
    public static EntityMenuEntry deserialize(@Nullable JsonElement element) {

        if (element != null && element.isJsonObject()) {

            MenuEntryBuilder builder = new MenuEntryBuilder();
            JsonObject jsonobject = JSONUtils.getJsonObject(element, "mob_entry");
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
            builder.setSide(IEntrySerializer.deserializeEnum(displayobject, "side", MenuEntityHandler.MenuSide.class, MenuEntityHandler.MenuSide.BOTH));
            builder.setData(IEntrySerializer.deserializeEnumProperties(dataobject, EntityMenuEntry.PropertyFlags.class, EntityMenuEntry.PropertyFlags::toString, EntityMenuEntry.PropertyFlags::getPropertyMask));
            if (type != null) {

                builder.setScale(JSONUtils.getFloat(displayobject, "scale"));
                builder.setXOffset(JSONUtils.getInt(displayobject, "x_offset"));
                builder.setYOffset(JSONUtils.getInt(displayobject, "y_offset"));
                builder.setNbt(JSONUtils.getString(dataobject, "nbt"));

                if (type == EntityType.PLAYER) {

                    JsonObject playerobject = JSONUtils.getJsonObject(jsonobject, EntityMenuEntry.PLAYER_NAME);
                    JsonObject modelobject = JSONUtils.getJsonObject(playerobject, EntityMenuEntry.MODEL_NAME);
                    builder.setProfile(JSONUtils.getString(playerobject, "profile"));
                    builder.setCrouching(JSONUtils.getBoolean(playerobject, "crouching"));
                    builder.setModelParts(IEntrySerializer.deserializeEnumProperties(modelobject, PlayerModelPart.class, PlayerModelPart::getPartName, PlayerModelPart::getPartMask));
                }
            }

            return builder.build();
        }

        return null;
    }

}
