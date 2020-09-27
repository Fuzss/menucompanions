package com.fuzs.menucompanions.client.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EntityMenuEntry {

    @Nullable
    private final EntityType<?> type;
    private final CompoundNBT compound;
    private final float scale;
    private final int xOffset;
    private final int yOffset;
    private final boolean nameplate;
    private final int weight;
    private final MenuSide side;
    private final String comment;

    private EntityMenuEntry(@Nullable EntityType<?> type, CompoundNBT compound, float scale, int xOffset, int yOffset, boolean nameplate, int weight, MenuSide side, String comment) {

        this.type = type;
        this.compound = compound;
        this.scale = scale;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.nameplate = nameplate;
        this.weight = weight;
        this.side = side;
        this.comment = comment;
    }

    private boolean isTypeSet() {

        return this.type != null;
    }

    public Vector3f getRenderVec(Entity entity) {

        return new Vector3f((this.side == MenuSide.RIGHT ? -1 : 1) * this.xOffset, this.yOffset, this.getScale(entity));
    }

    private float getScale(Entity entity) {

        if (this.isTypeSet()) {

            return this.scale;
        }

        return getScale(entity.getHeight());
    }

    private static float getScale(float height) {

        final float min = 0.6F;
        final float max = 2.4F;

        if (height < min) {

            return min / height;
        } else if (height > max) {

            return max / height;
        }

        return 1.0F;
    }

    public boolean showNameplate() {

        return this.nameplate;
    }

    public int getWeight() {

        return this.weight;
    }

    public boolean isSide(MenuSide side) {

        return this.side == MenuSide.BOTH || this.side == side;
    }

    @Nullable
    public Entity create(World worldIn) {

        return CreateEntityUtil.loadEntity(this.getEntityType(), this.compound, worldIn);
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

    @Nullable
    public JsonElement serialize() {

        JsonObject jsonobject = new JsonObject();
        if (!this.comment.isEmpty()) {

            jsonobject.addProperty("__comment", this.comment);
        }

        IEntrySerializer.serializeEntityType(jsonobject, this.type);
        if (this.isTypeSet()) {

            jsonobject.addProperty("nbt", IEntrySerializer.serializeNBT(this.compound));
            jsonobject.addProperty("scale", this.scale);
            jsonobject.addProperty("xOffset", this.xOffset);
            jsonobject.addProperty("yOffset", this.yOffset);
        }

        jsonobject.addProperty("nameplate", this.nameplate);
        jsonobject.addProperty("weight", this.weight);
        IEntrySerializer.serializeEnum(jsonobject, "side", this.side);

        return jsonobject;
    }

    @Nullable
    public static EntityMenuEntry deserialize(@Nullable JsonElement element) {

        if (element != null && element.isJsonObject()) {

            JsonObject jsonobject = JSONUtils.getJsonObject(element, "mob_entry");
            EntityType<?> id = IEntrySerializer.deserializeEntityType(jsonobject);
            boolean nameplate = JSONUtils.getBoolean(jsonobject, "nameplate");
            int weight = JSONUtils.getInt(jsonobject, "weight");
            MenuSide side = IEntrySerializer.deserializeEnum(jsonobject, "side", MenuSide.class, null);

            if (id != null) {

                String nbt = JSONUtils.getString(jsonobject, "nbt");
                float scale = JSONUtils.getFloat(jsonobject, "scale");
                int xOffset = JSONUtils.getInt(jsonobject, "xOffset");
                int yOffset = JSONUtils.getInt(jsonobject, "yOffset");

                return new Builder().setType(id).setNameplate(nameplate).setWeight(weight).setSide(side).setNbt(nbt).setScale(scale).setXOffset(xOffset).setYOffset(yOffset).build();
            }

            return new Builder().setNameplate(nameplate).setWeight(weight).setSide(side).build();
        }

        return null;
    }

    public static class Builder {

        private EntityType<?> type = null;
        private String nbt = "";
        private float scale = 1.0F;
        private int xOffset = 0;
        private int yOffset = 0;
        private boolean nameplate = false;
        private int weight = 1;
        private MenuSide side = MenuSide.BOTH;
        private String comment = "";

        public EntityMenuEntry build() {

            CompoundNBT compound = type != null ? IEntrySerializer.deserializeNbt(this.nbt, this.type) : new CompoundNBT();
            this.scale = this.type != null ? getScale(this.type.getHeight()) : 1.0F;
            this.weight = Math.max(1, this.weight);

            return new EntityMenuEntry(this.type, compound, this.scale, this.xOffset, this.yOffset, this.nameplate, this.weight, this.side, this.comment);
        }

        public Builder setType(EntityType<?> type) {

            this.type = type;
            return this;
        }

        public Builder setNbt(String nbt) {

            this.nbt = nbt;
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

        public Builder setSide(MenuSide side) {

            this.side = side;
            return this;
        }

        public Builder setComment(String comment) {

            this.comment = comment;
            return this;
        }

    }

}
