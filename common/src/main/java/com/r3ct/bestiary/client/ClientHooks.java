package com.r3ct.bestiary.client;

import com.r3ct.bestiary.block.TrophyBlockEntity;
import com.r3ct.bestiary.client.screen.TrophySelectionScreen;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class ClientHooks {
    public static void openTrophyScreen(TrophyBlockEntity blockEntity) {

        List<String> entityList = blockEntity.getEntityList();

        if (entityList == null) {
            entityList = new ArrayList<>();
        }

        String currentDisplay = blockEntity.getDisplayEntityId();

        Minecraft.getInstance().setScreen(new TrophySelectionScreen(
                blockEntity.getBlockPos(),
                entityList,
                currentDisplay
        ));
    }
}