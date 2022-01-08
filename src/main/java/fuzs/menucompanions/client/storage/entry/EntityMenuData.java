package fuzs.menucompanions.client.storage.entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fuzs.menucompanions.client.util.CreateEntityUtil;
import fuzs.menucompanions.client.util.EntrySerializer;
import fuzs.menucompanions.client.world.MenuClientWorld;
import fuzs.menucompanions.config.ClientConfig;
import fuzs.menucompanions.mixin.client.accessor.EntityAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class EntityMenuData {
    public static final String DISPLAY_FLAG = "display";
    public static final String DATA_FLAG = "data";
    public static final String PLAYER_FLAG = "player";
    public static final String MODEL_FLAG = "model";

    @Nullable
    private final EntityType<?> type;
    protected final CompoundTag compound;
    protected final byte data;
    private final float scale;
    private final int xOffset;
    private final int yOffset;
    private final boolean nameplate;
    private final boolean particles;
    private final int weight;
    private final float volume;
    private final ClientConfig.MenuSide side;

    public EntityMenuData(@Nullable EntityType<?> type, CompoundTag compound, byte data, float scale, int xOffset, int yOffset, boolean nameplate, boolean particles, int weight, float volume, ClientConfig.MenuSide side) {
        this.type = type;
        this.compound = compound;
        this.data = data;
        this.scale = scale;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.nameplate = nameplate;
        this.particles = particles;
        this.weight = weight;
        this.volume = volume;
        this.side = side;
    }

    private boolean isTypeSet() {
        return this.type != null;
    }

    @Nullable
    public EntityType<?> getRawType() {
        return this.type;
    }

    public float calculateScale(Entity entity) {
        if (this.isTypeSet()) {
            return this.scale;
        }
        return calculateScale(entity.getBbWidth(), entity.getBbHeight());
    }

    public static float calculateScale(float width, float height) {
        float minWidth = 1.0F / 2.0F;
        float maxWidth = 3.0F / 1.0F;
        float midWidth = (minWidth + maxWidth) / 2.0F;
        float minHeight = 1.0F / 3.0F;
        float maxHeight = 4.0F / 3.0F;
        float midHeight = (minHeight + maxHeight) / 2.0F;
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

    public float getSoundVolume() {
        return this.volume;
    }

    public boolean validSide(ClientConfig.MenuSide side) {
        return this.side == ClientConfig.MenuSide.BOTH || this.side == side;
    }

    public boolean tick() {
        return MenuPropertyFlags.readProperty(this.data, MenuPropertyFlags.TICK);
    }

    public boolean walking() {
        return MenuPropertyFlags.readProperty(this.data, MenuPropertyFlags.WALK);
    }

    public boolean inLove() {
        return MenuPropertyFlags.readProperty(this.data, MenuPropertyFlags.IN_LOVE);
    }

    @Nullable
    public Entity create(MenuClientWorld worldIn) {
        return CreateEntityUtil.loadEntity(this.getEntityType(), this.compound, worldIn, entity -> {
            entity.setOnGround(MenuPropertyFlags.readProperty(this.data, MenuPropertyFlags.ON_GROUND));
            ((EntityAccessor) entity).setWasTouchingWater(MenuPropertyFlags.readProperty(this.data, MenuPropertyFlags.IN_WATER));
            if (entity instanceof Mob mob && MenuPropertyFlags.readProperty(this.data, MenuPropertyFlags.AGGRESSIVE)) {
                mob.setAggressive(true);
            }
            if (MenuPropertyFlags.readProperty(this.data, MenuPropertyFlags.CROUCH)) {
                entity.setPose(Pose.CROUCHING);
            }
            CreateEntityUtil.onInitialSpawn(entity, worldIn, this.compound.isEmpty());
            return entity;
        });
    }

    private EntityType<?> getEntityType() {

        if (this.type != null) {

            return this.type;
        }

        List<EntityType<?>> types = ForgeRegistries.ENTITIES.getValues().stream()
                .filter(type -> type.getCategory() != MobCategory.MISC).collect(Collectors.toList());

        return types.get((int) (types.size() * Math.random()));
    }

    public JsonElement serialize() {
        JsonObject jsonobject = new JsonObject();
        EntrySerializer.serializeEntityType(jsonobject, this.type);
        jsonobject.addProperty("weight", this.weight);
        jsonobject.add(DISPLAY_FLAG, this.serializeDisplay());
        jsonobject.add(DATA_FLAG, this.serializeData());
        return jsonobject;
    }

    private JsonObject serializeDisplay() {
        JsonObject jsonobject = new JsonObject();
        if (this.isTypeSet()) {
            jsonobject.addProperty("scale", this.scale);
            jsonobject.addProperty("xoffset", this.xOffset);
            jsonobject.addProperty("yoffset", this.yOffset);
        }
        jsonobject.addProperty("nameplate", this.nameplate);
        jsonobject.addProperty("particles", this.particles);
        jsonobject.addProperty("volume", this.volume);
        EntrySerializer.serializeEnum(jsonobject, "side", this.side);
        return jsonobject;
    }

    private JsonObject serializeData() {

        JsonObject jsonobject = new JsonObject();
        if (this.isTypeSet()) {

            jsonobject.addProperty("nbt", EntrySerializer.serializeTag(this.compound));
        }
        EntrySerializer.serializeEnumProperties(jsonobject, MenuPropertyFlags.class, this.data, MenuPropertyFlags::toString, MenuPropertyFlags::getPropertyMask);

        return jsonobject;
    }

}
