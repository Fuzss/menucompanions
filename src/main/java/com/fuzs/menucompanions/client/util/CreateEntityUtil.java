package com.fuzs.menucompanions.client.util;

import com.fuzs.menucompanions.MenuCompanions;
import com.fuzs.menucompanions.client.entity.MenuClientPlayerEntity;
import com.fuzs.menucompanions.client.handler.MenuEntityHandler;
import com.fuzs.menucompanions.client.world.MenuClientWorld;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.*;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class CreateEntityUtil {

    private static GameProfile gameProfile;

    @Nullable
    public static Entity loadEntity(EntityType<?> type, CompoundNBT compound, MenuClientWorld worldIn, Function<Entity, Entity> mapper) {

        CompoundNBT compoundnbt = compound.copy();
        compoundnbt.putString("id", Objects.requireNonNull(ForgeRegistries.ENTITIES.getKey(type)).toString());

        return loadEntityAndExecute(compoundnbt, worldIn, mapper);
    }

    @Nullable
    public static Entity loadEntityAndExecute(CompoundNBT compound, MenuClientWorld worldIn, Function<Entity, Entity> mapper) {

        return loadEntity(compound, worldIn).map(mapper).map(entity -> {

            if (compound.contains("Passengers", 9)) {

                ListNBT listnbt = compound.getList("Passengers", 10);
                for(int i = 0; i < listnbt.size(); ++i) {

                    Entity passenger = loadEntityAndExecute(listnbt.getCompound(i), worldIn, mapper);
                    if (passenger != null) {

                        passenger.startRiding(entity, true);
                        entity.updatePassenger(passenger);
                    }
                }
            }

            return entity;
        }).orElse(null);
    }

    private static Optional<Entity> loadEntity(CompoundNBT compound, MenuClientWorld worldIn) {

        try {

            return loadEntityUnchecked(compound, worldIn);
        } catch (RuntimeException runtimeexception) {

            MenuCompanions.LOGGER.warn("Exception loading entity: ", runtimeexception);
            MenuEntityHandler.addToBlacklist(compound.getString("id"));
            return Optional.empty();
        }
    }

    public static Optional<Entity> loadEntityUnchecked(CompoundNBT compound, MenuClientWorld worldIn) {

        return Util.acceptOrElse(EntityType.readEntityType(compound)
                .map(entityType -> MenuEntityHandler.isAllowed(entityType) ? entityType : null)
                .map(entityType -> create(worldIn, entityType)), entity -> {

            try {

                entity.read(compound);
            } catch (Exception e) {

                // just enable held items and armor items to show at least in case nothing else works
                // world is casted to ServerWorld in IAngerable, but that's fine as the problematic method is always called after everything else has been read
                if (entity instanceof MobEntity && !(entity instanceof IAngerable)) {

                    readLivingAdditional(entity, compound);
                }
            }
        }, () -> {

            String id = compound.getString("id");
            MenuCompanions.LOGGER.warn("Skipping Entity with id {}", id);
            MenuEntityHandler.addToBlacklist(id);
        });
    }

    @Nullable
    public static Entity create(MenuClientWorld worldIn, EntityType<?> type) {

        if (type == EntityType.PLAYER && gameProfile != null) {

            return new MenuClientPlayerEntity(worldIn, gameProfile);
        }

        return type.create(worldIn);
    }

    public static void setGameProfile(GameProfile input) {

        gameProfile = input;
    }

    public static void onInitialSpawn(Entity entity, IServerWorld worldIn, boolean noNbt) {

        // prevents crash in sprite renderers
        entity.ticksExisted = 2;
        // prevents Entity#move from running as it calls a block tag which isn't registered yet
        entity.noClip = true;
        if (noNbt && entity instanceof MobEntity) {

            try {

                // set difficulty very hard so gear is more likely to appear
                DifficultyInstance difficulty = new DifficultyInstance(Difficulty.HARD, 100000L, 100000, 1.0F);
                ((MobEntity) entity).onInitialSpawn(worldIn, difficulty, SpawnReason.COMMAND, null, null);
                if (entity instanceof AgeableEntity && ((AgeableEntity) entity).getRNG().nextFloat() <= 0.05F) {

                    ((AgeableEntity) entity).setChild(true);
                }
            } catch (Exception ignored) {

                // just ignore this as it's not important and doesn't fail too often
            }
        }
    }

    public static void readLivingAdditional(Entity entity, CompoundNBT compound) {

        if (compound.contains("ArmorItems", 9)) {

            ListNBT listnbt = compound.getList("ArmorItems", 10);
            Stream.of(EquipmentSlotType.values())
                    .filter(slot -> slot.getSlotType() == EquipmentSlotType.Group.ARMOR)
                    .forEach(slot -> entity.setItemStackToSlot(slot, ItemStack.read(listnbt.getCompound(slot.getIndex()))));
        }

        if (compound.contains("HandItems", 9)) {

            ListNBT listnbt = compound.getList("HandItems", 10);
            Stream.of(EquipmentSlotType.values())
                    .filter(slot -> slot.getSlotType() == EquipmentSlotType.Group.HAND)
                    .forEach(slot -> entity.setItemStackToSlot(slot, ItemStack.read(listnbt.getCompound(slot.getIndex()))));
        }
    }

}
