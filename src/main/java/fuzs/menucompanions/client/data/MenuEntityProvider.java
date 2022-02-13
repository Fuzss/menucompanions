package fuzs.menucompanions.client.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.gson.JsonElement;
import fuzs.menucompanions.MenuCompanions;
import fuzs.menucompanions.client.data.entry.MobMenuData;
import fuzs.menucompanions.config.ClientConfig;
import fuzs.puzzleslib.json.JsonConfigFileUtil;
import fuzs.puzzleslib.util.PuzzlesUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MenuEntityProvider {
    private static final List<MobMenuData> MENU_ENTRIES = Lists.newArrayList();
    private static final List<MobMenuData> DEFAULT_MENU_ENTRIES = Lists.newArrayList(
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
            new MenuEntryBuilder().setType(EntityType.PIGLIN_BRUTE).setLeft().setWeight(6).build(),
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

    @Nullable
    public static MobMenuData getRandomEntry(ClientConfig.MenuSide side) {
        List<MobMenuData> sidedEntries = MENU_ENTRIES.stream().filter(entry -> entry.validSide(side)).collect(Collectors.toList());
        return PuzzlesUtil.getRandomEntry(sidedEntries, MobMenuData::getWeight);
    }

    public static void removeEntry(MobMenuData entry) {
        MENU_ENTRIES.remove(entry);
    }

    public static void serialize(File jsonFile) {
        serializeDataMap(jsonFile, getMobDataMap());
    }

    @SuppressWarnings("ConstantConditions")
    private static Multimap<String, MobMenuData> getMobDataMap() {
        // sort directly by path and not by entity type in case entities from different mods have the same name
        if (MenuCompanions.CONFIG.client().defaultMobs.isEmpty()) {
            return Multimaps.index(DEFAULT_MENU_ENTRIES, entry -> ForgeRegistries.ENTITIES.getKey(entry.getRawType()).getPath());
        }
        return MenuCompanions.CONFIG.client().defaultMobs.stream()
                .map(type -> new MenuEntryBuilder().setType(type).build())
                .collect(Multimaps.toMultimap(entry -> ForgeRegistries.ENTITIES.getKey(entry.getRawType()).getPath(), Function.identity(), HashMultimap::create));
    }

    private static void serializeDataMap(File jsonFile, Multimap<String, MobMenuData> entityIdToData) {
        for (Map.Entry<String, Collection<MobMenuData>> entry : entityIdToData.asMap().entrySet()) {
            int index = 0;
            for (MobMenuData data : entry.getValue()) {
                String fileName = String.format("%s_%02d.json", entry.getKey(), index++);
                File file = new File(jsonFile, String.join(File.separator, entry.getKey(), fileName));
                JsonConfigFileUtil.saveToFile(file, data.serialize());
            }
        }
    }

    public static void deserialize(FileReader reader) throws NullPointerException {
        JsonElement jsonelement = JsonConfigFileUtil.GSON.fromJson(reader, JsonElement.class);
        MobMenuData deserialize = MenuEntryBuilder.deserialize(jsonelement);
        if (deserialize == null) {
            throw new NullPointerException("Couldn't deserialize file");
        }
        MENU_ENTRIES.add(deserialize);
    }

    public static void clear() {
        MENU_ENTRIES.clear();
    }
}
