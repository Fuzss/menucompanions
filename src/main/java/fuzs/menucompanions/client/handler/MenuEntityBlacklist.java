package fuzs.menucompanions.client.handler;

import fuzs.menucompanions.MenuCompanions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class MenuEntityBlacklist {
    private static ForgeConfigSpec.ConfigValue<List<String>> entityBlacklist;

    public static void setEntityBlacklist(ForgeConfigSpec.ConfigValue<List<String>> configValue) {
        entityBlacklist = configValue;
    }
    public static boolean isAllowed(EntityType<?> type) {
        return !MenuCompanions.CONFIG.client().entityBlacklist.contains(type);
    }

    public static void addToBlacklist(String type) {
        ResourceLocation location = ResourceLocation.tryParse(type);
        if (location != null && ForgeRegistries.ENTITIES.containsKey(location)) {
            addToBlacklist(ForgeRegistries.ENTITIES.getValue(location));
        }
    }

    public static void addToBlacklist(EntityType<?> type) {
        if (entityBlacklist != null) {
            if (isAllowed(type)) {
                final List<String> list = entityBlacklist.get();
                list.add(type.getRegistryName().toString());
                // TODO check this, might be needed to trigger writing to file
                entityBlacklist.set(list);
            }
        } else {
            MenuCompanions.LOGGER.warn("Unable to modify blacklist, config value not set");
        }
    }
}
