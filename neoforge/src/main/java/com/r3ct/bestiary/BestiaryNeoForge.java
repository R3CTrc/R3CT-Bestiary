package com.r3ct.bestiary;

import com.r3ct.bestiary.client.data.ClientPlayerData;
import com.r3ct.bestiary.config.BestiaryConfig;
import com.r3ct.bestiary.network.SyncDataPayload;
import com.r3ct.bestiary.logic.MobKillHandler;
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
                (payload, context) -> context.enqueueWork(() -> MobKillHandler.handleLeaderboardRequest((net.minecraft.server.level.ServerPlayer) context.player()))
        );

        registrar.playToClient(
                com.r3ct.bestiary.network.LeaderboardDataPayload.TYPE, com.r3ct.bestiary.network.LeaderboardDataPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    ClientPlayerData.leaderboardData = new java.util.ArrayList<>(payload.entries());
                })
        );
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            com.r3ct.bestiary.data.PlayerData data = com.r3ct.bestiary.data.ModState.getPlayerData(serverPlayer.level().getServer(), serverPlayer.getUUID());
            com.r3ct.bestiary.platform.Services.PLATFORM.sendSyncDataPacketToClient(serverPlayer, data.killCounts, data.rewardedCategories);
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

        registerTrophy.accept("trophy_building", ModBlocks.TROPHY_BUILDING);
        registerTrophy.accept("trophy_combat", ModBlocks.TROPHY_COMBAT);
        registerTrophy.accept("trophy_tools", ModBlocks.TROPHY_TOOLS);
        registerTrophy.accept("trophy_food", ModBlocks.TROPHY_FOOD);
        registerTrophy.accept("trophy_redstone", ModBlocks.TROPHY_REDSTONE);
        registerTrophy.accept("trophy_ingredients", ModBlocks.TROPHY_INGREDIENTS);
        registerTrophy.accept("trophy_natural", ModBlocks.TROPHY_NATURAL);
        registerTrophy.accept("trophy_colored", ModBlocks.TROPHY_COLORED);
        registerTrophy.accept("trophy_egg", ModBlocks.TROPHY_EGG);
        registerTrophy.accept("trophy_functional", ModBlocks.TROPHY_FUNCTIONAL);
        registerTrophy.accept("trophy_mod", ModBlocks.TROPHY_MOD);

        event.register(BuiltInRegistries.BLOCK_ENTITY_TYPE.key(), helper -> {
            helper.register(Identifier.parse(Constants.MOD_ID + ":trophy_building_be"), ModBlocks.TROPHY_BE_TYPE);
        });

        event.register(Registries.CREATIVE_MODE_TAB, helper -> {
            helper.register(Identifier.parse(Constants.MOD_ID + ":main_tab"),
                    net.minecraft.world.item.CreativeModeTab.builder()
                            .title(net.minecraft.network.chat.Component.translatable("itemGroup." + Constants.MOD_ID + ".main_tab"))
                            .icon(() -> new net.minecraft.world.item.ItemStack(ModBlocks.TROPHY_BUILDING))
                            .displayItems((context, output) -> {
                                output.accept(ModBlocks.TROPHY_BUILDING);
                                output.accept(ModBlocks.TROPHY_NATURAL);
                                output.accept(ModBlocks.TROPHY_COLORED);
                                output.accept(ModBlocks.TROPHY_COMBAT);
                                output.accept(ModBlocks.TROPHY_TOOLS);
                                output.accept(ModBlocks.TROPHY_REDSTONE);
                                output.accept(ModBlocks.TROPHY_FUNCTIONAL);
                                output.accept(ModBlocks.TROPHY_FOOD);
                                output.accept(ModBlocks.TROPHY_INGREDIENTS);
                                output.accept(ModBlocks.TROPHY_EGG);
                                output.accept(ModBlocks.TROPHY_MOD);
                            })
                            .build()
            );
        });
    }
}