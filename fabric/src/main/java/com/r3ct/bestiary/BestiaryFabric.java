package com.r3ct.bestiary;

import com.r3ct.bestiary.config.BestiaryConfig;
import com.r3ct.bestiary.network.SyncDataPayload;
import com.r3ct.bestiary.logic.MobProgressHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import com.r3ct.bestiary.block.ModBlocks;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

public class BestiaryFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        BestiaryConfig.load();

        registerTrophy("trophy_building", ModBlocks.TROPHY_BUILDING);
        registerTrophy("trophy_combat", ModBlocks.TROPHY_COMBAT);
        registerTrophy("trophy_tools", ModBlocks.TROPHY_TOOLS);
        registerTrophy("trophy_food", ModBlocks.TROPHY_FOOD);
        registerTrophy("trophy_redstone", ModBlocks.TROPHY_REDSTONE);
        registerTrophy("trophy_ingredients", ModBlocks.TROPHY_INGREDIENTS);
        registerTrophy("trophy_natural", ModBlocks.TROPHY_NATURAL);
        registerTrophy("trophy_colored", ModBlocks.TROPHY_COLORED);
        registerTrophy("trophy_egg", ModBlocks.TROPHY_EGG);
        registerTrophy("trophy_functional", ModBlocks.TROPHY_FUNCTIONAL);
        registerTrophy("trophy_mod", ModBlocks.TROPHY_MOD);

        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.parse(Constants.MOD_ID + ":trophy_building_be"), ModBlocks.TROPHY_BE_TYPE);

        ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                Identifier.parse(Constants.MOD_ID + ":main_tab")
        );

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, FabricCreativeModeTab.builder()
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

        PayloadTypeRegistry.clientboundPlay().register(SyncDataPayload.TYPE, SyncDataPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(com.r3ct.bestiary.network.RequestLeaderboardPayload.TYPE, com.r3ct.bestiary.network.RequestLeaderboardPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(com.r3ct.bestiary.network.LeaderboardDataPayload.TYPE, com.r3ct.bestiary.network.LeaderboardDataPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(com.r3ct.bestiary.network.MobStatsSyncPayload.TYPE, com.r3ct.bestiary.network.MobStatsSyncPayload.STREAM_CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            com.r3ct.bestiary.data.PlayerData data = com.r3ct.bestiary.data.ModState.getPlayerData(server, handler.player.getUUID());
            com.r3ct.bestiary.platform.Services.PLATFORM.sendSyncDataPacketToClient(handler.player, data.killCounts, data.rewardedCategories);
            var statsMap = com.r3ct.bestiary.scanner.ServerMobScanner.getServerMobStats(handler.player.level());
            ServerPlayNetworking.send(handler.player, new com.r3ct.bestiary.network.MobStatsSyncPayload(statsMap));
        });

        ServerPlayNetworking.registerGlobalReceiver(com.r3ct.bestiary.network.RequestLeaderboardPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> MobProgressHandler.handleLeaderboardRequest(context.player()));
        });
    }

    private void registerTrophy(String name, Block block) {
        Identifier id = Identifier.parse(Constants.MOD_ID + ":" + name);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);

        Registry.register(BuiltInRegistries.BLOCK, id, block);
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()
                .setId(itemKey).stacksTo(1).rarity(Rarity.EPIC).fireResistant()
        ));
    }
}