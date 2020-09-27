package com.fuzs.menucompanions.client.handler;

import com.fuzs.menucompanions.client.util.EntityMenuEntry;
import com.fuzs.menucompanions.client.util.MenuSide;
import com.fuzs.menucompanions.config.JSONConfigUtil;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.entity.EntityType;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class MenuEntityProvider {

    private static final List<EntityMenuEntry> DEFAULT_MENU_ENTRIES = Lists.newArrayList(
            new EntityMenuEntry.Builder().setType(null).setComment("this is a random entry").build(),
            new EntityMenuEntry.Builder().setType(EntityType.WITHER_SKELETON).setNbt("{CustomName:'{\"text\":\"Betsy\"}'}").setNameplate(true).build(),
//            new EntityMenuEntry.Builder().setType(EntityType.IRON_GOLEM).build(),
//            new EntityMenuEntry.Builder().setType(EntityType.WOLF).setNbt("{Sitting:1,Owner:0}").build(),
//            new EntityMenuEntry.Builder().setType(EntityType.CREEPER).setNbt("{powered:1}").build(),
//            new EntityMenuEntry.Builder().setType(EntityType.BOAT).setNbt("{Passengers:[{id:enderman,carriedBlockState:{Name:grass_block}}]}").build(),
//            new EntityMenuEntry.Builder().setType(EntityType.ZOMBIE).setNbt("{HandItems:[{Count:1,id:diamond_sword},{Count:1,id:shield}],ArmorItems:[{Count:1,id:diamond_boots},{Count:1,id:diamond_leggings},{Count:1,id:diamond_chestplate},{Count:1,id:diamond_helmet}]}").build(),
//            new EntityMenuEntry.Builder().setType(EntityType.SPIDER).setNbt("{Passengers:[{id:skeleton,HandItems:[{Count:1,id:bow},{}]}]}").build(),
//            new EntityMenuEntry.Builder().setType(EntityType.SPIDER).setNbt("{Passengers:[{id:skeleton,Passengers:[{id:zombie}]}]}").build(),
//            new EntityMenuEntry.Builder().setType(EntityType.CHICKEN).setNbt("{Passengers:[{id:zombie,IsBaby:1}]}").build(),
            new EntityMenuEntry.Builder().setType(EntityType.STRIDER).setNbt("{Saddle:1,Passengers:[{id:zombified_piglin,HandItems:[{Count:1,id:warped_fungus_on_a_stick},{}]}]}").build()
    );

    private static final List<EntityMenuEntry> MENU_ENTRIES = Lists.newArrayList();

    @Nullable
    public static EntityMenuEntry getRandomEntry(MenuSide side) {

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

        throw new RuntimeException("Unreachable statement");
    }

    public static void serialize(String jsonName, File jsonFile) {

        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("__comment", "Here is a guide on how to do stuff");
        JsonArray jsonarray = new JsonArray();
        DEFAULT_MENU_ENTRIES.forEach(entry -> jsonarray.add(entry.serialize()));

        JsonArray mainarray = new JsonArray();
        mainarray.add(jsonobject);
        mainarray.add(jsonarray);
        JSONConfigUtil.saveToFile(jsonName, jsonFile, mainarray);
    }

    public static void deserialize(FileReader reader) {

        MENU_ENTRIES.clear();

        Stream.of(JSONConfigUtil.GSON.fromJson(reader, JsonElement[].class))
                .filter(element -> element instanceof JsonArray)
                .flatMap(arr -> StreamSupport.stream(((JsonArray) arr).spliterator(), false))
                .forEach(element -> Optional.ofNullable(EntityMenuEntry.deserialize(element)).ifPresent(MENU_ENTRIES::add));
    }

}
