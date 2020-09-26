package com.fuzs.menucompanions.client.util;

import com.fuzs.menucompanions.MenuCompanions;
import net.minecraft.entity.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

public class CreateEntityUtil {

    @Nullable
    public static Entity loadEntity(EntityType<?> type, CompoundNBT compound, World worldIn) {

        Entity entity = type.create(worldIn);
        if (entity == null) {

            return null;
        }

        if (!compound.isEmpty()) {

            entity.read(compound);
        } else if (entity instanceof MobEntity) {

            try {

                DifficultyInstance difficulty = new DifficultyInstance(Difficulty.HARD, 100000L, 100000, 1.0F);
                ((MobEntity) entity).onInitialSpawn(null, difficulty, SpawnReason.COMMAND, null, null);
            } catch (Exception ignored) {

            }
        }

        if (compound.contains("Passengers", 9)) {

            ListNBT listnbt = compound.getList("Passengers", 10);
            for(int i = 0; i < listnbt.size(); ++i) {

                Entity passenger = CreateEntityUtil.loadEntityAndExecute(listnbt.getCompound(i), worldIn, Function.identity());
                if (passenger != null) {

                    passenger.startRiding(entity, true);
                }
            }
        }

        return entity;
    }

    @Nullable
    public static Entity loadEntityAndExecute(CompoundNBT compound, World worldIn, Function<Entity, Entity> p_220335_2_) {
        return loadEntity(compound, worldIn).map(p_220335_2_).map((p_220346_3_) -> {
            if (compound.contains("Passengers", 9)) {
                ListNBT listnbt = compound.getList("Passengers", 10);

                for(int i = 0; i < listnbt.size(); ++i) {
                    Entity entity = loadEntityAndExecute(listnbt.getCompound(i), worldIn, p_220335_2_);
                    if (entity != null) {
                        entity.startRiding(p_220346_3_, true);
                    }
                }
            }

            return p_220346_3_;
        }).orElse(null);
    }

    private static Optional<Entity> loadEntity(CompoundNBT compound, World worldIn) {
        try {
            return loadEntityUnchecked(compound, worldIn);
        } catch (RuntimeException runtimeexception) {
            MenuCompanions.LOGGER.warn("Exception loading entity: ", (Throwable)runtimeexception);
            return Optional.empty();
        }
    }

    public static Optional<Entity> loadEntityUnchecked(CompoundNBT compound, World worldIn) {
        return Util.acceptOrElse(EntityType.readEntityType(compound).map((entityType) -> {
            return entityType.create(worldIn);
        }), (entity) -> {
            if (!(entity instanceof IAngerable)) {

                entity.read(compound);
            } else if (entity instanceof MobEntity) {

                readAdditional((MobEntity) entity, compound);
            }
        }, () -> {
            MenuCompanions.LOGGER.warn("Skipping Entity with id {}", (Object)compound.getString("id"));
        });
    }

    private static void readAdditional(MobEntity entity, CompoundNBT compound) {

        if (compound.contains("CanPickUpLoot", 1)) {
            entity.setCanPickUpLoot(compound.getBoolean("CanPickUpLoot"));
        }

        if (compound.getBoolean("PersistenceRequired")) {

            entity.enablePersistence();
        }

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
