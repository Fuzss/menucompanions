package com.fuzs.menucompanions.client.util;

import com.fuzs.menucompanions.MenuCompanions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Objects;

public class MenuEntityEntry {

    private final EntityType<?> type;
    private final CompoundNBT compound;
    private final boolean nameplate;
    private final int weight;
    private final MenuSide side;

    public MenuEntityEntry(EntityType<?> type) {

        this(type, new CompoundNBT(), false, 1, MenuSide.BOTH);
    }

    public MenuEntityEntry(EntityType<?> type, String compound) {

        this(type, compound, false, 1, MenuSide.BOTH);
    }

    public MenuEntityEntry(EntityType<?> type, String compound, boolean nameplate, int weight, MenuSide side) {

        this(type, deserializeNbt(compound, type), nameplate, weight, side);
    }

    private MenuEntityEntry(EntityType<?> type, CompoundNBT compound, boolean nameplate, int weight, MenuSide side) {

        this.type = type;
        this.compound = compound;
        this.nameplate = nameplate;
        // don't allow negative weight
        this.weight = Math.max(1, weight);
        this.side = side;
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

        return CreateEntityUtil.loadEntity(this.type, this.compound, worldIn);
    }

    @Nullable
    public JsonElement serialize() {

        JsonObject jsonobject = new JsonObject();
        serializeEntityType(jsonobject, this.type);
        jsonobject.addProperty("nbt", serializeNBT(this.compound));
        jsonobject.addProperty("nameplate", this.nameplate);
        jsonobject.addProperty("weight", this.weight);
        serializeEnum(jsonobject, "side", this.side);

        return jsonobject;
    }

    @Nullable
    public static MenuEntityEntry deserialize(@Nullable JsonElement element) {

        if (element != null && element.isJsonObject()) {

            JsonObject jsonobject = JSONUtils.getJsonObject(element, "mob_entry");
            EntityType<?> id = deserializeEntityType(jsonobject);
            if (id == null) {

                return null;
            }

            String nbt = JSONUtils.getString(jsonobject, "nbt");
            boolean nameplate = JSONUtils.getBoolean(jsonobject, "nameplate");
            int weight = JSONUtils.getInt(jsonobject, "weight");
            MenuSide side = deserializeEnum(jsonobject, "side", MenuSide.class, null);
            if (jsonobject.has("name")) {

                String name = JSONUtils.getString(jsonobject, "name");
                return new MenuPlayerEntry(id, nbt, nameplate, weight, side, name);
            }

            return new MenuEntityEntry(id, nbt, nameplate, weight, side);
        }

        return null;
    }

    private static String serializeNBT(CompoundNBT nbt) {

        return nbt.toFormattedComponent().getString();
    }

    private static CompoundNBT deserializeNbt(String nbt, EntityType<?> type) {

        try {

            return new JsonToNBT(new StringReader(nbt)).readStruct();
        } catch (CommandSyntaxException e) {

            MenuCompanions.LOGGER.error("Failed to read nbt for entity type \"" + type.getName() + "\"");
        }

        return new CompoundNBT();
    }

    private static void serializeEntityType(JsonObject jsonobject, EntityType<?> value) {

        jsonobject.addProperty("id", Objects.requireNonNull(ForgeRegistries.ENTITIES.getKey(value)).toString());
    }

    @Nullable
    private static EntityType<?> deserializeEntityType(JsonObject jsonobject) {

        if (jsonobject.has("id")) {

            String id = JSONUtils.getString(jsonobject, "id");
            ResourceLocation resourceKey = ResourceLocation.tryCreate(id);
            if (resourceKey != null && ForgeRegistries.ENTITIES.containsKey(resourceKey)) {

                return ForgeRegistries.ENTITIES.getValue(resourceKey);
            }

            MenuCompanions.LOGGER.error("Failed to read entity type id \"" + id + "\"");
        }

        return null;
    }

    private static <T extends Enum<T>> void serializeEnum(JsonObject jsonobject, String key, T value) {

        jsonobject.addProperty(key, value.name());
    }

    private static <T extends Enum<T>> T deserializeEnum(JsonObject jsonobject, String key, Class<T> clazz, T defaultValue) {

        if (jsonobject.has(key)) {

            try {

                return Enum.valueOf(clazz, JSONUtils.getString(jsonobject, key));
            } catch (IllegalArgumentException e) {

                MenuCompanions.LOGGER.error(e);
            }
        }

        return defaultValue;
    }

    public static class MenuPlayerEntry extends MenuEntityEntry {

        private final String name;

        public MenuPlayerEntry(EntityType<?> type, String nbt, boolean nameplate, int weight, MenuSide side, String name) {

            super(type, nbt, nameplate, weight, side);
            this.name = name;
        }

        @Nullable
        public JsonElement serialize() {

            JsonObject jsonobject = super.serialize().getAsJsonObject();
            jsonobject.addProperty("name", this.name);

            return jsonobject;
        }

    }

    public enum MenuSide {

        BOTH, LEFT, RIGHT
    }

}
