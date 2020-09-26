package com.fuzs.menucompanions.client.handler;

import com.fuzs.menucompanions.MenuCompanions;
import com.fuzs.menucompanions.client.util.DrawEntityUtil;
import com.fuzs.menucompanions.client.util.MenuEntityEntry;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.RemoteClientPlayerEntity;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.network.play.server.SPlayerListItemPacket;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.*;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MenuEntityHandler {

    private final Minecraft mc = Minecraft.getInstance();
    private final List<Consumer<? extends Event>> events = Lists.newArrayList();
    private final int maxAttempts = 10;

    private ClientWorld renderWorld;
    private final EnumMap<MenuEntityEntry.MenuSide, Pair<Entity, Boolean>> renderEntities = Maps.newEnumMap(MenuEntityEntry.MenuSide.class);
    private LivingEntity renderEntity;

    public void setup(ForgeConfigSpec.Builder builder) {

        this.addListener(this::onGuiOpen);
        this.addListener(this::onDrawScreen);
        this.addListener(this::onClientTick);
        this.addListener(this::onRenderNameplate);
        this.setupConfig(builder);
    }

    public void load() {

        this.events.forEach(MinecraftForge.EVENT_BUS::addListener);
    }

    private <T extends Event> void addListener(Consumer<T> consumer) {

        this.events.add(consumer);
    }

    private void unregister() {

        this.events.forEach(MinecraftForge.EVENT_BUS::unregister);
    }

    private void setupConfig(ForgeConfigSpec.Builder builder) {

    }

    private Optional<ClientWorld> getRenderWorld() {

        if (this.renderWorld == null) {

            this.renderWorld = this.createRenderWorld();
        }

        return Optional.ofNullable(this.renderWorld);
    }

    private ClientWorld createRenderWorld() {

        try {

            GameProfile profileIn = this.mc.getSession().getProfile();
            @SuppressWarnings("ConstantConditions")
            ClientPlayNetHandler clientPlayNetHandler = new ClientPlayNetHandler(this.mc, null, null, profileIn);
            ClientWorld.ClientWorldInfo worldInfo = new ClientWorld.ClientWorldInfo(Difficulty.HARD, false, false);
            DimensionType dimensionType = DynamicRegistries.func_239770_b_().func_230520_a_().func_243576_d(DimensionType.OVERWORLD);
            return new ClientWorld(clientPlayNetHandler, worldInfo, World.OVERWORLD, dimensionType, 3, this.mc::getProfiler, this.mc.worldRenderer, false, 0);
        } catch (Exception e) {

            MenuCompanions.LOGGER.error(e);
        }

        this.unregister();
        return null;
    }

    private void onGuiOpen(final GuiOpenEvent evt) {

        if (evt.getGui() instanceof MainMenuScreen) {

            this.setMenuSide(MenuEntityEntry.MenuSide.LEFT);
            this.setMenuSide(MenuEntityEntry.MenuSide.RIGHT);
        }
    }

    private void onDrawScreen(final GuiScreenEvent.DrawScreenEvent.Post evt) {

        if (evt.getGui() instanceof MainMenuScreen) {

            this.runForSides((side, entity) -> {

                int posX = (int) (evt.getGui().width * (side == MenuEntityEntry.MenuSide.LEFT ? 1.0F : 6.0F) / 7.0F);
                int posY = evt.getGui().height / 4 + 5 * 24;
                int scale = 30;
                DrawEntityUtil.drawEntityOnScreen(posX, posY, scale, -evt.getMouseX() + posX, -evt.getMouseY() + posY - 50, entity);

                for (Entity passenger : entity.getRecursivePassengers()) {

                    posY -= scale * (passenger.getPosY() - passenger.getRidingEntity().getPosY());
//                    posY -= scale * (passenger.getRidingEntity().getMountedYOffset() + passenger.getYOffset());
                    DrawEntityUtil.drawEntityOnScreen(posX, posY, scale, -evt.getMouseX() + posX, -evt.getMouseY() + posY - 50, passenger);
                }
            });
        }
    }

    private void onClientTick(final TickEvent.ClientTickEvent evt) {

        if (evt.phase != TickEvent.Phase.END && this.mc.currentScreen instanceof MainMenuScreen) {

            this.runForSides((side, entity) -> {

                entity.ticksExisted++;
                if (entity.isBeingRidden()) {

                    for (Entity passenger : entity.getRecursivePassengers()) {

                        passenger.ticksExisted++;
                        passenger.getRidingEntity().updatePassenger(passenger);
                    }
                }
            });
        }
    }

    private void onRenderNameplate(final RenderNameplateEvent evt) {

        // TODO
        evt.setResult(Event.Result.DENY);
        this.renderEntities.values().stream().filter(pair -> pair.getLeft() == evt.getEntity()).findFirst().ifPresent(pair -> {

            evt.setResult(Event.Result.DENY);
            if (pair.getRight()) {

                this.renderName(evt.getEntity(), evt.getContent(), evt.getMatrixStack(), evt.getRenderTypeBuffer(), evt.getPackedLight(), evt.getEntityRenderer().getRenderManager());
            }
        });
    }

    protected void renderName(Entity entityIn, ITextComponent displayNameIn, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, EntityRendererManager renderManager) {

        float renderHeight = entityIn.getHeight() + 0.5F;
        float renderOffset = "deadmau5".equals(displayNameIn.getString()) ? -10 : 0;

        matrixStackIn.push();
        matrixStackIn.translate(0.0D, renderHeight, 0.0D);
        matrixStackIn.rotate(renderManager.getCameraOrientation());
        matrixStackIn.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix4f = matrixStackIn.getLast().getMatrix();

        float backgroundOpacity = Minecraft.getInstance().gameSettings.getTextBackgroundOpacity(0.25F);
        int alpha = (int)(backgroundOpacity * 255.0F) << 24;
        FontRenderer fontrenderer = renderManager.getFontRenderer();
        int textWidth = -fontrenderer.func_238414_a_(displayNameIn) / 2;
        fontrenderer.func_243247_a(displayNameIn, textWidth, renderOffset, 553648127, false, matrix4f, bufferIn, true, alpha, packedLightIn);
        fontrenderer.func_243247_a(displayNameIn, textWidth, renderOffset, -1, false, matrix4f, bufferIn, false, 0, packedLightIn);

        matrixStackIn.pop();
    }

    private void setMenuSide(MenuEntityEntry.MenuSide side) {

        for (int i = 0; i < this.maxAttempts; i++) {

            MenuEntityEntry entry = MenuEntityProvider.getRandomEntry(side);
            try {

                if (entry != null && this.getRenderWorld().isPresent()) {

                    Entity entity = entry.create(this.getRenderWorld().get());
                    if (entity != null) {

                        this.renderEntities.put(side, Pair.of(entity, entry.showNameplate()));
                        return;
                    } else {

                        MenuCompanions.LOGGER.error("Entity is null");
                    }
                }
            } catch (Exception e) {

                MenuCompanions.LOGGER.error(e);
            }
        }

        this.renderEntities.put(side, null);
    }

    private void setRenderEntityRandom() {

        List<EntityType<?>> entityTypes = ForgeRegistries.ENTITIES.getValues().stream().filter(type -> type.getClassification() != EntityClassification.MISC).collect(Collectors.toList());
        Collections.shuffle(entityTypes);
        Optional<EntityType<?>> optionalLivingEntity = entityTypes.stream().findFirst();
        optionalLivingEntity.ifPresent(type -> this.renderEntity = (LivingEntity) type.create(this.getRenderWorld().get()));
        try {

            if (this.renderEntity instanceof MobEntity) {

                ((MobEntity) this.renderEntity).onInitialSpawn(null, new DifficultyInstance(Difficulty.HARD, 100000L, 100000, 1.0F), SpawnReason.COMMAND, null, null);
            }
        } catch (Exception ignored) {

        }
    }

    private void setRenderEntityPlayer() {

        this.renderEntity = new RemoteClientPlayerEntity(this.getRenderWorld().get(), this.mc.getSession().getProfile()) {

            private NetworkPlayerInfo playerInfo;

            @Override
            protected NetworkPlayerInfo getPlayerInfo() {

                if (this.playerInfo == null) {

                     this.playerInfo = new NetworkPlayerInfo(new SPlayerListItemPacket().new AddPlayerData(MenuEntityHandler.this.mc.getSession().getProfile(), 1, GameType.SURVIVAL, null));
                }

                return this.playerInfo;
            }

            @Override
            public boolean isSpectator() {

                return false;
            }

            @Override
            public boolean isCreative() {

                return false;
            }

            @Override
            public boolean isWearing(@Nonnull PlayerModelPart part) {

                return MenuEntityHandler.this.mc.gameSettings.getModelParts().contains(part);
            }

        };
    }

    private void runForSides(BiConsumer<MenuEntityEntry.MenuSide, Entity> action) {

        this.renderEntities.forEach((key, value) -> {

            try {

                action.accept(key, value.getLeft());
            } catch (Exception e) {

                e.printStackTrace();
                MenuCompanions.LOGGER.error(e);
                this.setMenuSide(key);
            }
        });
    }

}
