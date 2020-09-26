package com.fuzs.menucompanions.client.handler;

import com.fuzs.menucompanions.client.util.MenuEntityEntry;
import com.fuzs.menucompanions.config.JSONConfigUtil;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.entity.EntityType;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MenuEntityProvider {

    private static final List<MenuEntityEntry> DEFAULT_MENU_ENTRIES = Lists.newArrayList(
            new MenuEntityEntry(EntityType.BOAT, "{Passengers:[{id:enderman}]}"),
            new MenuEntityEntry(EntityType.PLAYER),
//            new MenuEntityEntry(EntityType.ZOMBIE, "{HandItems:[{Count:1,id:diamond_sword},{Count:1,id:shield}],ArmorItems:[{Count:1,id:diamond_boots},{Count:1,id:diamond_leggings},{Count:1,id:diamond_chestplate},{Count:1,id:diamond_helmet}]}"),
//            new MenuEntityEntry(EntityType.SPIDER, "{Passengers:[{id:skeleton,HandItems:[{Count:1,id:bow},{}]}]}"),
            new MenuEntityEntry(EntityType.SPIDER, "{Passengers:[{id:skeleton,Passengers:[{id:zombie}]}]}"),
            new MenuEntityEntry(EntityType.CHICKEN, "{Passengers:[{id:zombie,IsBaby:1}]}"),
            new MenuEntityEntry(EntityType.STRIDER, "{Saddle:1,Passengers:[{id:zombified_piglin,HandItems:[{Count:1,id:warped_fungus_on_a_stick},{}]}]}")
    );

    private static final List<MenuEntityEntry> MENU_ENTRIES = Lists.newArrayList();

    @Nullable
    public static MenuEntityEntry getRandomEntry(MenuEntityEntry.MenuSide side) {

        List<MenuEntityEntry> sidedEntries = MENU_ENTRIES.stream().filter(entry -> entry.isSide(side)).collect(Collectors.toList());
        if (sidedEntries.isEmpty()) {

            return null;
        }

        int weight = (int) (sidedEntries.stream().mapToInt(MenuEntityEntry::getWeight).sum() * Math.random());
        Collections.shuffle(sidedEntries);
        for (MenuEntityEntry entry : sidedEntries) {

            weight -= entry.getWeight();
            if (weight <= 0) {

                return entry;
            }
        }

        return null;
    }

    public static void serialize(String jsonName, File jsonFile) {

        JsonArray jsonarray = new JsonArray();
        DEFAULT_MENU_ENTRIES.forEach(entry -> jsonarray.add(entry.serialize()));

        JSONConfigUtil.saveToFile(jsonName, jsonFile, jsonarray);
    }

    public static void deserialize(FileReader reader) {

        MENU_ENTRIES.clear();

        Stream.of(JSONConfigUtil.GSON.fromJson(reader, JsonElement[].class))
                .forEach(element -> MENU_ENTRIES.add(MenuEntityEntry.deserialize(element)));
    }

}
