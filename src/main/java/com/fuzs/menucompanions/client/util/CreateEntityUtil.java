package com.fuzs.menucompanions.client.util;

import com.fuzs.menucompanions.MenuCompanions;
import com.fuzs.menucompanions.client.element.MenuEntityElement;
import com.fuzs.menucompanions.client.entity.player.MenuClientPlayerEntity;
import com.fuzs.menucompanions.client.world.MenuClientWorld;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.*;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.tileentity.SkullTileEntity;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IWorld;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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

        if (EntityType.readEntityType(compound).map(entityType ->
                MenuEntityElement.get().isAllowed(entityType) ? entityType : null).isPresent()) {

            try {

                return loadEntityUnchecked(compound, worldIn);
            } catch (RuntimeException runtimeexception) {

                MenuCompanions.LOGGER.warn("Exception loading entity: ", runtimeexception);
                MenuEntityElement.get().addToBlacklist(compound.getString("id"));
            }
        }

        return Optional.empty();
    }

    public static Optional<Entity> loadEntityUnchecked(CompoundNBT compound, MenuClientWorld worldIn) {

        return Util.acceptOrElse(EntityType.readEntityType(compound)
                .map(entityType -> create(worldIn, entityType)), entity -> {

            try {

                entity.read(compound);
            } catch (Exception e) {

                // just enable held items and armor items to show at least in case nothing else works
                if (entity instanceof MobEntity) {

                    readMobData(entity, compound);
                }
            }
        }, () -> {

            String id = compound.getString("id");
            MenuCompanions.LOGGER.warn("Skipping entity with id {}", id);
            MenuEntityElement.get().addToBlacklist(id);
        });
    }

    @Nullable
    public static Entity create(MenuClientWorld worldIn, EntityType<?> type) {

        if (type == EntityType.PLAYER) {

            assert gameProfile != null : "No game profile found";
            return new MenuClientPlayerEntity(worldIn, gameProfile);
        }

        return type.create(worldIn);
    }

    public static void setGameProfile(String profile) {

        gameProfile = getGameProfile(profile);
    }

    private static GameProfile getGameProfile(String profile) {

        if (StringUtils.isNullOrEmpty(profile)) {

            return Minecraft.getInstance().getSession().getProfile();
        }

        return updateGameProfile(new GameProfile(null, profile));
    }

    private static GameProfile updateGameProfile(GameProfile input) {

        GameProfile gameprofile = SkullTileEntity.updateGameProfile(input);
        if (gameprofile == input) {

            YggdrasilAuthenticationService yggdrasilauthenticationservice = new YggdrasilAuthenticationService(Minecraft.getInstance().getProxy(), UUID.randomUUID().toString());
            MinecraftSessionService minecraftsessionservice = yggdrasilauthenticationservice.createMinecraftSessionService();
            GameProfileRepository gameprofilerepository = yggdrasilauthenticationservice.createProfileRepository();
            PlayerProfileCache playerprofilecache = new PlayerProfileCache(gameprofilerepository, new File(Minecraft.getInstance().gameDir, MinecraftServer.USER_CACHE_FILE.getName()));
            SkullTileEntity.setProfileCache(playerprofilecache);
            SkullTileEntity.setSessionService(minecraftsessionservice);

            gameprofile = SkullTileEntity.updateGameProfile(input);
        }

        return gameprofile;
    }

    public static void onInitialSpawn(Entity entity, IWorld worldIn, boolean noNbt) {

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

                    ((AgeableEntity) entity).setGrowingAge(-24000);
                }
            } catch (Exception ignored) {

                // just ignore this as it's not important and doesn't fail too often
            }
        }
    }

    public static void readMobData(Entity entity, CompoundNBT compound) {

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
