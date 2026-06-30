package com.r3ct.bestiary.client;

import com.r3ct.bestiary.block.TrophyBlockEntity;
import com.r3ct.bestiary.client.screen.TrophyScreen;
import net.minecraft.client.Minecraft;

public class ClientHooks {
    public static void openTrophyScreen(TrophyBlockEntity blockEntity) {
        Minecraft.getInstance().setScreen(new TrophyScreen(blockEntity));
    }
}