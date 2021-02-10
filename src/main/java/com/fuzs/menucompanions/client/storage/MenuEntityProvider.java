package com.fuzs.menucompanions.client.storage;

import com.fuzs.menucompanions.client.element.MenuEntityElement;
import com.fuzs.puzzleslib_mc.config.json.JsonConfigFileUtil;
import com.fuzs.puzzleslib_mc.util.PuzzlesLibUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.gson.JsonElement;
import net.minecraft.entity.EntityType;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MenuEntityProvider {

    private static final List<EntityMenuEntry> MENU_ENTRIES = Lists.newArrayList();
    private static final List<EntityMenuEntry> DEFAULT_MENU_ENTRIES = Lists.newArrayList(

            new MenuEntryBuilder().setType(EntityType.PLAYER).setRight().setWeight(31).renderName().build(),
            new MenuEntryBuilder().setType(EntityType.PLAYER).setRight().renderName().setNbt("{ArmorItems:[{Count:1,id:diamond_boots},{Count:1,id:diamond_leggings},{Count:1,id:diamond_chestplate},{Count:1,id:diamond_helmet}]}").build(),
            new MenuEntryBuilder().setType(EntityType.BEE).setLeft().setData(PropertyFlag.TICK).setScale(1.6F).setWeight(8).build(),
            new MenuEntryBuilder().setType(EntityType.BEE).setLeft().setData(PropertyFlag.TICK).setScale(1.6F).setWeight(7).setNbt("{HasNectar:1}").build(),
            new MenuEntryBuilder().setType(EntityType.BEE).setLeft().setData(PropertyFlag.TICK).setScale(1.6F).setNbt("{Anger:1}").build(),
            new MenuEntryBuilder().setType(EntityType.BEE).setLeft().setData(PropertyFlag.TICK).setScale(1.6F).setWeight(2).setNbt("{Age:-24000}").build(),
            new MenuEntryBuilder().setType(EntityType.BEE).setLeft().setData(PropertyFlag.TICK).setScale(1.6F).setWeight(2).setNbt("{Age:-24000,HasNectar:1}").build()
    );

    @Nullable
    public static EntityMenuEntry getRandomEntry(MenuEntityElement.MenuSide side) {

        List<EntityMenuEntry> sidedEntries = MENU_ENTRIES.stream().filter(entry -> entry.isSide(side)).collect(Collectors.toList());

        return PuzzlesLibUtil.getRandomEntry(sidedEntries, EntityMenuEntry::getWeight);
    }

    public static void removeEntry(EntityMenuEntry entry) {

        MENU_ENTRIES.remove(entry);
    }

    @SuppressWarnings("ConstantConditions")
    public static void serialize(File jsonFile) {

        // sort directly by path and not by entity type in case entities from different mods have the same name
        Multimap<String, EntityMenuEntry> defaultsAsMap = Multimaps.index(DEFAULT_MENU_ENTRIES, entry -> entry.getRawType().getRegistryName().getPath());
        for (Map.Entry<String, Collection<EntityMenuEntry>> mapEntry : defaultsAsMap.asMap().entrySet()) {

            int index = 0;
            for (EntityMenuEntry entry : mapEntry.getValue()) {

                String fileName = String.format("%s%d.json", mapEntry.getKey(), ++index);
                File file = new File(jsonFile, String.join(File.separator, mapEntry.getKey(), fileName));
                JsonConfigFileUtil.saveToFile(file, entry.serialize());
            }
        }
    }

    public static void deserialize(FileReader reader) {

        JsonElement jsonelement = JsonConfigFileUtil.GSON.fromJson(reader, JsonElement.class);
        EntityMenuEntry deserialize = MenuEntryBuilder.deserialize(jsonelement);
        if (deserialize != null) {

            MENU_ENTRIES.add(deserialize);
        }
    }

    public static void clear() {

        MENU_ENTRIES.clear();
    }

}
