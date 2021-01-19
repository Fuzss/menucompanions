package com.fuzs.menucompanions.client.storage;

import com.fuzs.menucompanions.client.element.MenuEntityElement;
import com.fuzs.menucompanions.client.util.CreateEntityUtil;
import com.fuzs.menucompanions.client.util.IEntrySerializer;
import com.fuzs.menucompanions.client.world.MenuClientWorld;
import com.fuzs.menucompanions.mixin.client.accessor.IEntityAccessor;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EntityMenuEntry {

    public static final String DISPLAY_NAME = "display";
    public static final String DATA_NAME = "data";
    public static final String PLAYER_NAME = "player";
    public static final String MODEL_NAME = "model";

    @Nullable
    private final EntityType<?> type;
    protected final CompoundNBT compound;
    private final byte data;
    private final float scale;
    private final int xOffset;
    private final int yOffset;
    private final boolean nameplate;
    private final boolean particles;
    private final int weight;
    private final MenuEntityElement.MenuSide side;

    public EntityMenuEntry(@Nullable EntityType<?> type, CompoundNBT compound, byte data, float scale, int xOffset, int yOffset, boolean nameplate, boolean particles, int weight, MenuEntityElement.MenuSide side) {

        this.type = type;
        this.compound = compound;
        this.data = data;
        this.scale = scale;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.nameplate = nameplate;
        this.particles = particles;
        this.weight = weight;
        this.side = side;
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

    public static float getScale(float width, float height) {

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

    public boolean isSide(MenuEntityElement.MenuSide side) {

        return this.side == MenuEntityElement.MenuSide.BOTH || this.side == side;
    }

    public boolean isTick() {

        return this.readProperty(PropertyFlags.TICK);
    }

    public boolean isWalking() {

        return this.readProperty(PropertyFlags.WALKING);
    }

    public boolean isInLove() {

        return this.readProperty(PropertyFlags.IN_LOVE);
    }

    @Nullable
    public Entity create(MenuClientWorld worldIn) {

        return CreateEntityUtil.loadEntity(this.getEntityType(), this.compound, worldIn, entity -> {

            entity.setOnGround(this.readProperty(PropertyFlags.ON_GROUND));
            ((IEntityAccessor) entity).setInWater(this.readProperty(PropertyFlags.IN_WATER));
            if (entity instanceof MobEntity && this.readProperty(PropertyFlags.AGGRESSIVE)) {

                ((MobEntity) entity).setAggroed(true);
            }

            CreateEntityUtil.onInitialSpawn(entity, worldIn, this.compound.isEmpty());

            return entity;
        });
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
        IEntrySerializer.serializeEntityType(jsonobject, this.type);
        jsonobject.addProperty("weight", this.weight);
        jsonobject.add(DISPLAY_NAME, this.serializeDisplay());
        jsonobject.add(DATA_NAME, this.serializeData());

        return jsonobject;
    }

    private JsonObject serializeDisplay() {

        JsonObject jsonobject = new JsonObject();
        if (this.isTypeSet()) {

            jsonobject.addProperty("scale", this.scale);
            jsonobject.addProperty("x_offset", this.xOffset);
            jsonobject.addProperty("y_offset", this.yOffset);
        }

        jsonobject.addProperty("nameplate", this.nameplate);
        jsonobject.addProperty("particles", this.particles);
        IEntrySerializer.serializeEnum(jsonobject, "side", this.side);

        return jsonobject;
    }

    private JsonObject serializeData() {

        JsonObject jsonobject = new JsonObject();
        if (this.isTypeSet()) {

            jsonobject.addProperty("nbt", IEntrySerializer.serializeNBT(this.compound));
        }

        IEntrySerializer.serializeEnumProperties(jsonobject, PropertyFlags.class, this.data, PropertyFlags::toString, PropertyFlags::getPropertyMask);

        return jsonobject;
    }

    protected boolean readProperty(PropertyFlags property) {

        return (this.data & property.getPropertyMask()) == property.getPropertyMask();
    }

}
