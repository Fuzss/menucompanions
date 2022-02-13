package fuzs.menucompanions.client.util;

import com.mojang.authlib.GameProfile;
import fuzs.menucompanions.MenuCompanions;
import fuzs.menucompanions.client.handler.MenuEntityBlacklist;
import fuzs.menucompanions.client.multiplayer.MenuClientLevel;
import fuzs.menucompanions.client.player.MenuPlayer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class EntityFactory {
    private static GameProfile gameProfile;

    @Nullable
    public static Entity loadEntity(EntityType<?> type, CompoundTag compound, MenuClientLevel worldIn, Function<Entity, Entity> mapper) {
        CompoundTag compoundnbt = compound.copy();
        compoundnbt.putString("id", Objects.requireNonNull(ForgeRegistries.ENTITIES.getKey(type)).toString());
        return loadEntityAndExecute(compoundnbt, worldIn, mapper);
    }

    @Nullable
    public static Entity loadEntityAndExecute(CompoundTag compound, MenuClientLevel worldIn, Function<Entity, Entity> mapper) {
        return loadEntity(compound, worldIn).map(mapper).map(entity -> {
            if (compound.contains("Passengers", 9)) {
                ListTag listnbt = compound.getList("Passengers", 10);
                for(int i = 0; i < listnbt.size(); ++i) {
                    Entity passenger = loadEntityAndExecute(listnbt.getCompound(i), worldIn, mapper);
                    if (passenger != null) {
                        passenger.startRiding(entity, true);
                        entity.positionRider(passenger);
                    }
                }
            }
            return entity;
        }).orElse(null);
    }

    private static Optional<Entity> loadEntity(CompoundTag compound, MenuClientLevel worldIn) {
        if (EntityType.by(compound).map(entityType -> {
            return MenuEntityBlacklist.isAllowed(entityType) ? entityType : null;
        }).isPresent()) {
            try {
                return loadEntityUnchecked(compound, worldIn);
            } catch (RuntimeException runtimeexception) {
                MenuCompanions.LOGGER.warn("Exception loading entity: ", runtimeexception);
                MenuEntityBlacklist.addToBlacklist(compound.getString("id"));
            }
        }
        return Optional.empty();
    }

    public static Optional<Entity> loadEntityUnchecked(CompoundTag compound, MenuClientLevel worldIn) {
        return Util.ifElse(EntityType.by(compound)
                .map(entityType -> create(worldIn, entityType)), entity -> {
            try {
                if (entity instanceof NeutralMob) {
                    // world is casted to ServerWorld in IAngerable, so we need to handle those mobs manually
                    readAngerableAdditional(entity, compound);
                } else {
                    entity.load(compound);
                }
            } catch (Exception e) {
                // just enable held items and armor items to show at least in case nothing else works
                if (entity instanceof Mob) {
                    readMobData(entity, compound);
                }
            }
        }, () -> {
            String id = compound.getString("id");
            MenuCompanions.LOGGER.warn("Skipping entity with id {}", id);
            MenuEntityBlacklist.addToBlacklist(id);
        });
    }

    @Nullable
    public static Entity create(MenuClientLevel worldIn, EntityType<?> type) {
        if (type == EntityType.PLAYER) {
            return new MenuPlayer(worldIn, gameProfile);
        }
        return type.create(worldIn);
    }

    public static void setGameProfile(String name) {
        if (StringUtils.isEmpty(name)) {
            gameProfile = Minecraft.getInstance().getUser().getGameProfile();
        } else {
            SkullBlockEntity.updateGameprofile(new GameProfile(null, name), profile -> gameProfile = profile);
        }
    }

    public static void onInitialSpawn(Entity entity, ServerLevelAccessor worldIn, boolean noNbt) {
//        // prevents crash in sprite renderers
//        entity.ticksExisted = 2;
//        // prevents Entity#move from running as it calls a block tag which isn't registered yet
//        entity.noClip = true;
        if (noNbt && entity instanceof Mob mob) {
            try {
                // set difficulty very hard so gear is more likely to appear
                DifficultyInstance difficulty = new DifficultyInstance(Difficulty.HARD, 100000L, 100000, 1.0F);
                mob.finalizeSpawn(worldIn, difficulty, MobSpawnType.COMMAND, null, null);
                if (entity instanceof AgeableMob ageableMob && ageableMob.getRandom().nextFloat() <= 0.05F) {
                    ageableMob.setBaby(true);
                }
            } catch (Exception ignored) {
                // just ignore this as it's not important and doesn't fail too often
            }
        }
    }

    private static void readAngerableAdditional(Entity entity, CompoundTag compound) {
        ((NeutralMob) entity).setRemainingPersistentAngerTime(compound.getInt("AngerTime"));
        if (entity instanceof Mob) {
            readMobData(entity, compound);
        }
        if (entity instanceof EnderMan enderMan) {
            readEndermanData(enderMan, compound);
        } else if (entity instanceof ZombifiedPiglin zombifiedPiglin) {
            zombifiedPiglin.setBaby(compound.getBoolean("IsBaby"));
        } else if (entity instanceof Wolf wolf) {
            readWolfData(wolf, compound);
        }
    }

    public static void readMobData(Entity entity, CompoundTag compound) {
        if (compound.contains("ArmorItems", 9)) {
            ListTag listnbt = compound.getList("ArmorItems", 10);
            Stream.of(EquipmentSlot.values())
                    .filter(slot -> slot.getType() == EquipmentSlot.Type.ARMOR)
                    .forEach(slot -> entity.setItemSlot(slot, ItemStack.of(listnbt.getCompound(slot.getIndex()))));
        }
        if (compound.contains("HandItems", 9)) {
            ListTag listnbt = compound.getList("HandItems", 10);
            Stream.of(EquipmentSlot.values())
                    .filter(slot -> slot.getType() == EquipmentSlot.Type.HAND)
                    .forEach(slot -> entity.setItemSlot(slot, ItemStack.of(listnbt.getCompound(slot.getIndex()))));
        }
    }

    private static void readEndermanData(EnderMan entity, CompoundTag compound) {
        BlockState blockstate = null;
        if (compound.contains("carriedBlockState", 10)) {
            blockstate = NbtUtils.readBlockState(compound.getCompound("carriedBlockState"));
            if (blockstate.isAir()) {
                blockstate = null;
            }
        }
        entity.setCarriedBlock(blockstate);
    }

    private static void readWolfData(Wolf entity, CompoundTag compound) {
        if (compound.contains("CollarColor", 99)) {
            entity.setCollarColor(DyeColor.byId(compound.getInt("CollarColor")));
        }
        entity.setTame(compound.hasUUID("Owner") || !compound.getString("Owner").isEmpty());
        entity.setOrderedToSit(compound.getBoolean("Sitting"));
        entity.setInSittingPose(entity.isOrderedToSit());
    }
}
