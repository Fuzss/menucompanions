package fuzs.menucompanions.client.util;

import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fuzs.menucompanions.MenuCompanions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Function;

public class EntrySerializer {
    public static final String RANDOM = "random";

    public static String serializeTag(CompoundTag tag) {
        return NbtUtils.toPrettyComponent(tag).getString();
    }

    public static CompoundTag deserializeTag(String tag, EntityType<?> type) {
        try {
            if (!tag.isEmpty()) {
                return TagParser.parseTag(tag);
            }
        } catch (CommandSyntaxException e) {
            MenuCompanions.LOGGER.warn("Failed to read nbt for entity type {}", type != null ? type.getDescription().getString() : RANDOM);
        }
        return new CompoundTag();
    }

    public static void serializeEntityType(JsonObject jsonobject, EntityType<?> value) {
        String id = value != null ? Objects.requireNonNull(ForgeRegistries.ENTITIES.getKey(value)).toString() : RANDOM;
        jsonobject.addProperty("id", id);
    }

    @Nullable
    public static EntityType<?> readEntityType(String id) {
        ResourceLocation resourceKey = ResourceLocation.tryParse(id);
        if (resourceKey != null && ForgeRegistries.ENTITIES.containsKey(resourceKey)) {
            return ForgeRegistries.ENTITIES.getValue(resourceKey);
        }
        return null;
    }

    public static <T extends Enum<T>> void serializeEnum(JsonObject jsonobject, String key, T value) {
        jsonobject.addProperty(key, value.name());
    }

    public static <T extends Enum<T>> T deserializeEnum(JsonObject jsonobject, String key, Class<T> clazz, T defaultValue) {
        if (jsonobject.has(key)) {
            try {
                return Enum.valueOf(clazz, GsonHelper.getAsString(jsonobject, key));
            } catch (IllegalArgumentException e) {

                MenuCompanions.LOGGER.error("Unable to deserialize enum value: {}", e.getMessage());
            }
        }
        return defaultValue;
    }

    public static <T extends Enum<T>> void serializeEnumProperties(JsonObject jsonobject, Class<T> clazz, byte data, Function<T, String> key, Function<T, Integer> value) {
        for (T constant : clazz.getEnumConstants()) {
            jsonobject.addProperty(key.apply(constant), (data & value.apply(constant)) == value.apply(constant));
        }
    }

    public static <T extends Enum<T>> int deserializeEnumProperties(JsonObject jsonobject, Class<T> clazz, Function<T, String> key, Function<T, Integer> value) {
        byte data = 0;
        for (T constant : clazz.getEnumConstants()) {
            if (GsonHelper.getAsBoolean(jsonobject, key.apply(constant), false)) {
                data |= value.apply(constant);
            }
        }
        return data;
    }
}
