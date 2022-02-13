package fuzs.menucompanions.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fuzs.menucompanions.MenuCompanions;
import fuzs.menucompanions.data.entry.MobMenuData;
import fuzs.menucompanions.data.entry.MobDataFlag;
import fuzs.menucompanions.data.entry.PlayerMenuData;
import fuzs.menucompanions.util.EntrySerializer;
import fuzs.menucompanions.config.ClientConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerModelPart;

import javax.annotation.Nullable;
import java.util.Locale;

public class MenuEntryBuilder {
    private EntityType<?> type = null;
    private String nbt = "";
    private byte data = new MobDataFlag.Builder().add(MobDataFlag.TICK).add(MobDataFlag.IN_WATER).add(MobDataFlag.ON_GROUND).get();
    private float scale = 1.0F;
    private int xOffset = 0;
    private int yOffset = 0;
    private boolean nameplate = false;
    private boolean particles = true;
    private int weight = 1;
    private float volume = 0.5F;
    private ClientConfig.MenuSide side = ClientConfig.MenuSide.BOTH;
    private String profile = "";
    private byte modelParts = 127;

    public MobMenuData build() {
        CompoundTag compound = this.type != null ? EntrySerializer.deserializeTag(this.nbt, this.type) : new CompoundTag();
        this.weight = Math.max(1, this.weight);
        if (this.type != null) {
            if (this.scale <= 0.0F) {
                this.scale = MobMenuData.calculateScale(this.type.getWidth(), this.type.getHeight());
            }
        } else {
            this.xOffset = 0;
            this.yOffset = 0;
        }
        if (this.type == EntityType.PLAYER) {
            return new PlayerMenuData(this.type, compound, this.data, this.scale, this.xOffset, this.yOffset, this.nameplate, this.particles, this.weight, this.volume, this.side, this.profile, this.modelParts);
        }
        return new MobMenuData(this.type, compound, this.data, this.scale, this.xOffset, this.yOffset, this.nameplate, this.particles, this.weight, this.volume, this.side);
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

    public MenuEntryBuilder setData(MobDataFlag... flags) {
        this.data = new MobDataFlag.Builder().addAll(flags).get();
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

    private MenuEntryBuilder setSide(ClientConfig.MenuSide side) {
        this.side = side;
        return this;
    }

    public MenuEntryBuilder setLeft() {
        this.side = ClientConfig.MenuSide.LEFT;
        return this;
    }

    public MenuEntryBuilder setRight() {
        this.side = ClientConfig.MenuSide.RIGHT;
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
    public static MobMenuData deserialize(@Nullable JsonElement element) {
        if (element != null && element.isJsonObject() && element.getAsJsonObject().has("id")) {
            MenuEntryBuilder builder = new MenuEntryBuilder();
            JsonObject jsonobject = element.getAsJsonObject();
            JsonObject displayobject = GsonHelper.getAsJsonObject(jsonobject, MobMenuData.DISPLAY_FLAG);
            JsonObject dataobject = GsonHelper.getAsJsonObject(jsonobject, MobMenuData.DATA_FLAG);
            String id = GsonHelper.getAsString(jsonobject, "id");
            EntityType<?> type = null;
            if (!id.toLowerCase(Locale.ROOT).equals(EntrySerializer.RANDOM)) {
                type = EntrySerializer.readEntityType(id);
                if (type == null) {
                    MenuCompanions.LOGGER.warn("Unable to read entry with id {}", id);
                    return null;
                }
            }
            builder.setType(type);
            builder.setWeight(GsonHelper.getAsInt(jsonobject, "weight"));
            builder.setNameplate(GsonHelper.getAsBoolean(displayobject, "nameplate"));
            builder.setParticles(GsonHelper.getAsBoolean(displayobject, "particles"));
            builder.setVolume(GsonHelper.getAsFloat(displayobject, "volume"));
            builder.setSide(EntrySerializer.deserializeEnum(displayobject, "side", ClientConfig.MenuSide.class, ClientConfig.MenuSide.BOTH));
            builder.setData((byte) EntrySerializer.deserializeEnumProperties(dataobject, MobDataFlag.class, MobDataFlag::toString, MobDataFlag::getPropertyMask));
            if (type != null) {
                builder.setScale(GsonHelper.getAsFloat(displayobject, "scale"));
                builder.setXOffset(GsonHelper.getAsInt(displayobject, "xoffset"));
                builder.setYOffset(GsonHelper.getAsInt(displayobject, "yoffset"));
                builder.setNbt(GsonHelper.getAsString(dataobject, "nbt"));
                if (type == EntityType.PLAYER) {
                    JsonObject playerobject = GsonHelper.getAsJsonObject(jsonobject, MobMenuData.PLAYER_FLAG);
                    JsonObject modelobject = GsonHelper.getAsJsonObject(playerobject, MobMenuData.MODEL_FLAG);
                    builder.setProfile(GsonHelper.getAsString(playerobject, "profile"));
                    builder.setModelParts(EntrySerializer.deserializeEnumProperties(modelobject, PlayerModelPart.class, PlayerModelPart::getId, PlayerModelPart::getMask));
                }
            }
            return builder.build();
        }
        return null;
    }
}
