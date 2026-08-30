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

        registerTrophy("trophy", ModBlocks.TROPHY);

        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.parse(Constants.MOD_ID + ":trophy_be"), ModBlocks.TROPHY_BE_TYPE);

        ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                Identifier.parse(Constants.MOD_ID + ":main_tab")
        );

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, FabricCreativeModeTab.builder()
                .title(net.minecraft.network.chat.Component.translatable("itemGroup." + Constants.MOD_ID + ".main_tab"))
                .icon(() -> new net.minecraft.world.item.ItemStack(ModBlocks.TROPHY))
                .displayItems((context, output) -> {
                    output.accept(ModBlocks.TROPHY);
                })
                .build()
        );

        PayloadTypeRegistry.clientboundPlay().register(SyncDataPayload.TYPE, SyncDataPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(com.r3ct.bestiary.network.RequestLeaderboardPayload.TYPE, com.r3ct.bestiary.network.RequestLeaderboardPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(com.r3ct.bestiary.network.LeaderboardDataPayload.TYPE, com.r3ct.bestiary.network.LeaderboardDataPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(com.r3ct.bestiary.network.ConfigSyncPayload.TYPE, com.r3ct.bestiary.network.ConfigSyncPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(com.r3ct.bestiary.network.SetTrophyEntityPayload.TYPE, com.r3ct.bestiary.network.SetTrophyEntityPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(com.r3ct.bestiary.network.DebugCompleteCategoryPayload.TYPE, com.r3ct.bestiary.network.DebugCompleteCategoryPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            com.r3ct.bestiary.data.PlayerData data = com.r3ct.bestiary.data.ModState.getPlayerData(server, handler.player.getUUID());

            com.r3ct.bestiary.platform.Services.PLATFORM.sendSyncDataPacketToClient(handler.player, new java.util.HashSet<>(data.unlockedMobs), new java.util.ArrayList<>(data.rewardedCategories));

            String mobsJson = BestiaryConfig.getMobsConfigAsString();
            String rewardsJson = BestiaryConfig.getRewardsConfigAsString();
            ServerPlayNetworking.send(handler.player, new com.r3ct.bestiary.network.ConfigSyncPayload(mobsJson, rewardsJson));
        });

        ServerPlayNetworking.registerGlobalReceiver(com.r3ct.bestiary.network.RequestLeaderboardPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> MobProgressHandler.handleLeaderboardRequest(context.player()));
        });

        ServerPlayNetworking.registerGlobalReceiver(com.r3ct.bestiary.network.SetTrophyEntityPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                net.minecraft.world.entity.player.Player player = context.player();
                if (player.level().isLoaded(payload.pos()) && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(payload.pos())) < 64.0) {
                    net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(payload.pos());
                    if (be instanceof com.r3ct.bestiary.block.TrophyBlockEntity tbe) {
                        tbe.setDisplayEntityId(payload.entityId());
                        player.level().playSound(null, payload.pos(), net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.2f);
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(com.r3ct.bestiary.network.DebugCompleteCategoryPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                MobProgressHandler.debugCompleteCategory(context.player(), payload.categoryId());
            });
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