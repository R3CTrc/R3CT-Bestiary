package com.r3ct.bestiary;

import com.r3ct.bestiary.client.input.KeyMappings;
import com.r3ct.bestiary.client.data.ClientPlayerData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class BestiaryClientFabric implements ClientModInitializer {

    private static final KeyMapping.Category R3CT_BESTIARY_CATEGORY = KeyMapping.Category.register(Identifier.parse("r3ct_bestiary:main"));

    @Override
    public void onInitializeClient() {

        KeyMappings.openCatalogKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.r3ct.open_catalog",
                com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                R3CT_BESTIARY_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            KeyMappings.handleKeyInput();
        });

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                com.r3ct.bestiary.network.SyncDataPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        ClientPlayerData.unlockedMobs = new java.util.HashSet<>(payload.unlockedMobs());
                        ClientPlayerData.rewardedCategories = new java.util.HashSet<>(payload.rewardedCategories());
                    });
                }
        );

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                com.r3ct.bestiary.network.LeaderboardDataPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    ClientPlayerData.leaderboardData = new java.util.ArrayList<>(payload.entries());
                })
        );

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                com.r3ct.bestiary.network.MobStatsSyncPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    ClientPlayerData.serverMobStats = payload.statsMap();
                })
        );

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                com.r3ct.bestiary.network.ConfigSyncPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    com.r3ct.bestiary.config.BestiaryConfig.syncFromServer(payload.mobsJson(), payload.rewardsJson());
                })
        );

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            com.r3ct.bestiary.config.BestiaryConfig.load();
        });

        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                com.r3ct.bestiary.block.ModBlocks.TROPHY_BE_TYPE,
                com.r3ct.bestiary.client.render.TrophyBlockEntityRenderer::new
        );
    }
}