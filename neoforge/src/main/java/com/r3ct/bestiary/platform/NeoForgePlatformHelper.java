package com.r3ct.bestiary.platform;

import com.r3ct.bestiary.platform.services.IPlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public void sendSyncDataPacketToClient(net.minecraft.server.level.ServerPlayer player, java.util.Map<String, java.util.List<String>> unlockedActions, java.util.List<String> rewardedCategories) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player,
                new com.r3ct.bestiary.network.SyncDataPayload(unlockedActions, rewardedCategories)
        );
    }

    @Override
    public void sendRequestLeaderboardPacketToServer() {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new com.r3ct.bestiary.network.RequestLeaderboardPayload());
    }

    @Override
    public void sendLeaderboardDataPacketToClient(net.minecraft.server.level.ServerPlayer player, java.util.List<com.r3ct.bestiary.network.LeaderboardDataPayload.TopPlayerEntry> entries) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new com.r3ct.bestiary.network.LeaderboardDataPayload(entries));
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityType<T> createBlockEntityType(java.util.function.BiFunction<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState, T> factory, net.minecraft.world.level.block.Block... blocks) {
        return new net.minecraft.world.level.block.entity.BlockEntityType<>(factory::apply, java.util.Set.of(blocks));
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
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new com.r3ct.bestiary.network.SetTrophyEntityPayload(pos, entityId));
    }

    @Override
    public void sendDebugCompleteCategoryPacket(String categoryId) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new com.r3ct.bestiary.network.DebugCompleteCategoryPayload(categoryId));
    }
}