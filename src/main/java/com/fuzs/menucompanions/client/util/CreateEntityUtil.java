package com.fuzs.menucompanions.client.util;

import com.fuzs.menucompanions.MenuCompanions;
import com.fuzs.menucompanions.client.handler.MenuEntityHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.entity.monster.ZombifiedPiglinEntity;
import net.minecraft.entity.monster.piglin.PiglinEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class CreateEntityUtil {

    @SuppressWarnings("ConstantConditions")
    @Nullable
    public static Entity loadEntity(EntityType<?> type, CompoundNBT compound, World worldIn, int properties) {

        CompoundNBT compoundnbt = compound.copy();
        compoundnbt.putString("id", Objects.requireNonNull(ForgeRegistries.ENTITIES.getKey(type)).toString());

        return loadEntityAndExecute(compoundnbt, worldIn, entity -> {

            // prevents crash in sprite renderers
            entity.ticksExisted = 2;
            // prevents Entity#move from running as it calls a block tag which isn't registered yet
            entity.noClip = true;
            entity.setOnGround(EntityMenuEntry.PropertyFlags.ON_GROUND.read(properties));
            ObfuscationReflectionHelper.setPrivateValue(Entity.class, entity, EntityMenuEntry.PropertyFlags.IN_WATER.read(properties), "inWater");

            // piglins do otherwise use an item tag not registered yet
            if (entity instanceof PiglinEntity) {

                ((PiglinEntity) entity).func_234442_u_(true);
            }

            if (compound.isEmpty() && entity instanceof MobEntity) {

                try {

                    // set difficulty very hard so gear is more likely to appear
                    DifficultyInstance difficulty = new DifficultyInstance(Difficulty.HARD, 100000L, 100000, 1.0F);
                    ((MobEntity) entity).onInitialSpawn(null, difficulty, SpawnReason.COMMAND, null, null);
                } catch (Exception ignored) {

                    // just ignore this as it's not important and doesn't fail too often
                }
            }

            return entity;
        });
    }

    @Nullable
    public static Entity loadEntityAndExecute(CompoundNBT compound, World worldIn, Function<Entity, Entity> exec) {

        return loadEntity(compound, worldIn).map(exec).map(entity -> {

            if (compound.contains("Passengers", 9)) {

                ListNBT listnbt = compound.getList("Passengers", 10);
                for(int i = 0; i < listnbt.size(); ++i) {

                    Entity passenger = loadEntityAndExecute(listnbt.getCompound(i), worldIn, exec);
                    if (passenger != null) {

                        passenger.startRiding(entity, true);
                    }
                }
            }

            return entity;
        }).orElse(null);
    }

    private static Optional<Entity> loadEntity(CompoundNBT compound, World worldIn) {

        try {

            return loadEntityUnchecked(compound, worldIn);
        } catch (RuntimeException runtimeexception) {

            MenuCompanions.LOGGER.warn("Exception loading entity: ", runtimeexception);
            MenuEntityHandler.addToBlacklist(compound.getString("id"));
            return Optional.empty();
        }
    }

    public static Optional<Entity> loadEntityUnchecked(CompoundNBT compound, World worldIn) {

        return Util.acceptOrElse(EntityType.readEntityType(compound)
                .map(entityType -> MenuEntityHandler.isAllowed(entityType) ? entityType : null)
                .map(entityType -> entityType.create(worldIn)), entity -> {

            try {

                entity.read(compound);
            } catch (Exception e) {

                // world is casted to ServerWorld in IAngerable, so we need to handle those mobs manually
                readAngerableAdditional(entity, compound);
            }
        }, () -> {

            String id = compound.getString("id");
            MenuCompanions.LOGGER.warn("Skipping Entity with id {}", id);
            MenuEntityHandler.addToBlacklist(id);
        });
    }

    @SuppressWarnings("deprecation")
    private static void readAngerableAdditional(Entity entity, CompoundNBT compound) {

        if (entity instanceof MobEntity) {

            readLivingAdditional(entity, compound);
        }

        if (entity instanceof EndermanEntity) {

            BlockState blockstate = null;
            if (compound.contains("carriedBlockState", 10)) {

                blockstate = NBTUtil.readBlockState(compound.getCompound("carriedBlockState"));
                if (blockstate.isAir()) {

                    blockstate = null;
                }
            }

            ((EndermanEntity) entity).setHeldBlockState(blockstate);
        } else if (entity instanceof ZombifiedPiglinEntity) {

            ((ZombifiedPiglinEntity) entity).setChild(compound.getBoolean("IsBaby"));
        } else if (entity instanceof WolfEntity) {

            if (compound.contains("CollarColor", 99)) {

                ((WolfEntity) entity).setCollarColor(DyeColor.byId(compound.getInt("CollarColor")));
            }

            ((WolfEntity) entity).setTamed(compound.hasUniqueId("Owner") || !compound.getString("Owner").isEmpty());
            ((WolfEntity) entity).func_233687_w_(compound.getBoolean("Sitting"));
            ((WolfEntity) entity).func_233686_v_(((WolfEntity) entity).func_233685_eM_());
        }
    }

    private static void readLivingAdditional(Entity entity, CompoundNBT compound) {

        if (compound.contains("ArmorItems", 9)) {

            ListNBT listnbt = compound.getList("ArmorItems", 10);
            NonNullList<ItemStack> inventoryArmor = (NonNullList<ItemStack>) entity.getArmorInventoryList();
            for(int i = 0; i < inventoryArmor.size(); ++i) {

                inventoryArmor.set(i, ItemStack.read(listnbt.getCompound(i)));
            }
        }

        if (compound.contains("HandItems", 9)) {

            ListNBT listnbt1 = compound.getList("HandItems", 10);
            NonNullList<ItemStack> inventoryHands = (NonNullList<ItemStack>) entity.getHeldEquipment();
            for(int j = 0; j < inventoryHands.size(); ++j) {

                inventoryHands.set(j, ItemStack.read(listnbt1.getCompound(j)));
            }
        }
    }

}
