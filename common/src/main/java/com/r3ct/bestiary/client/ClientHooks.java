package com.r3ct.bestiary.client;

import com.r3ct.bestiary.block.TrophyBlockEntity;
import com.r3ct.bestiary.client.screen.TrophySelectionScreen;
import com.r3ct.bestiary.client.data.ClientPlayerData;
import com.r3ct.bestiary.logic.MobProgressHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class ClientHooks {
    public static void openTrophyScreen(TrophyBlockEntity blockEntity) {
        List<String> unlockedEntities = new ArrayList<>();

        for (String entityId : ClientPlayerData.killCounts.keySet()) {
            int count = ClientPlayerData.killCounts.getOrDefault(entityId, 0);

            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId)).map(net.minecraft.core.Holder::value).orElse(null);

            if (type != null) {
                List<Integer> thresholds = MobProgressHandler.getProgressThresholds(entityId, type.getCategory());
                int baseReq = thresholds.isEmpty() ? 1 : thresholds.get(0);

                if (count >= baseReq) {
                    unlockedEntities.add(entityId);
                }
            }
        }

        String currentDisplay = blockEntity.getDisplayEntityId();

        Minecraft.getInstance().setScreen(new TrophySelectionScreen(
                blockEntity.getBlockPos(),
                unlockedEntities,
                currentDisplay
        ));
    }
}