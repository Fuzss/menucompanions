package fuzs.menucompanions.config;

import com.google.common.collect.Lists;
import fuzs.menucompanions.client.handler.MenuEntityBlacklist;
import fuzs.menucompanions.client.handler.MenuMobHandler;
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
    public final int[] mobOffsets = new int[4];
    // 0: button reload x offset, 1: button reload y offset
    public final int[] reloadOffsets = new int[2];
    public boolean playAmbientSounds;
    public boolean hurtEntity;
    private List<String> entityBlacklistRaw;
    public ReloadMode reloadMode;
    public MenuSide entitySide;
    private List<String> defaultMobsRaw;
    public boolean onlyPauseScreen;

    public Set<EntityType<?>> entityBlacklist;
    public Set<EntityType<?>> defaultMobs;

    public ClientConfig() {
        super("");
    }

    @Override
    public void addToBuilder(ForgeConfigSpec.Builder builder, ConfigHolder.ConfigCallback saveCallback) {
        saveCallback.accept(builder.comment("Time in seconds a mob will be shown for. Set to 0 to prevent mobs from cycling automatically (they'll only chane when the screen is reopened).").defineInRange("display_time", 0, 0, Integer.MAX_VALUE), v -> this.displayTime = v * 20);
        saveCallback.accept(builder.comment("Size of shown mobs.").defineInRange("entity_size", 60, 0, Integer.MAX_VALUE), v -> this.entitySize = v);
        saveCallback.accept(builder.comment("Offset on x-axis from original position on left side.").defineInRange("left_x_offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.mobOffsets[0] = v);
        saveCallback.accept(builder.comment("Offset on y-axis from original position on left side.").defineInRange("left_y_offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.mobOffsets[1] = v);
        saveCallback.accept(builder.comment("Offset on x-axis from original position on right side.").defineInRange("right_x_offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.mobOffsets[2] = v);
        saveCallback.accept(builder.comment("Offset on y-axis from original position on right side.").defineInRange("right_y_offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.mobOffsets[3] = v);
        saveCallback.accept(builder.comment("Play ambient sounds when clicking on mobs.").define("play_sounds", true), v -> this.playAmbientSounds = v);
        saveCallback.accept(builder.comment("Hurt entity when clicked and there is no ambient sound to play.").define("hurt_entity", false), v -> this.hurtEntity = v);
        final ForgeConfigSpec.ConfigValue<List<String>> entityBlacklist = builder.comment("Blacklist to prevent certain mobs from rendering. Problematic entities will be added automatically upon being detected.", "If a lot of or even all mobs end up here, another mod probably is using incompatible mixins. In that case please report this along with your log to the issue tracker!").define("entity_blacklist", EntryCollectionBuilder.getKeyList(ForgeRegistries.ENTITIES, EntityType.ENDER_DRAGON, EntityType.EVOKER_FANGS, EntityType.FALLING_BLOCK, EntityType.AREA_EFFECT_CLOUD, EntityType.ITEM, EntityType.FISHING_BOBBER));
        MenuEntityBlacklist.setEntityBlacklist(entityBlacklist);
        saveCallback.accept(entityBlacklist, v -> this.entityBlacklistRaw = v);
        saveCallback.accept(builder.comment("Whether to add a menu button for reloading menu companions and where to place it. Mainly useful when configuring new companions.").defineEnum("reload_button", ReloadMode.NO_BUTTON), v -> this.reloadMode = v);
        saveCallback.accept(builder.comment("Reload button offset on x-axis from original position.").defineInRange("reload_x_offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.reloadOffsets[0] = v);
        saveCallback.accept(builder.comment("Reload button offset on y-axis from original position.").defineInRange("reload_y_offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE), v -> this.reloadOffsets[1] = v);
        saveCallback.accept(builder.comment("Choose which side menu companions can be shown at.").defineEnum("entity_side", ClientConfig.MenuSide.BOTH), v -> this.entitySide = v);
        saveCallback.accept(builder.comment("Mobs to initially generate json config files for so their values can be further customized. They will only be generated when no config files are found.", "When this option is left blank, an internal set of default mobs will be generated instead.").define("default_mobs", Lists.<String>newArrayList()),v -> this.defaultMobsRaw = v);
        saveCallback.accept(builder.comment("Only show mobs on pause screen and not within sub menus.", "Mobs can only be shown on sub screens when there is enough room.").define("only_pause_screen", false), v -> this.onlyPauseScreen = v);
    }

    @Override
    protected void afterConfigReload() {
        this.entityBlacklist = EntryCollectionBuilder.of(ForgeRegistries.ENTITIES).buildSet(this.entityBlacklistRaw);
        this.defaultMobs = EntryCollectionBuilder.of(ForgeRegistries.ENTITIES).buildSet(this.defaultMobsRaw);
    }

    public enum ReloadMode {
        NO_BUTTON, RIGHT, LEFT
    }

    public enum MenuSide {
        LEFT, RIGHT, BOTH;

        public int index() {
            return switch (this) {
                case LEFT -> 0;
                case RIGHT -> 1;
                case BOTH -> -1;
            };
        }

        public int offsetsIndex() {
            return switch (this) {
                case LEFT -> 0;
                case RIGHT -> 2;
                case BOTH -> -1;
            };
        }

        public static MenuSide[] sides() {
            return new MenuSide[]{LEFT, RIGHT};
        }
    }
}
