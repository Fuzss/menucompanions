package com.fuzs.menucompanions.client.util;

import com.fuzs.menucompanions.MenuCompanions;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Objects;

public interface IEntrySerializer {

    String RANDOM = "random";

    static String serializeNBT(CompoundNBT nbt) {

        return nbt.toFormattedComponent().getString();
    }

    static CompoundNBT deserializeNbt(String nbt, EntityType<?> type) {

        try {

            if (!nbt.isEmpty()) {

                return new JsonToNBT(new StringReader(nbt)).readStruct();
            }
        } catch (CommandSyntaxException e) {

            MenuCompanions.LOGGER.warn("Failed to read nbt for entity type {}", type != null ? type.getName().getString() : RANDOM);
        }

        return new CompoundNBT();
    }

    static void serializeEntityType(JsonObject jsonobject, EntityType<?> value) {

        String id = value != null ? Objects.requireNonNull(ForgeRegistries.ENTITIES.getKey(value)).toString() : RANDOM;
        jsonobject.addProperty("id", id);
    }

    @Nullable
    static EntityType<?> deserializeEntityType(JsonObject jsonobject) {

        if (jsonobject.has("id")) {

            String id = JSONUtils.getString(jsonobject, "id");
            if (id.toLowerCase(Locale.ROOT).equals(RANDOM)) {

                return null;
            }

            ResourceLocation resourceKey = ResourceLocation.tryCreate(id);
            if (resourceKey != null && ForgeRegistries.ENTITIES.containsKey(resourceKey)) {

                return ForgeRegistries.ENTITIES.getValue(resourceKey);
            }

            MenuCompanions.LOGGER.warn("Failed to read entity type id {}, assigning random entity type instead", id);
        }

        return null;
    }

    static <T extends Enum<T>> void serializeEnum(JsonObject jsonobject, String key, T value) {

        jsonobject.addProperty(key, value.name());
    }

    static <T extends Enum<T>> T deserializeEnum(JsonObject jsonobject, String key, Class<T> clazz, T defaultValue) {

        if (jsonobject.has(key)) {

            try {

                return Enum.valueOf(clazz, JSONUtils.getString(jsonobject, key));
            } catch (IllegalArgumentException e) {

                MenuCompanions.LOGGER.error("Unable to deserialize enum value: {}", e.getMessage());
            }
        }

        return defaultValue;
    }

}
