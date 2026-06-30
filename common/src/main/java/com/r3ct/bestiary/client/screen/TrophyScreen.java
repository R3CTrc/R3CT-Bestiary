package com.r3ct.bestiary.client.screen;

import com.r3ct.bestiary.block.TrophyBlockEntity;
import com.r3ct.bestiary.platform.Services;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class TrophyScreen extends Screen {
    private final TrophyBlockEntity trophyBE;
    private int page = 0;
    private final int itemsPerPage = 10;

    public TrophyScreen(TrophyBlockEntity trophyBE) {
        super(Component.literal("Wybierz okaz"));
        this.trophyBE = trophyBE;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        List<String> list = trophyBE.getEntityList();
        if (list == null || list.isEmpty()) return;

        int maxPages = (int) Math.ceil(list.size() / (double) itemsPerPage);
        int startY = 40;
        int buttonWidth = 160;
        int buttonHeight = 20;

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, list.size());

        // Generujemy po kolei ładne przyciski z nazwami mobów (wziętymi z tłumaczeń gry)
        for (int i = startIndex; i < endIndex; i++) {
            String entityId = list.get(i);
            Component entityName = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId))
                    // ZMIANA TUTAJ: Wyciągamy wartość z holdera przez holder.value()
                    .map(holder -> holder.value().getDescription())
                    .orElse(Component.literal(entityId));

            int yOffset = startY + ((i - startIndex) * (buttonHeight + 4));

            this.addRenderableWidget(Button.builder(entityName, btn -> {
                // Wysyłamy pakiet do serwera po kliknięciu i zamykamy menu
                Services.PLATFORM.sendSetTrophyEntityPacket(trophyBE.getBlockPos(), entityId);
                this.onClose();
            }).bounds(this.width / 2 - buttonWidth / 2, yOffset, buttonWidth, buttonHeight).build());
        }

        // Paginacja (Kolejne strony, jeśli kategoria ma więcej niż 10 mobów)
        if (maxPages > 1) {
            int navY = startY + (itemsPerPage * (buttonHeight + 4)) + 10;

            Button prevBtn = Button.builder(Component.literal("< Poprzednia"), btn -> {
                this.page--;
                this.init();
            }).bounds(this.width / 2 - 100, navY, 90, 20).build();
            prevBtn.active = (page > 0);
            this.addRenderableWidget(prevBtn);

            Button nextBtn = Button.builder(Component.literal("Następna >"), btn -> {
                this.page++;
                this.init();
            }).bounds(this.width / 2 + 10, navY, 90, 20).build();
            nextBtn.active = (page < maxPages - 1);
            this.addRenderableWidget(nextBtn);
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}