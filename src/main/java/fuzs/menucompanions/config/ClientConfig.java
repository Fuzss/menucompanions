package fuzs.menucompanions.config;

import fuzs.menucompanions.client.handler.MenuEntityBlacklist;
import fuzs.menucompanions.client.handler.MenuEntityHandler;
import fuzs.menucompanions.client.util.ReloadMode;
import fuzs.puzzleslib.config.AbstractConfig;
import fuzs.puzzleslib.config.ConfigHolder;
import fuzs.puzzleslib.config.serialization.EntryCollectionBuilder;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;

public class ClientConfig extends AbstractConfig {
    public int displayTime;
    public int entitySize;
    // 0: left side x offset, 1: left side y offset, 2: right side x offset, 3: right side y offset
    public final int[] menuButtonOffsets = new int[4];
    // 0: button reload x offset, 1: button reload y offset
    public final int[] reloadButtonOffsets = new int[2];
    public boolean playAmbientSounds;
    public boolean hurtEntity;
    private List<String> entityBlacklistRaw;
    public ReloadMode reloadMode;
    private MenuSide entitySide;

    public Set<EntityType<?>> entityBlacklist;

    public ClientConfig() {
        super("");
    }

    @Override
    public void addToBuilder(ForgeConfigSpec.Builder builder, ConfigHolder.ConfigCallback saveCallback) {
        saveCallback.accept(builder.comment("Time in seconds an entity will be shown for. Set to 0 to never change entities.").defineInRange("display_time", 0, 0, Integer.MAX_VALUE), v -> this.displayTime = v * 20);
        saveCallback.accept(builder.comment("Size of menu companions.").defineInRange("entity_size", 60, 0, Integer.MAX_VALUE), v -> this.entitySize = v);
        saveCallback.accept(builder.comment("Offset on x-axis from original position on left side.").defineInRange("left_x_offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.menuButtonOffsets[0] = v);
        saveCallback.accept(builder.comment("Offset on y-axis from original position on left side.").defineInRange("left_y_offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.menuButtonOffsets[1] = v);
        saveCallback.accept(builder.comment("Offset on x-axis from original position on right side.").defineInRange("right_x_offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.menuButtonOffsets[2] = v);
        saveCallback.accept(builder.comment("Offset on y-axis from original position on right side.").defineInRange("right_y_offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.menuButtonOffsets[3] = v);
        saveCallback.accept(builder.comment("Play ambient sounds when clicking on menu mobs.").define("play_sounds", true), v -> this.playAmbientSounds = v);
        saveCallback.accept(builder.comment("Hurt entity when clicked and there is no ambient sound to play.").define("hurt_entity", false), v -> this.hurtEntity = v);
        final ForgeConfigSpec.ConfigValue<List<String>> entityBlacklist = builder.comment("Blacklist to prevent certain entities form rendering. Problematic entities will be added automatically upon being detected.").define("entity_blacklist", EntryCollectionBuilder.getKeyList(ForgeRegistries.ENTITIES, EntityType.ENDER_DRAGON, EntityType.EVOKER_FANGS, EntityType.FALLING_BLOCK, EntityType.AREA_EFFECT_CLOUD, EntityType.ITEM, EntityType.FISHING_BOBBER));
        MenuEntityBlacklist.setEntityBlacklist(entityBlacklist);
        saveCallback.accept(entityBlacklist, v -> this.entityBlacklistRaw = v);
        saveCallback.accept(builder.comment("When to show reload button on main menu. By default requires the control key to be pressed.").defineEnum("reload_button", ReloadMode.RIGHT_ALWAYS), v -> this.reloadMode = v);
        saveCallback.accept(builder.comment("Reload button offset on x-axis from original position.").defineInRange("reload_x_offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.menuButtonOffsets[4] = v);
        saveCallback.accept(builder.comment("Reload button offset on y-axis from original position.").defineInRange("reload_y_offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.menuButtonOffsets[5] = v);
        saveCallback.accept(builder.comment("Choose which side menu companions can be shown at.").defineEnum("entity_side", ClientConfig.MenuSide.BOTH), v -> this.entitySide = v);
    }

    @Override
    protected void afterConfigReload() {
        this.entityBlacklist = EntryCollectionBuilder.of(ForgeRegistries.ENTITIES).buildSet(this.entityBlacklistRaw);
        MenuEntityHandler.INSTANCE.enableEntityRenderer(MenuSide.LEFT, this.entitySide != MenuSide.RIGHT);
        MenuEntityHandler.INSTANCE.enableEntityRenderer(MenuSide.RIGHT, this.entitySide != MenuSide.LEFT);
    }

    public enum MenuSide {
        LEFT, RIGHT, BOTH;

        public static MenuSide[] sides() {
            return new MenuSide[]{LEFT, RIGHT};
        }
    }
}
