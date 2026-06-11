package com.r3ct.bestiary.client.screen;

import com.r3ct.bestiary.config.BestiaryConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class ClientConfigScreen extends Screen {
    private final Screen parent;
    private EditBox scaleBox;

    public ClientConfigScreen(Screen parent) {
        super(Component.translatable("gui.r3ct_bestiary.config.client.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int rightColumnX = this.width / 2 + 20;
        int widgetWidth = 140;
        int widgetHeight = 20;

        this.scaleBox = new EditBox(this.font, rightColumnX, 80, widgetWidth, widgetHeight, Component.translatable("gui.r3ct_bestiary.config.client.scale"));
        this.scaleBox.setValue(String.valueOf(BestiaryConfig.catalogScale));
        this.addRenderableWidget(this.scaleBox);

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 40, 200, 20).build());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x99000000);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);

        int leftColumnX = this.width / 2 - 160;
        guiGraphics.text(this.font, Component.translatable("gui.r3ct_bestiary.config.client.scale_desc"), leftColumnX, 80 + 6, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        try {
            BestiaryConfig.catalogScale = Float.parseFloat(this.scaleBox.getValue());
        } catch (NumberFormatException ignored) {}

        BestiaryConfig.saveClient();

        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}