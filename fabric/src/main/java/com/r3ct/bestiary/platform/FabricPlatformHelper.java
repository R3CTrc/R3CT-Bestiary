package com.r3ct.bestiary.platform;

import com.r3ct.bestiary.network.SyncDataPayload;
import com.r3ct.bestiary.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public void sendSyncDataPacketToClient(net.minecraft.server.level.ServerPlayer player, java.util.Map<String, Integer> killCounts, java.util.Set<String> rewardedCategories) {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                player,
                new com.r3ct.bestiary.network.SyncDataPayload(killCounts, new java.util.ArrayList<>(rewardedCategories))
        );
    }

    @Override
    public void sendRequestLeaderboardPacketToServer() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.r3ct.bestiary.network.RequestLeaderboardPayload());
    }

    @Override
    public void sendLeaderboardDataPacketToClient(net.minecraft.server.level.ServerPlayer player, java.util.List<com.r3ct.bestiary.network.LeaderboardDataPayload.TopPlayerEntry> entries) {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new com.r3ct.bestiary.network.LeaderboardDataPayload(entries));
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityType<T> createBlockEntityType(java.util.function.BiFunction<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState, T> factory, net.minecraft.world.level.block.Block... blocks) {
        return net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(factory::apply, blocks).build();
    }

    @Override
    public boolean isCatalogKey(Object event) {
        if (event instanceof net.minecraft.client.input.KeyEvent keyEvent) {
            return com.r3ct.bestiary.client.input.KeyMappings.openCatalogKey != null &&
                    com.r3ct.bestiary.client.input.KeyMappings.openCatalogKey.matches(keyEvent);
        }
        return false;
    }

    @Override
    public void sendSetTrophyEntityPacket(net.minecraft.core.BlockPos pos, String entityId) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.r3ct.bestiary.network.SetTrophyEntityPayload(pos, entityId));
    }

    @Override
    public void sendDebugCompleteCategoryPacket(String categoryId) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.r3ct.bestiary.network.DebugCompleteCategoryPayload(categoryId));
    }
}