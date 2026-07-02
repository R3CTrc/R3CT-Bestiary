package com.r3ct.bestiary.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrophySelectionScreen extends Screen {

    private final BlockPos trophyPos;
    private final List<String> allEntities;
    private final List<String> filteredEntities = new ArrayList<>();

    private String selectedEntityId;
    private EditBox searchBox;
    private int scrollOffset = 0;
    private boolean isScrolling = false;

    private final Map<String, net.minecraft.world.entity.LivingEntity> dummyCache = new HashMap<>();

    private static final int WINDOW_WIDTH = 280;
    private static final int WINDOW_HEIGHT = 200;

    public TrophySelectionScreen(BlockPos pos, List<String> availableEntities, String currentDisplay) {
        super(Component.translatable("gui.r3ct_bestiary.trophy_select"));
        this.trophyPos = pos;
        this.allEntities = availableEntities;
        this.filteredEntities.addAll(availableEntities);
        this.selectedEntityId = currentDisplay != null && !currentDisplay.isEmpty() ? currentDisplay :
                (availableEntities.isEmpty() ? null : availableEntities.get(0));
    }

    @Override
    protected void init() {
        super.init();

        int windowX = (this.width - WINDOW_WIDTH) / 2;
        int windowY = (this.height - WINDOW_HEIGHT) / 2;

        this.searchBox = new EditBox(this.font, windowX + 15, windowY + 20, 140, 16, Component.literal("Szukaj..."));
        this.searchBox.setResponder(query -> {
            this.filteredEntities.clear();
            this.scrollOffset = 0;
            if (query.isEmpty()) {
                this.filteredEntities.addAll(this.allEntities);
            } else {
                String q = query.toLowerCase();
                for (String id : this.allEntities) {
                    if (id.toLowerCase().contains(q) || getEntityName(id).getString().toLowerCase().contains(q)) {
                        this.filteredEntities.add(id);
                    }
                }
            }
        });
        this.addRenderableWidget(this.searchBox);
    }

    private Component getEntityName(String entityId) {
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId))
                .map(holder -> holder.value().getDescription())
                .orElse(Component.literal(entityId));
    }

    private ItemStack getIconForEntity(String entityId) {
        EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId)).map(net.minecraft.core.Holder::value).orElse(null);
        if (type != null) {
            java.util.Optional<net.minecraft.core.Holder<net.minecraft.world.item.Item>> eggOptional = SpawnEggItem.byId(type);
            if (eggOptional.isPresent()) {
                return new ItemStack(eggOptional.get().value());
            }
            if (entityId.equals("minecraft:ender_dragon")) return new ItemStack(Items.DRAGON_HEAD);
            if (entityId.equals("minecraft:wither")) return new ItemStack(Items.NETHER_STAR);
            if (entityId.equals("minecraft:warden")) return new ItemStack(Items.ECHO_SHARD);
        }
        return new ItemStack(Items.SPAWNER);
    }

    private net.minecraft.world.entity.LivingEntity getOrCreateDummy(String entityId) {
        if (dummyCache.containsKey(entityId)) return dummyCache.get(entityId);
        if (this.minecraft == null || this.minecraft.level == null) return null;

        EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId)).map(net.minecraft.core.Holder::value).orElse(null);
        if (type != null) {
            try {
                net.minecraft.world.entity.Entity entity = type.create(this.minecraft.level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                    if (living instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(true);
                    if (living instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin piglin) piglin.setImmuneToZombification(true);
                    if (living instanceof net.minecraft.world.entity.monster.hoglin.Hoglin hoglin) hoglin.setImmuneToZombification(true);

                    dummyCache.put(entityId, living);
                    return living;
                }
            } catch (Exception ignored) {}
        }
        dummyCache.put(entityId, null);
        return null;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {

        int windowX = (this.width - WINDOW_WIDTH) / 2;
        int windowY = (this.height - WINDOW_HEIGHT) / 2;

        guiGraphics.fill(windowX, windowY, windowX + WINDOW_WIDTH, windowY + WINDOW_HEIGHT, 0xF8121212);
        guiGraphics.fill(windowX - 1, windowY - 1, windowX + WINDOW_WIDTH + 1, windowY, 0xFF333333);
        guiGraphics.fill(windowX - 1, windowY + WINDOW_HEIGHT, windowX + WINDOW_WIDTH + 1, windowY + WINDOW_HEIGHT + 1, 0xFF333333);
        guiGraphics.fill(windowX - 1, windowY, windowX, windowY + WINDOW_HEIGHT, 0xFF333333);
        guiGraphics.fill(windowX + WINDOW_WIDTH, windowY, windowX + WINDOW_WIDTH + 1, windowY + WINDOW_HEIGHT, 0xFF333333);

        guiGraphics.text(this.font, Component.translatable("gui.r3ct_bestiary.trophy_select").withStyle(ChatFormatting.BOLD, ChatFormatting.WHITE), windowX + 15, windowY + 8, 0xFFFFFFFF, false);

        int columns = 6;
        int visibleRows = 5;
        int cellW = 22;
        int cellH = 22;
        int gridX = windowX + 15;
        int gridY = windowY + 45;

        int totalRows = (int) Math.ceil((double) filteredEntities.size() / columns);
        int maxScroll = Math.max(0, totalRows - visibleRows);

        if (maxScroll > 0) {
            int trackX = gridX + (columns * cellW) + 5;
            int trackY = gridY;
            int trackH = visibleRows * cellH;

            guiGraphics.fill(trackX, trackY, trackX + 3, trackY + trackH, 0xFF222222);
            float scrollFraction = (float) scrollOffset / maxScroll;
            int thumbH = Math.max(10, (int) (((float) visibleRows / totalRows) * trackH));
            int thumbY = trackY + (int) (scrollFraction * (trackH - thumbH));
            guiGraphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, isScrolling ? 0xFF888888 : 0xFF555555);
        }

        int startIndex = scrollOffset * columns;
        int endIndex = Math.min(startIndex + (columns * visibleRows), filteredEntities.size());

        String hoveredEntity = null;

        for (int i = startIndex; i < endIndex; i++) {
            int index = i - startIndex;
            int slotX = gridX + (index % columns * cellW);
            int slotY = gridY + (index / columns * cellH);
            String entityId = filteredEntities.get(i);

            boolean isSelected = entityId.equals(selectedEntityId);
            boolean isHovered = mouseX >= slotX && mouseX < slotX + cellW - 2 && mouseY >= slotY && mouseY < slotY + cellH - 2;

            if (isHovered) hoveredEntity = entityId;

            int bgColor = isSelected ? 0xFFB8860B : (isHovered ? 0xFF444444 : 0xFF2A2A2A);
            guiGraphics.fill(slotX, slotY, slotX + cellW - 2, slotY + cellH - 2, bgColor);
            guiGraphics.fill(slotX + 1, slotY + 1, slotX + cellW - 3, slotY + cellH - 3, 0xFF1E1E1E);
            guiGraphics.item(getIconForEntity(entityId), slotX + 2, slotY + 2);
        }

        int previewX = windowX + 175;
        int previewY = windowY + 20;
        int previewW = 90;
        int previewH = 135;

        guiGraphics.fill(previewX, previewY, previewX + previewW, previewY + previewH, 0xFF181818);
        guiGraphics.fill(previewX, previewY, previewX + previewW, previewY + 1, 0xFF333333);

        String entityToPreview = hoveredEntity != null ? hoveredEntity : selectedEntityId;

        if (entityToPreview != null) {
            net.minecraft.world.entity.LivingEntity dummy = getOrCreateDummy(entityToPreview);
            if (dummy != null) {
                if (this.minecraft != null && this.minecraft.level != null) {
                    dummy.tickCount = (int) (this.minecraft.level.getGameTime() % 10000);
                }

                float maxDim = Math.max(dummy.getBbWidth(), dummy.getBbHeight());
                if (maxDim <= 0.01F) maxDim = 1.0F;
                int scale = (int) (35.0F * (float) Math.pow(maxDim, 0.4) / maxDim);

                net.minecraft.client.gui.screens.inventory.InventoryScreen.extractEntityInInventoryFollowsMouse(
                        guiGraphics, previewX + 5, previewY + 15, previewX + previewW - 5, previewY + previewH - 25,
                        scale, 0.0625F, (float) mouseX, (float) mouseY, dummy
                );
            }

            Component nameComp = getEntityName(entityToPreview);
            int textW = this.font.width(nameComp);
            float textScale = textW > previewW - 10 ? (previewW - 10f) / textW : 1.0f;

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(previewX + (previewW / 2.0f), previewY + previewH - 15);
            guiGraphics.pose().scale(textScale, textScale);
            guiGraphics.text(this.font, nameComp, -textW / 2, 0, 0xFFDDDDDD, false);
            guiGraphics.pose().popMatrix();
        }

        int btnX = previewX;
        int btnY = windowY + WINDOW_HEIGHT - 35;
        boolean btnHovered = mouseX >= btnX && mouseX <= btnX + previewW && mouseY >= btnY && mouseY <= btnY + 20;

        int btnColor = btnHovered ? 0xFFD4AF37 : 0xFFAA8822;
        guiGraphics.fill(btnX, btnY, btnX + previewW, btnY + 20, btnColor);
        guiGraphics.fill(btnX + 1, btnY + 1, btnX + previewW - 1, btnY + 19, btnHovered ? 0xFFE5C158 : 0xFFC5A030);

        Component btnText = Component.translatable("gui.r3ct_bestiary.apply").withStyle(ChatFormatting.BOLD);
        guiGraphics.text(this.font, btnText, btnX + (previewW / 2) - (this.font.width(btnText) / 2), btnY + 6, 0xFFFFFFFF, true);

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        int windowX = (this.width - WINDOW_WIDTH) / 2;
        int windowY = (this.height - WINDOW_HEIGHT) / 2;
        double mouseX = event.x();
        double mouseY = event.y();

        int columns = 6;
        int visibleRows = 5;
        int cellW = 22;
        int cellH = 22;
        int gridX = windowX + 15;
        int gridY = windowY + 45;

        int startIndex = scrollOffset * columns;
        int endIndex = Math.min(startIndex + (columns * visibleRows), filteredEntities.size());

        for (int i = startIndex; i < endIndex; i++) {
            int index = i - startIndex;
            int slotX = gridX + (index % columns * cellW);
            int slotY = gridY + (index / columns * cellH);

            if (mouseX >= slotX && mouseX < slotX + cellW - 2 && mouseY >= slotY && mouseY < slotY + cellH - 2) {
                this.selectedEntityId = filteredEntities.get(i);
                this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }

        int trackX = gridX + (columns * cellW) + 5;
        if (mouseX >= trackX - 2 && mouseX <= trackX + 5 && mouseY >= gridY && mouseY <= gridY + (visibleRows * cellH)) {
            this.isScrolling = true;
            updateScrollbar(mouseY);
            return true;
        }

        int btnX = windowX + 175;
        int btnY = windowY + WINDOW_HEIGHT - 35;
        if (mouseX >= btnX && mouseX <= btnX + 90 && mouseY >= btnY && mouseY <= btnY + 20) {
            this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.2F));

            if (this.selectedEntityId != null) {
                com.r3ct.bestiary.platform.Services.PLATFORM.sendSetTrophyEntityPacket(this.trophyPos, this.selectedEntityId);
            }
            this.onClose();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void updateScrollbar(double mouseY) {
        int windowY = (this.height - WINDOW_HEIGHT) / 2;
        int gridY = windowY + 45;
        int trackH = 5 * 22;

        int totalRows = (int) Math.ceil((double) filteredEntities.size() / 6.0);
        int maxScroll = Math.max(0, totalRows - 5);

        if (maxScroll > 0) {
            float fraction = Mth.clamp((float)(mouseY - gridY) / trackH, 0.0f, 1.0f);
            this.scrollOffset = Math.round(fraction * maxScroll);
        }
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        if (this.isScrolling) {
            updateScrollbar(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (event.button() == 0) this.isScrolling = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int totalRows = (int) Math.ceil((double) filteredEntities.size() / 6.0);
        int maxScroll = Math.max(0, totalRows - 5);

        if (scrollY > 0 && this.scrollOffset > 0) {
            this.scrollOffset--;
            return true;
        } else if (scrollY < 0 && this.scrollOffset < maxScroll) {
            this.scrollOffset++;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}