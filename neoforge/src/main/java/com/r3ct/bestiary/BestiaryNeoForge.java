package com.r3ct.bestiary;

import com.r3ct.bestiary.client.data.ClientPlayerData;
import com.r3ct.bestiary.config.BestiaryConfig;
import com.r3ct.bestiary.network.SyncDataPayload;
import com.r3ct.bestiary.logic.MobProgressHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.RegisterEvent;
import com.r3ct.bestiary.block.ModBlocks;

import java.util.HashMap;
import java.util.HashSet;

@Mod(Constants.MOD_ID)
public class BestiaryNeoForge {

    public BestiaryNeoForge(IEventBus modEventBus) {
        BestiaryConfig.load();

        modEventBus.addListener(this::registerPackets);
        modEventBus.addListener(this::onRegister);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
    }

    private void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Constants.MOD_ID);

        registrar.playToClient(
                SyncDataPayload.TYPE, SyncDataPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    ClientPlayerData.killCounts = new HashMap<>(payload.killCounts());
                    ClientPlayerData.rewardedCategories = new HashSet<>(payload.rewardedCategories());
                })
        );

        registrar.playToServer(
                com.r3ct.bestiary.network.RequestLeaderboardPayload.TYPE, com.r3ct.bestiary.network.RequestLeaderboardPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> MobProgressHandler.handleLeaderboardRequest((net.minecraft.server.level.ServerPlayer) context.player()))
        );

        registrar.playToClient(
                com.r3ct.bestiary.network.LeaderboardDataPayload.TYPE, com.r3ct.bestiary.network.LeaderboardDataPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    ClientPlayerData.leaderboardData = new java.util.ArrayList<>(payload.entries());
                })
        );

        registrar.playToClient(
                com.r3ct.bestiary.network.MobStatsSyncPayload.TYPE, com.r3ct.bestiary.network.MobStatsSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    ClientPlayerData.serverMobStats = payload.statsMap();
                })
        );

        registrar.playToClient(
                com.r3ct.bestiary.network.ConfigSyncPayload.TYPE, com.r3ct.bestiary.network.ConfigSyncPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    BestiaryConfig.syncFromServer(payload.mobsJson(), payload.rewardsJson());
                })
        );
        registrar.playToServer(
                com.r3ct.bestiary.network.SetTrophyEntityPayload.TYPE, com.r3ct.bestiary.network.SetTrophyEntityPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    net.minecraft.world.entity.player.Player player = context.player();
                    if (player.level().isLoaded(payload.pos()) && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(payload.pos())) < 64.0) {
                        net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(payload.pos());
                        if (be instanceof com.r3ct.bestiary.block.TrophyBlockEntity tbe) {
                            if (tbe.getEntityList().contains(payload.entityId())) {
                                tbe.setDisplayEntityId(payload.entityId());
                                player.level().playSound(null, payload.pos(), net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.2f);
                            }
                        }
                    }
                })
        );
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            com.r3ct.bestiary.data.PlayerData data = com.r3ct.bestiary.data.ModState.getPlayerData(serverPlayer.level().getServer(), serverPlayer.getUUID());
            com.r3ct.bestiary.platform.Services.PLATFORM.sendSyncDataPacketToClient(serverPlayer, data.killCounts, data.rewardedCategories);
            var statsMap = com.r3ct.bestiary.scanner.ServerMobScanner.getServerMobStats(serverPlayer.level());
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer, new com.r3ct.bestiary.network.MobStatsSyncPayload(statsMap));
            String mobsJson = BestiaryConfig.getMobsConfigAsString();
            String rewardsJson = BestiaryConfig.getRewardsConfigAsString();
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer, new com.r3ct.bestiary.network.ConfigSyncPayload(mobsJson, rewardsJson));
        }
    }

    private void onRegister(RegisterEvent event) {

        java.util.function.BiConsumer<String, Block> registerTrophy = (name, block) -> {
            Identifier id = Identifier.parse(Constants.MOD_ID + ":" + name);
            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);

            event.register(BuiltInRegistries.BLOCK.key(), helper -> helper.register(id, block));
            event.register(BuiltInRegistries.ITEM.key(), helper -> helper.register(id, new BlockItem(block, new Item.Properties()
                    .setId(itemKey).stacksTo(1).rarity(Rarity.EPIC).fireResistant()
            )));
        };

        // Zarejestrowanie tylko JEDNEGO, uniwersalnego trofeum
        registerTrophy.accept("trophy", ModBlocks.TROPHY);

        event.register(BuiltInRegistries.BLOCK_ENTITY_TYPE.key(), helper -> {
            helper.register(Identifier.parse(Constants.MOD_ID + ":trophy_be"), ModBlocks.TROPHY_BE_TYPE);
        });

        event.register(Registries.CREATIVE_MODE_TAB, helper -> {
            helper.register(Identifier.parse(Constants.MOD_ID + ":main_tab"),
                    net.minecraft.world.item.CreativeModeTab.builder()
                            .title(net.minecraft.network.chat.Component.translatable("itemGroup." + Constants.MOD_ID + ".main_tab"))
                            .icon(() -> new net.minecraft.world.item.ItemStack(ModBlocks.TROPHY))
                            .displayItems((context, output) -> {
                                output.accept(ModBlocks.TROPHY); // Tylko jedno trofeum w zakładce
                            })
                            .build()
            );
        });
    }
}