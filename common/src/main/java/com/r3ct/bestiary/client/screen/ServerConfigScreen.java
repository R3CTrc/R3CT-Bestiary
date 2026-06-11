package com.r3ct.bestiary.client.screen;

import com.r3ct.bestiary.config.BestiaryConfig;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.nio.file.Paths;

public class ServerConfigScreen extends Screen {
    private final Screen parent;

    public ServerConfigScreen(Screen parent) {
        super(Component.translatable("gui.r3ct_bestiary.config.server.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = this.height / 2 - 30;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.r3ct_bestiary.config.server.mobs_button"), button -> {
                    File configFile = Paths.get("config", "r3ct_bestiary", "r3ct_bestiary_mobs.json").toFile();
                    if (!configFile.exists()) BestiaryConfig.saveMobs();
                    Util.getPlatform().openUri(configFile.toURI());
                })
                .bounds(centerX, startY, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.translatable("gui.r3ct_bestiary.config.server.mobs_tooltip")))
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.r3ct_bestiary.config.server.rewards_button"), button -> {
                    File configFile = Paths.get("config", "r3ct_bestiary", "r3ct_bestiary_rewards.json").toFile();
                    if (!configFile.exists()) BestiaryConfig.saveRewards();
                    Util.getPlatform().openUri(configFile.toURI());
                })
                .bounds(centerX, startY + 25, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.translatable("gui.r3ct_bestiary.config.server.rewards_tooltip")))
                .build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(centerX, startY + 60, buttonWidth, buttonHeight).build());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x99000000);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}