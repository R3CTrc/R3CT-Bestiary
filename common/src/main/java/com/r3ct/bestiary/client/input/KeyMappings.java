package com.r3ct.bestiary.client.input;

import com.r3ct.bestiary.client.screen.CatalogScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class KeyMappings {

    public static KeyMapping openCatalogKey;

    public static void handleKeyInput() {
        if (openCatalogKey == null) return;

        Minecraft mc = Minecraft.getInstance();

        while (openCatalogKey.consumeClick()) {
            if (mc.screen == null && mc.level != null) {
                mc.setScreen(new CatalogScreen());
            }
        }
    }
}