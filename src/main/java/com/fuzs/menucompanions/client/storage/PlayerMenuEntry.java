package com.fuzs.menucompanions.client.storage;

import com.fuzs.menucompanions.client.handler.MenuEntityHandler;
import com.fuzs.menucompanions.client.util.CreateEntityUtil;
import com.fuzs.menucompanions.client.util.IEntrySerializer;
import com.fuzs.menucompanions.client.world.MenuClientWorld;
import com.fuzs.menucompanions.mixin.EntityAccessorMixin;
import com.fuzs.menucompanions.mixin.PlayerEntityAccessorMixin;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.tileentity.SkullTileEntity;
import net.minecraft.util.StringUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.util.UUID;

public class PlayerMenuEntry extends EntityMenuEntry {

    private final String profile;
    private final byte modelParts;
    private final boolean crouching;

    public PlayerMenuEntry(@Nullable EntityType<?> type, CompoundNBT compound, byte data, float scale, int xOffset, int yOffset, boolean nameplate, boolean particles, int weight, MenuEntityHandler.MenuSide side, String comment, String profile, byte modelParts, boolean crouching) {

        super(type, compound, data, scale, xOffset, yOffset, nameplate, particles, weight, side, comment);
        this.profile = profile;
        this.modelParts = modelParts;
        this.crouching = crouching;
    }

    @Nullable
    public Entity create(MenuClientWorld worldIn) {

        CreateEntityUtil.setGameProfile(getGameProfile(this.profile));
        return CreateEntityUtil.loadEntity(EntityType.PLAYER, this.compound, worldIn, entity -> {

            entity.setOnGround(this.readProperty(PropertyFlags.ON_GROUND));
            ((EntityAccessorMixin) entity).setInWater(this.readProperty(PropertyFlags.IN_WATER));
            CreateEntityUtil.readLivingAdditional(entity, this.compound);
            entity.getDataManager().set(PlayerEntityAccessorMixin.getPlayerModelFlag(), this.modelParts);
            if (this.crouching) {

                entity.setPose(Pose.CROUCHING);
            }

            return entity;
        });
    }

    @Override
    public JsonElement serialize() {

        JsonObject jsonobject = super.serialize().getAsJsonObject();
        jsonobject.add(PLAYER_NAME, this.serializePlayer());

        return jsonobject;
    }

    private JsonObject serializePlayer() {

        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("profile", this.profile);
        jsonobject.addProperty("crouching", this.crouching);
        jsonobject.add(MODEL_NAME, this.serializeModel());

        return jsonobject;
    }

    private JsonObject serializeModel() {

        JsonObject jsonobject = new JsonObject();
        IEntrySerializer.serializeEnumProperties(jsonobject, PlayerModelPart.class, this.modelParts, PlayerModelPart::getPartName, PlayerModelPart::getPartMask);

        return jsonobject;
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

}
