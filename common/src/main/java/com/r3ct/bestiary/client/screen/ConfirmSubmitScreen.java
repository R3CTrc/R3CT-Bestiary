package com.r3ct.bestiary.client.screen;

import com.r3ct.bestiary.platform.Services;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class ConfirmSubmitScreen extends Screen {
    private final Screen parent;
    private final ItemStack stack;
    private final int slotId;
    private final String itemId;

    public ConfirmSubmitScreen(Screen parent, ItemStack stack, int slotId, String itemId) {
        super(Component.translatable("gui.r3ct_collection.catalog.confirm_title"));
        this.parent = parent;
        this.stack = stack;
        this.slotId = slotId;
        this.itemId = itemId;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int btnY = this.height / 2 + 30;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.r3ct_collection.catalog.yes").withStyle(net.minecraft.ChatFormatting.GREEN), button -> {
            Services.PLATFORM.sendSubmitItemPacketToServer(this.itemId, this.slotId);
            if (this.minecraft != null) {
                this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
                this.minecraft.setScreen(this.parent);
            }
        }).bounds(centerX - 105, btnY, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.r3ct_collection.catalog.no").withStyle(net.minecraft.ChatFormatting.RED), button -> {
            if (this.minecraft != null) this.minecraft.setScreen(this.parent);
        }).bounds(centerX + 5, btnY, 100, 20).build());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xD9000000);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFFFF);

        int itemX = this.width / 2 - 8;
        int itemY = this.height / 2 - 10;

        guiGraphics.item(this.stack, itemX, itemY);
        guiGraphics.itemDecorations(this.font, this.stack, itemX, itemY);
        guiGraphics.fill(itemX - 2, itemY - 2, itemX + 18, itemY + 18, 0x44FFFFFF);

        if (mouseX >= itemX && mouseX <= itemX + 16 && mouseY >= itemY && mouseY <= itemY + 16) {
            guiGraphics.setTooltipForNextFrame(this.font, this.stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(this.minecraft.level), this.minecraft.player, net.minecraft.world.item.TooltipFlag.NORMAL), java.util.Optional.empty(), mouseX, mouseY);
        }
    }
}