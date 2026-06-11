package com.r3ct.bestiary.platform;

import com.r3ct.bestiary.network.SubmitItemPayload;
import com.r3ct.bestiary.network.SyncDataPayload;
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
    public void sendSubmitItemPacketToServer(String itemId, int slotId) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new SubmitItemPayload(itemId, slotId));
    }

    @Override
    public void sendClaimRewardPacketToServer(String tabId) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new com.r3ct.bestiary.network.ClaimCategoryRewardPayload(tabId));
    }

    @Override
    public void sendSyncDataPacketToClient(net.minecraft.server.level.ServerPlayer player, java.util.Set<String> unlockedItems, java.util.Set<String> rewardedCategories) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new SyncDataPayload(new java.util.ArrayList<>(unlockedItems), new java.util.ArrayList<>(rewardedCategories)));
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
}