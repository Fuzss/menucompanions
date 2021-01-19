package com.fuzs.menucompanions.client.storage;

import com.fuzs.menucompanions.client.element.MenuEntityElement;
import com.fuzs.menucompanions.config.JSONConfigUtil;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.entity.EntityType;
import net.minecraft.util.JSONUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MenuEntityProvider {

    private static final List<EntityMenuEntry> DEFAULT_MENU_ENTRIES = Lists.newArrayList(

            new MenuEntryBuilder().setType(EntityType.PLAYER).setRight().setWeight(31).renderName().build(),
            new MenuEntryBuilder().setType(EntityType.PLAYER).setRight().renderName().setNbt("{ArmorItems:[{Count:1,id:netherite_boots},{Count:1,id:netherite_leggings},{Count:1,id:netherite_chestplate},{Count:1,id:netherite_helmet}]}").build(),
            new MenuEntryBuilder().setType(EntityType.ZOMBIFIED_PIGLIN).setLeft().setWeight(13).build(),
            new MenuEntryBuilder().setType(EntityType.ZOMBIFIED_PIGLIN).setLeft().setWeight(2).setNbt("{IsBaby:1,HandItems:[{Count:1,id:golden_sword},{}]}").build(),
            new MenuEntryBuilder().setType(EntityType.CHICKEN).setLeft().setNbt("{Passengers:[{id:zombified_piglin,IsBaby:1,HandItems:[{Count:1,id:golden_sword},{}]}]}").build(),
            new MenuEntryBuilder().setType(EntityType.GHAST).setLeft().setWeight(10).setYOffset(24).setScale(0.4F).build(),
            new MenuEntryBuilder().setType(EntityType.MAGMA_CUBE).setLeft().setWeight(5).setNbt("{Size:0}").build(),
            new MenuEntryBuilder().setType(EntityType.MAGMA_CUBE).setLeft().setWeight(5).setNbt("{Size:1}").build(),
            new MenuEntryBuilder().setType(EntityType.MAGMA_CUBE).setLeft().setWeight(5).setNbt("{Size:3}").build(),
            new MenuEntryBuilder().setType(EntityType.ENDERMAN).setLeft().setWeight(10).build(),
            new MenuEntryBuilder().setType(EntityType.ENDERMAN).setLeft().setNbt("{carriedBlockState:{Name:crimson_fungus}}").build(),
            new MenuEntryBuilder().setType(EntityType.ENDERMAN).setLeft().setNbt("{carriedBlockState:{Name:warped_fungus}}").build(),
            new MenuEntryBuilder().setType(EntityType.ENDERMAN).setLeft().setNbt("{carriedBlockState:{Name:crimson_roots}}").build(),
            new MenuEntryBuilder().setType(EntityType.ENDERMAN).setLeft().setNbt("{carriedBlockState:{Name:warped_roots}}").build(),
            new MenuEntryBuilder().setType(EntityType.ENDERMAN).setLeft().setNbt("{carriedBlockState:{Name:crimson_nylium}}").build(),
            new MenuEntryBuilder().setType(EntityType.ENDERMAN).setLeft().setNbt("{carriedBlockState:{Name:warped_nylium}}").build(),
            new MenuEntryBuilder().setType(EntityType.BLAZE).setLeft().setWeight(16).build(),
            new MenuEntryBuilder().setType(EntityType.WITHER_SKELETON).setLeft().setWeight(13).build(),
            new MenuEntryBuilder().setType(EntityType.SKELETON).setLeft().setWeight(3).build(),
            new MenuEntryBuilder().setType(EntityType.PIGLIN).setLeft().setWeight(13).build(),
            new MenuEntryBuilder().setType(EntityType.PIGLIN).setLeft().setWeight(3).setNbt("{IsBaby:1}").build(),
            new MenuEntryBuilder().setType(EntityType.field_242287_aj).setLeft().setWeight(6).build(),
            new MenuEntryBuilder().setType(EntityType.HOGLIN).setLeft().setWeight(12).build(),
            new MenuEntryBuilder().setType(EntityType.HOGLIN).setLeft().setNbt("{Age:-24000}").build(),
            new MenuEntryBuilder().setType(EntityType.HOGLIN).setLeft().setNbt("{Age:-24000,Passengers:[{id:piglin,IsBaby:1}]}").build(),
            new MenuEntryBuilder().setType(EntityType.HOGLIN).setLeft().setNbt("{Age:-24000,Passengers:[{id:piglin,IsBaby:1,Passengers:[{id:piglin,IsBaby:1}]}]}").build(),
            new MenuEntryBuilder().setType(EntityType.HOGLIN).setLeft().setNbt("{Age:-24000,Passengers:[{id:piglin,IsBaby:1,Passengers:[{id:piglin,IsBaby:1,Passengers:[{id:piglin,IsBaby:1}]}]}]}").build(),
            new MenuEntryBuilder().setType(EntityType.ZOGLIN).setLeft().build(),
            new MenuEntryBuilder().setType(EntityType.STRIDER).setLeft().setWeight(10).build(),
            new MenuEntryBuilder().setType(EntityType.STRIDER).setLeft().setWeight(2).setNbt("{Age:-24000}").build(),
            new MenuEntryBuilder().setType(EntityType.STRIDER).setLeft().setWeight(3).setNbt("{Passengers:[{id:strider,Age:-24000}]}").build(),
            new MenuEntryBuilder().setType(EntityType.STRIDER).setLeft().setWeight(1).setNbt("{Saddle:1,Passengers:[{id:zombified_piglin,HandItems:[{Count:1,id:warped_fungus_on_a_stick},{}]}]}").build()
    );

    private static final int FILE_FORMAT = 1;
    private static final List<EntityMenuEntry> MENU_ENTRIES = Lists.newArrayList();

    @Nullable
    public static EntityMenuEntry getRandomEntry(MenuEntityElement.MenuSide side) {

        List<EntityMenuEntry> sidedEntries = MENU_ENTRIES.stream().filter(entry -> entry.isSide(side)).collect(Collectors.toList());
        if (sidedEntries.isEmpty()) {

            return null;
        }

        int weight = (int) (sidedEntries.stream().mapToInt(EntityMenuEntry::getWeight).sum() * Math.random());
        Collections.shuffle(sidedEntries);
        for (EntityMenuEntry entry : sidedEntries) {

            weight -= entry.getWeight();
            if (weight <= 0) {

                return entry;
            }
        }

        return null;
    }

    public static void serialize(String jsonName, File jsonFile) {

        JsonArray jsonarray = new JsonArray();
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("__comment", "For documentation check the project page on CurseForge.");
        jsonobject.addProperty("file_format", FILE_FORMAT);
        jsonarray.add(jsonobject);
        DEFAULT_MENU_ENTRIES.forEach(entry -> jsonarray.add(entry.serialize()));
        JSONConfigUtil.saveToFile(jsonName, jsonFile, jsonarray);
    }

    public static void deserialize(FileReader reader) {

        MENU_ENTRIES.clear();
        JsonElement[] elements = JSONConfigUtil.GSON.fromJson(reader, JsonElement[].class);
        int version = 0;
        for (JsonElement jsonelement : elements) {

            if (jsonelement != null && jsonelement.isJsonObject()) {

                JsonObject jsonobject = jsonelement.getAsJsonObject();
                if (jsonobject.has("file_format")) {

                    version = JSONUtils.getInt(jsonobject, "file_format");
                    break;
                }
            }
        }

        for (JsonElement jsonelement : elements) {

            if (jsonelement != null && jsonelement.isJsonObject()) {

                JsonObject jsonobject = jsonelement.getAsJsonObject();
                if (jsonobject.has("id")) {

                    Optional.ofNullable(MenuEntryBuilder.deserialize(jsonelement, version)).ifPresent(MENU_ENTRIES::add);
                }
            }
        }
    }

}
