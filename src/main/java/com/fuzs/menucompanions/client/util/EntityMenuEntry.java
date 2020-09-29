package com.fuzs.menucompanions.client.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.JSONUtils;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EntityMenuEntry {

    private static final String DISPLAY_NAME = "display";
    private static final String PROPERTIES_NAME = "properties";

    @Nullable
    private final EntityType<?> type;
    private final CompoundNBT compound;
    private final int properties;
    private final float scale;
    private final int xOffset;
    private final int yOffset;
    private final boolean nameplate;
    private final boolean particles;
    private final int weight;
    private final MenuSide side;
    private final String comment;

    private EntityMenuEntry(@Nullable EntityType<?> type, CompoundNBT compound, int properties, float scale, int xOffset, int yOffset, boolean nameplate, boolean particles, int weight, MenuSide side, String comment) {

        this.type = type;
        this.compound = compound;
        this.properties = properties;
        this.scale = scale;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.nameplate = nameplate;
        this.particles = particles;
        this.weight = weight;
        this.side = side;
        this.comment = comment;
    }

    private boolean isTypeSet() {

        return this.type != null;
    }

    public float getScale(Entity entity) {

        if (this.isTypeSet()) {

            return this.scale;
        }

        return getScale(entity.getWidth(), entity.getHeight());
    }

    private static float getScale(float width, float height) {

        final float minWidth = 1.0F / 2.0F;
        final float maxWidth = 3.0F / 1.0F;
        final float midWidth = (minWidth + maxWidth) / 2.0F;
        final float minHeight = 1.0F / 3.0F;
        final float maxHeight = 4.0F / 3.0F;
        final float midHeight = (minHeight + maxHeight) / 2.0F;

        width /= 0.6F;
        height /= 1.8F;

        if (Math.abs(width / midWidth - 1.0F) < Math.abs(height / midHeight - 1.0F)) {

            if (height < minHeight) {

                return minHeight / height;
            } else if (height > maxHeight) {

                return maxHeight / height;
            }
        } else {

            if (width < minWidth) {

                return minWidth / width;
            } else if (width > maxWidth) {

                return maxWidth / width;
            }
        }

        return 1.0F;
    }

    public int getXOffset() {

        return this.xOffset;
    }

    public int getYOffset() {

        return this.yOffset;
    }

    public boolean showNameplate() {

        return this.nameplate;
    }

    public boolean showParticles() {

        return this.particles;
    }

    public int getWeight() {

        return this.weight;
    }

    public boolean isSide(MenuSide side) {

        return this.side == MenuSide.BOTH || this.side == side;
    }

    public boolean isTick() {

        return PropertyFlags.TICK.read(this.properties);
    }

    @Nullable
    public Entity create(World worldIn) {

        return CreateEntityUtil.loadEntity(this.getEntityType(), this.compound, worldIn, this.properties);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private EntityType<?> getEntityType() {

        if (this.type != null) {

            return this.type;
        }

        List<EntityType<?>> types = ForgeRegistries.ENTITIES.getValues().stream()
                .filter(type -> type.getClassification() != EntityClassification.MISC).collect(Collectors.toList());
        Collections.shuffle(types);

        return types.stream().findFirst().get();
    }

    public JsonElement serialize() {

        JsonObject jsonobject = new JsonObject();
        if (!this.comment.isEmpty()) {

            jsonobject.addProperty("__comment", this.comment);
        }

        IEntrySerializer.serializeEntityType(jsonobject, this.type);
        jsonobject.addProperty("weight", this.weight);
        jsonobject.add(DISPLAY_NAME, this.serializeDisplay());
        jsonobject.add(PROPERTIES_NAME, this.serializeProperties());

        return jsonobject;
    }

    private JsonObject serializeDisplay() {

        JsonObject jsonobject = new JsonObject();
        if (this.isTypeSet()) {

            jsonobject.addProperty("scale", this.scale);
            jsonobject.addProperty("xOffset", this.xOffset);
            jsonobject.addProperty("yOffset", this.yOffset);
        }

        jsonobject.addProperty("nameplate", this.nameplate);
        jsonobject.addProperty("particles", this.particles);
        IEntrySerializer.serializeEnum(jsonobject, "side", this.side);

        return jsonobject;
    }

    private JsonObject serializeProperties() {

        JsonObject jsonobject = new JsonObject();
        if (this.isTypeSet()) {

            jsonobject.addProperty("nbt", IEntrySerializer.serializeNBT(this.compound));
        }

        if (!this.isTypeSet() || this.type.getClassification() != EntityClassification.MISC) {

            jsonobject.addProperty("tick", PropertyFlags.TICK.read(this.properties));
        }

        jsonobject.addProperty("onGround", PropertyFlags.ON_GROUND.read(this.properties));
        jsonobject.addProperty("inWater", PropertyFlags.IN_WATER.read(this.properties));

        return jsonobject;
    }

    @Nullable
    public static EntityMenuEntry deserialize(@Nullable JsonElement element) {

        if (element != null && element.isJsonObject()) {

            Builder builder = new Builder();
            JsonObject jsonobject = JSONUtils.getJsonObject(element, "mob_entry");
            JsonObject displayobject = JSONUtils.getJsonObject(jsonobject, DISPLAY_NAME);
            JsonObject propertiesobject = JSONUtils.getJsonObject(jsonobject, PROPERTIES_NAME);

            EntityType<?> id = IEntrySerializer.deserializeEntityType(jsonobject);
            builder.setType(id);
            builder.setWeight(JSONUtils.getInt(jsonobject, "weight"));
            builder.setNameplate(JSONUtils.getBoolean(displayobject, "nameplate"));
            builder.setParticles(JSONUtils.getBoolean(displayobject, "particles"));
            builder.setSide(IEntrySerializer.deserializeEnum(displayobject, "side", MenuSide.class, MenuSide.BOTH));
            builder.setOnGround(JSONUtils.getBoolean(propertiesobject, "onGround"));
            builder.setInWater(JSONUtils.getBoolean(propertiesobject, "inWater"));
            if (id != null) {

                builder.setScale(JSONUtils.getFloat(displayobject, "scale"));
                builder.setXOffset(JSONUtils.getInt(displayobject, "xOffset"));
                builder.setYOffset(JSONUtils.getInt(displayobject, "yOffset"));
                builder.setNbt(JSONUtils.getString(propertiesobject, "nbt"));
            }

            if (id == null || id.getClassification() != EntityClassification.MISC) {

                builder.setTick(JSONUtils.getBoolean(propertiesobject, "tick"));
            }

            return builder.build();
        }

        return null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class Builder {

        private EntityType<?> type = null;
        private String nbt = "";
        private int properties = 7;
        private float scale = 1.0F;
        private int xOffset = 0;
        private int yOffset = 0;
        private boolean nameplate = false;
        private boolean particles = true;
        private int weight = 1;
        private MenuSide side = MenuSide.BOTH;
        private String comment = "";

        public EntityMenuEntry build() {

            CompoundNBT compound = type != null ? IEntrySerializer.deserializeNbt(this.nbt, this.type) : new CompoundNBT();
            this.weight = Math.max(1, this.weight);
            if (this.type != null) {

                this.scale = this.scale != 1.0F ? this.scale : getScale(this.type.getWidth(), this.type.getHeight());
            } else {

                this.scale = 1.0F;
                this.xOffset = 0;
                this.yOffset = 0;
            }

            return new EntityMenuEntry(this.type, compound, this.properties, this.scale, this.xOffset, this.yOffset, this.nameplate, this.particles, this.weight, this.side, this.comment);
        }

        public Builder setType(EntityType<?> type) {

            this.type = type;
            return this;
        }

        public Builder setNbt(String nbt) {

            this.nbt = nbt;
            return this;
        }

        public Builder setTick(boolean tick) {

            this.properties = PropertyFlags.TICK.write(this.properties, tick);
            return this;
        }

        public Builder setOnGround(boolean onGround) {

            this.properties = PropertyFlags.ON_GROUND.write(this.properties, onGround);
            return this;
        }

        public Builder setInWater(boolean inWater) {

            this.properties = PropertyFlags.IN_WATER.write(this.properties, inWater);
            return this;
        }

        public Builder setParticles(boolean particles) {

            this.particles = particles;
            return this;
        }

        public Builder setScale(float scale) {

            this.scale = scale;
            return this;
        }

        public Builder setXOffset(int xOffset) {

            this.xOffset = xOffset;
            return this;
        }

        public Builder setYOffset(int yOffset) {

            this.yOffset = yOffset;
            return this;
        }

        public Builder setNameplate(boolean nameplate) {

            this.nameplate = nameplate;
            return this;
        }

        public Builder setWeight(int weight) {

            this.weight = weight;
            return this;
        }

        private Builder setSide(MenuSide side) {

            this.side = side;
            return this;
        }

        public Builder setLeft() {

            this.side = MenuSide.LEFT;
            return this;
        }

        public Builder setRight() {

            this.side = MenuSide.RIGHT;
            return this;
        }

        public Builder setComment(String comment) {

            this.comment = comment;
            return this;
        }

    }

    public enum PropertyFlags {

        TICK,
        ON_GROUND,
        IN_WATER;

        private final int identifier = 1 << this.ordinal();

        public int write(int data, boolean value) {

            // delete byte before writing
            data &= ~this.identifier;
            return value ? data | this.identifier : data;
        }

        public boolean read(int data) {

            return (data & this.identifier) == this.identifier;
        }

    }

}
