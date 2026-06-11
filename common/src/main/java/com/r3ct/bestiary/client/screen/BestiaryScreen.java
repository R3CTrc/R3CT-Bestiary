package com.r3ct.bestiary.client.screen;

import com.r3ct.bestiary.client.data.ClientPlayerData;
import com.r3ct.bestiary.config.BestiaryConfig;
import com.r3ct.bestiary.logic.MobKillHandler;
import com.r3ct.bestiary.scanner.EntityTypeScanner;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class BestiaryScreen extends Screen {

    private static final Identifier BOOK_TEXTURE = Identifier.parse("minecraft:textures/gui/book.png");
    private static final Identifier TAB_UNSELECTED = Identifier.parse("advancements/tab_left_middle");
    private static final Identifier TAB_SELECTED = Identifier.parse("advancements/tab_left_middle_selected");
    private static final Identifier TAB_RIGHT_UNSELECTED = Identifier.parse("advancements/tab_right_middle");
    private static final Identifier TAB_RIGHT_SELECTED = Identifier.parse("advancements/tab_right_middle_selected");

    private static final int SOURCE_PAGE_SIZE = 192;
    private static final int RENDER_SIZE = 260;

    private int currentTabScroll = 0;
    private int selectedTabIndex = 0;
    private int currentRowScroll = 0;
    private int homeScroll = 0;

    private float[] tabProgressArray = new float[0];
    private long lastUpdateTime = 0L;
    private boolean isScrolling = false;

    private final List<EntityTypeScanner.CategoryData> cachedCategories = new ArrayList<>();

    private enum SpecialTab { NONE, HOME, INFO, LEADERBOARD }
    private SpecialTab activeSpecialTab = SpecialTab.HOME;

    public BestiaryScreen() {
        super(Component.translatable("gui.r3ct_bestiary.catalog.title"));
    }

    private float calculateEffectiveScale() {
        float configScale = BestiaryConfig.catalogScale;
        float maxPossibleScale = Math.min((float) this.width / (RENDER_SIZE + 60), (float) this.height / RENDER_SIZE);
        return Math.min(configScale, maxPossibleScale);
    }

    @Override
    protected void init() {
        super.init();
        BestiaryConfig.load();

        if (EntityTypeScanner.SCANNED_CATEGORIES.isEmpty()) {
            EntityTypeScanner.scanEntities();
        }

        cachedCategories.clear();
        cachedCategories.addAll(EntityTypeScanner.SCANNED_CATEGORIES.values());
        lastUpdateTime = System.currentTimeMillis();

        tabProgressArray = new float[cachedCategories.size()];
        for (int i = 0; i < cachedCategories.size(); i++) {
            EntityTypeScanner.CategoryData cat = cachedCategories.get(i);
            int gathered = getGatheredCount(cat);
            tabProgressArray[i] = cat.entityIds.isEmpty() ? 0f : (float) gathered / cat.entityIds.size();
        }

        com.r3ct.bestiary.platform.Services.PLATFORM.sendRequestLeaderboardPacketToServer();
    }

    private boolean isCompleted(String entityId) {
        int count = ClientPlayerData.killCounts.getOrDefault(entityId, 0);
        EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId)).map(net.minecraft.core.Holder::value).orElse(null);
        if (type == null) return false;

        List<Integer> thresholds = MobKillHandler.getKillThresholds(entityId, type.getCategory());
        return !thresholds.isEmpty() && count >= thresholds.get(0);
    }

    private int getGatheredCount(EntityTypeScanner.CategoryData cat) {
        int gathered = 0;
        for (String id : cat.entityIds) {
            if (isCompleted(id)) gathered++;
        }
        return gathered;
    }

    private ItemStack getCategoryIcon(EntityTypeScanner.CategoryData cat) {
        if (cat.namespace.equals("minecraft")) {
            if (cat.type.equals("bosses")) return new ItemStack(Items.ENDER_DRAGON_SPAWN_EGG);
            if (cat.type.equals("monsters")) return new ItemStack(Items.ZOMBIE_SPAWN_EGG);
            if (cat.type.equals("creatures")) return new ItemStack(Items.PIG_SPAWN_EGG);
        }

        if (!cat.entityIds.isEmpty()) {
            return getSpawnEggForEntity(cat.entityIds.get(0));
        }

        return new ItemStack(Items.SPAWNER);
    }

    private ItemStack getSpawnEggForEntity(String entityId) {
        EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId)).map(net.minecraft.core.Holder::value).orElse(null);
        if (type != null) {
            java.util.Optional<net.minecraft.core.Holder<net.minecraft.world.item.Item>> eggOptional = SpawnEggItem.byId(type);
            if (eggOptional.isPresent()) {
                return new ItemStack(eggOptional.get().value());
            }
        }

        return new ItemStack(Items.SPAWNER);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);

        float scale = calculateEffectiveScale();
        double scaledMouseX = (mouseX - this.width / 2.0) / scale + this.width / 2.0;
        double scaledMouseY = (mouseY - this.height / 2.0) / scale + this.height / 2.0;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000f;
        lastUpdateTime = currentTime;

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(this.width / 2.0f, this.height / 2.0f);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.pose().translate(-this.width / 2.0f, -this.height / 2.0f);

        int bookStartX = (this.width - RENDER_SIZE) / 2;
        int bookStartY = (this.height - RENDER_SIZE) / 2;

        renderTabs(guiGraphics, bookStartX, bookStartY, scaledMouseX, scaledMouseY, mouseX, mouseY);
        renderRightSpecialTabs(guiGraphics, bookStartX, bookStartY, scaledMouseX, scaledMouseY, mouseX, mouseY);

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_TEXTURE, bookStartX, bookStartY, 0f, 0f, RENDER_SIZE, RENDER_SIZE, SOURCE_PAGE_SIZE, SOURCE_PAGE_SIZE, 256, 256);

        if (activeSpecialTab == SpecialTab.NONE && !cachedCategories.isEmpty()) {
            renderItemGrid(guiGraphics, bookStartX, bookStartY, scaledMouseX, scaledMouseY, mouseX, mouseY, deltaTime);
        } else if (activeSpecialTab == SpecialTab.HOME) {
            renderHomeTab(guiGraphics, bookStartX, bookStartY, scaledMouseX, scaledMouseY, mouseX, mouseY, deltaTime);
        } else if (activeSpecialTab == SpecialTab.INFO) {
            renderInfoTab(guiGraphics, bookStartX, bookStartY);
        } else if (activeSpecialTab == SpecialTab.LEADERBOARD) {
            renderLeaderboardTab(guiGraphics, bookStartX, bookStartY, scaledMouseX, scaledMouseY);
        }

        guiGraphics.pose().popMatrix();
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderRightSpecialTabs(GuiGraphicsExtractor guiGraphics, int bookX, int bookY, double scaledMouseX, double scaledMouseY, int rawMouseX, int rawMouseY) {
        int tabW = 32;
        int tabH = 28;
        int baseX = bookX + RENDER_SIZE - 40;
        int startY = bookY + 20;

        SpecialTab[] tabs = {SpecialTab.HOME, SpecialTab.INFO, SpecialTab.LEADERBOARD};
        ItemStack[] icons = {new ItemStack(Items.COMPASS), new ItemStack(Items.WRITABLE_BOOK), new ItemStack(Items.MOJANG_BANNER_PATTERN)};
        String[] tooltips = {"gui.r3ct_bestiary.catalog.tab_home", "gui.r3ct_bestiary.catalog.tab_info", "gui.r3ct_bestiary.catalog.tab_leaderboard"};

        for (int i = 0; i < tabs.length; i++) {
            SpecialTab tab = tabs[i];
            int currentY = startY + (i * 32);
            boolean isSelected = (activeSpecialTab == tab);
            boolean isHovered = scaledMouseX >= baseX && scaledMouseX <= baseX + tabW && scaledMouseY >= currentY && scaledMouseY <= currentY + tabH;

            int finalX = (isHovered || isSelected) ? baseX + 2 : baseX;

            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, (isHovered || isSelected) ? TAB_RIGHT_SELECTED : TAB_RIGHT_UNSELECTED, finalX, currentY, tabW, tabH, 0xFFFFF2D4);
            guiGraphics.item(icons[i], finalX + 7, currentY + 6);

            if (isHovered) {
                guiGraphics.setTooltipForNextFrame(this.font, Component.translatable(tooltips[i]).withStyle(s -> s.withColor(0xFFD4AF37).withBold(true)), rawMouseX, rawMouseY);
            }
        }
    }

    private void renderHomeTab(GuiGraphicsExtractor guiGraphics, int bookX, int bookY, double scaledMouseX, double scaledMouseY, int rawMouseX, int rawMouseY, float deltaTime) {
        int centerX = bookX + (RENDER_SIZE / 2);
        Component title = Component.translatable("gui.r3ct_bestiary.catalog.tab_home");
        guiGraphics.text(this.font, title, centerX - (this.font.width(title) / 2) - 8, bookY + 15, 0xFF333333, false);

        int listStartY = bookY + 47;
        int visibleItems = 5;
        int rowHeight = 32;
        int maxScroll = Math.max(0, cachedCategories.size() - visibleItems);

        if (maxScroll > 0) {
            int trackX = bookX + 199;
            int trackY = bookY + 47;
            int trackH = 164;

            guiGraphics.fill(trackX, trackY, trackX + 4, trackY + trackH, 0xFF1A0A04);
            float scrollFraction = (float) homeScroll / maxScroll;
            int thumbH = Math.max(12, (int) (((float) visibleItems / cachedCategories.size()) * trackH));
            int thumbY = trackY + (int) (scrollFraction * (trackH - thumbH));
            guiGraphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, isScrolling ? 0xFFA07A5A : 0xFF8A5A3A);
        }

        for (int i = 0; i < visibleItems; i++) {
            int actualIndex = i + homeScroll;
            if (actualIndex >= cachedCategories.size()) break;

            EntityTypeScanner.CategoryData cat = cachedCategories.get(actualIndex);
            int currentY = listStartY + (i * rowHeight);

            int totalItems = cat.entityIds.size();
            int gatheredItems = getGatheredCount(cat);

            float targetProgress = totalItems > 0 ? (float) gatheredItems / totalItems : 0f;
            tabProgressArray[actualIndex] = Mth.lerp(deltaTime * 3.0f, tabProgressArray[actualIndex], targetProgress);
            float currentAnimProgress = tabProgressArray[actualIndex];
            int percent = Math.clamp(Math.round(currentAnimProgress * 100), 0, 100);

            guiGraphics.item(getCategoryIcon(cat), bookX + 48, currentY + 4);

            Component catNameComp = Component.literal(cat.getFormattedModName() + ": ")
                    .append(Component.translatable("mobcategory." + cat.type));
            guiGraphics.text(this.font, catNameComp, bookX + 73, currentY, 0xFF444444, false);

            Component countComp = Component.literal(gatheredItems + " / " + totalItems);
            Component percentComp = Component.literal(percent + "%");

            guiGraphics.text(this.font, countComp, bookX + 110, currentY + 12, 0xFF666666, false);

            int dynamicColor = percent < 33 ? 0xFFFF5555 : (percent < 66 ? 0xFFFFAA00 : 0xFF55FF55);
            guiGraphics.text(this.font, percentComp, bookX + 175, currentY + 12, dynamicColor, false);

            int barX = bookX + 73;
            int barW = 115;
            int barY = currentY + 22;
            int barH = 4;

            guiGraphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF2A1508);
            guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF1A0A04);
            int fillW = (int) (currentAnimProgress * barW);
            if (fillW > 0) {
                guiGraphics.fill(barX, barY, barX + fillW, barY + barH, dynamicColor);
                guiGraphics.fill(barX, barY, barX + fillW, barY + 1, 0x44FFFFFF);
            }
        }
    }

    private void renderInfoTab(GuiGraphicsExtractor guiGraphics, int bookX, int bookY) {
        int centerX = bookX + (RENDER_SIZE / 2);
        Component title = Component.translatable("gui.r3ct_bestiary.catalog.tab_info");
        guiGraphics.text(this.font, title, centerX - (this.font.width(title) / 2) - 8, bookY + 15, 0xFF333333, false);

        int textX = bookX + 50;
        int currentY = bookY + 40;
        int maxWidth = 150;

        currentY = drawWrappedText(guiGraphics, Component.translatable("gui.r3ct_bestiary.info.rewards_title").withStyle(net.minecraft.ChatFormatting.BOLD), textX, currentY, maxWidth, 0xFF000000);
        currentY += 5;

        currentY = drawWrappedText(guiGraphics, Component.translatable("gui.r3ct_bestiary.info.point1"), textX, currentY, maxWidth, 0xFF333333);
        currentY += 6;
        currentY = drawWrappedText(guiGraphics, Component.translatable("gui.r3ct_bestiary.info.point2"), textX, currentY, maxWidth, 0xFF333333);
    }

    private int drawWrappedText(GuiGraphicsExtractor guiGraphics, Component text, int x, int y, int maxWidth, int color) {
        java.util.List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(text, maxWidth);
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            guiGraphics.text(this.font, line, x, y, color, false);
            y += this.font.lineHeight + 2;
        }
        return y;
    }

    private void renderLeaderboardTab(GuiGraphicsExtractor guiGraphics, int bookX, int bookY, double scaledMouseX, double scaledMouseY) {
        int centerX = bookX + (RENDER_SIZE / 2);
        Component title = Component.translatable("gui.r3ct_bestiary.catalog.tab_leaderboard");
        guiGraphics.text(this.font, title, centerX - (this.font.width(title) / 2) - 8, bookY + 15, 0xFF333333, false);

        int startX = bookX + 50;
        int startY = bookY + 35;
        com.r3ct.bestiary.network.LeaderboardDataPayload.TopPlayerEntry hoveredEntry = null;

        if (ClientPlayerData.leaderboardData.isEmpty()) {
            return;
        }

        for (int i = 0; i < Math.min(10, ClientPlayerData.leaderboardData.size()); i++) {
            com.r3ct.bestiary.network.LeaderboardDataPayload.TopPlayerEntry entry = ClientPlayerData.leaderboardData.get(i);
            int y = startY + (i * 19);

            String nameColor = (i == 0) ? "§5§l" : (i == 1) ? "§6§l" : (i == 2) ? "§3§l" : "§8";
            String valColor = (i == 0) ? "§5" : (i == 1) ? "§6" : (i == 2) ? "§3" : "§8";

            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(net.minecraft.core.component.DataComponents.PROFILE, net.minecraft.world.item.component.ResolvableProfile.createUnresolved(entry.name()));
            guiGraphics.item(head, startX, y);

            guiGraphics.text(this.font, "§8" + (i + 1) + ". " + nameColor + entry.name(), startX + 20, y + 4, 0xFF333333, false);

            String scoreTxt = valColor + entry.totalCompleted();
            int scoreWidth = this.font.width(scoreTxt);
            guiGraphics.text(this.font, scoreTxt, startX + 145 - scoreWidth, y + 4, 0xFF333333, false);

            if (scaledMouseX >= startX && scaledMouseX <= startX + 155 && scaledMouseY >= y && scaledMouseY <= y + 16) {
                hoveredEntry = entry;
                guiGraphics.fill(startX - 2, y - 2, startX + 155, y + 18, 0x1A000000);
            }
        }

        if (hoveredEntry != null) {
            java.util.List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> tt = new java.util.ArrayList<>();

            tt.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("     §f§l" + hoveredEntry.name()).getVisualOrderText()));

            Component totalKillsComp = Component.translatable("gui.r3ct_bestiary.leaderboard.total_kills")
                    .withStyle(net.minecraft.ChatFormatting.GRAY)
                    .append(Component.literal(": " + hoveredEntry.totalCompleted()).withStyle(net.minecraft.ChatFormatting.YELLOW));
            tt.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(totalKillsComp.getVisualOrderText()));

            tt.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));

            for (EntityTypeScanner.CategoryData cat : cachedCategories) {
                int max = cat.entityIds.isEmpty() ? 1 : cat.entityIds.size();
                int gathered = 0;
                for (String id : cat.entityIds) {
                    if (hoveredEntry.unlockedMobs().contains(id)) gathered++;
                }

                int percent = Math.clamp(Math.round(((float) gathered / max) * 100), 0, 100);
                String colorCode = percent < 33 ? "§c" : (percent < 66 ? "§6" : "§a");

                Component catNameComp = Component.literal(cat.getFormattedModName() + ": ")
                        .append(Component.translatable("mobcategory." + cat.type));
                String line = "§7" + catNameComp.getString() + ": " + colorCode + percent + "%";
                tt.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal(line).getVisualOrderText()));
            }

            guiGraphics.tooltip(this.font, tt, (int) scaledMouseX, (int) scaledMouseY, net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, null);

            ItemStack ttHead = new ItemStack(Items.PLAYER_HEAD);
            ttHead.set(net.minecraft.core.component.DataComponents.PROFILE, net.minecraft.world.item.component.ResolvableProfile.createUnresolved(hoveredEntry.name()));
            guiGraphics.item(ttHead, (int) scaledMouseX + 11, (int) scaledMouseY - 14);
        }
    }

    private void renderTabs(GuiGraphicsExtractor guiGraphics, int bookX, int bookY, double scaledMouseX, double scaledMouseY, int rawMouseX, int rawMouseY) {
        int maxVisibleTabs = 7;
        int tabStartY = bookY + 20;
        int tabW = 32;
        int tabH = 28;
        int baseTabX = (bookX + 27) - tabW + 5;
        int arrowCenter = baseTabX + (tabW / 2);

        if (currentTabScroll > 0) {
            Component upArrow = Component.literal("▲");
            int w = this.font.width(upArrow);
            int y = tabStartY - 10;
            boolean isHoveringUp = scaledMouseX >= arrowCenter - 10 && scaledMouseX <= arrowCenter + 10 && scaledMouseY >= y - 2 && scaledMouseY <= y + 10;
            int color = isHoveringUp ? 0xFFFFFFFF : 0xFFBBBBBB;
            guiGraphics.text(this.font, upArrow, arrowCenter - (w / 2), y, color, false);

            if (isHoveringUp) {
                guiGraphics.setTooltipForNextFrame(this.font, Component.translatable("gui.r3ct_bestiary.catalog.prev_categories").withStyle(s -> s.withColor(0xFFAAAAAA)), rawMouseX, rawMouseY);
            }
        }

        if (currentTabScroll < cachedCategories.size() - maxVisibleTabs) {
            Component downArrow = Component.literal("▼");
            int w = this.font.width(downArrow);
            int y = tabStartY + (maxVisibleTabs * 30) + 2;
            boolean isHoveringDown = scaledMouseX >= arrowCenter - 10 && scaledMouseX <= arrowCenter + 10 && scaledMouseY >= y - 2 && scaledMouseY <= y + 10;
            int color = isHoveringDown ? 0xFFFFFFFF : 0xFFBBBBBB;
            guiGraphics.text(this.font, downArrow, arrowCenter - (w / 2), y, color, false);

            if (isHoveringDown) {
                guiGraphics.setTooltipForNextFrame(this.font, Component.translatable("gui.r3ct_bestiary.catalog.next_categories").withStyle(s -> s.withColor(0xFFAAAAAA)), rawMouseX, rawMouseY);
            }
        }

        for (int i = 0; i < maxVisibleTabs && (i + currentTabScroll) < cachedCategories.size(); i++) {
            int actualIndex = i + currentTabScroll;
            EntityTypeScanner.CategoryData cat = cachedCategories.get(actualIndex);
            int currentY = tabStartY + (i * 30);

            boolean isHovered = scaledMouseX >= baseTabX && scaledMouseX <= baseTabX + tabW && scaledMouseY >= currentY && scaledMouseY <= currentY + tabH;
            boolean isSelected = (activeSpecialTab == SpecialTab.NONE && actualIndex == selectedTabIndex);

            int finalX = (isHovered || isSelected) ? baseTabX - 2 : baseTabX;
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, (isHovered || isSelected) ? TAB_SELECTED : TAB_UNSELECTED, finalX, currentY, tabW, tabH, 0xFFFFF2D4);

            guiGraphics.item(getCategoryIcon(cat), finalX + 9, currentY + 6);

            if (isHovered) {
                List<Component> tabTooltip = new ArrayList<>();
                Component catNameComp = Component.literal(cat.getFormattedModName() + ": ")
                        .append(Component.translatable("mobcategory." + cat.type));
                tabTooltip.add(catNameComp.copy().withStyle(s -> s.withColor(0xFFD4AF37).withBold(true)));

                int totalCatItems = cat.entityIds.size();
                int gatheredCatItems = getGatheredCount(cat);

                float currentAnimProgress = actualIndex < tabProgressArray.length ? tabProgressArray[actualIndex] : 0f;
                int catPercent = Math.clamp(Math.round(currentAnimProgress * 100), 0, 100);
                int barColor = catPercent < 33 ? 0xFFFF5555 : (catPercent < 66 ? 0xFFFFAA00 : 0xFF55FF55);

                tabTooltip.add(Component.translatable("gui.r3ct_bestiary.catalog.gathered", gatheredCatItems, totalCatItems).withStyle(s -> s.withColor(0xFFBBBBBB)));

                int barLength = 12;
                int filled = (int) ((currentAnimProgress) * barLength);
                String filledStr = "█".repeat(filled);
                String emptyStr = "▒".repeat(barLength - filled);
                Component barComp = Component.literal(filledStr).withStyle(s -> s.withColor(barColor))
                        .append(Component.literal(emptyStr).withStyle(s -> s.withColor(0xFF444444)));

                tabTooltip.add(barComp);
                guiGraphics.setComponentTooltipForNextFrame(this.font, tabTooltip, rawMouseX, rawMouseY);
            }
        }
    }

    private void renderItemGrid(GuiGraphicsExtractor guiGraphics, int bookX, int bookY, double scaledMouseX, double scaledMouseY, int rawMouseX, int rawMouseY, float deltaTime) {
        EntityTypeScanner.CategoryData activeCat = cachedCategories.get(selectedTabIndex);
        List<String> items = activeCat.entityIds;

        int columns = 5;
        int visibleRows = 5;
        int cellW = 30;
        int cellH = 34;
        int centerX = bookX + (RENDER_SIZE / 2) - 7;

        int totalItems = items.size();
        int gatheredItems = getGatheredCount(activeCat);

        float targetProgress = totalItems > 0 ? (float) gatheredItems / totalItems : 0f;
        if (selectedTabIndex < tabProgressArray.length) {
            tabProgressArray[selectedTabIndex] = Mth.lerp(deltaTime * 3.0f, tabProgressArray[selectedTabIndex], targetProgress);
        }

        float currentAnimProgress = selectedTabIndex < tabProgressArray.length ? tabProgressArray[selectedTabIndex] : 0f;

        int percent = Math.clamp(Math.round(currentAnimProgress * 100), 0, 100);
        int dynamicColor = percent < 33 ? 0xFFFF5555 : (percent < 66 ? 0xFFFFAA00 : 0xFF55FF55);

        Component catName = Component.literal(activeCat.getFormattedModName() + ": ")
                .append(Component.translatable("mobcategory." + activeCat.type));
        guiGraphics.text(this.font, catName, centerX - (this.font.width(catName) / 2), bookY + 12, 0xFF333333, false);

        Component gatheringText = Component.translatable("gui.r3ct_bestiary.catalog.gathered_space", gatheredItems, totalItems);
        Component percentText = Component.literal("(" + percent + "%)");
        int totalTextWidth = this.font.width(gatheringText) + this.font.width(percentText);
        int startTextX = centerX - (totalTextWidth / 2);

        guiGraphics.text(this.font, gatheringText, startTextX, bookY + 23, 0xFF555555, false);
        guiGraphics.text(this.font, percentText, startTextX + this.font.width(gatheringText), bookY + 23, dynamicColor, false);

        int barW = 100;
        int barH = 4;
        int barX = centerX - (barW / 2);
        int barY = bookY + 34;
        guiGraphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF2A1508);
        guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF1A0A04);
        int fillW = (int) (currentAnimProgress * barW);
        if (fillW > 0) {
            guiGraphics.fill(barX, barY, barX + fillW, barY + barH, dynamicColor);
            guiGraphics.fill(barX, barY, barX + fillW, barY + 1, 0x44FFFFFF);
        }

        if (scaledMouseX >= barX && scaledMouseX <= barX + barW && scaledMouseY >= barY && scaledMouseY <= barY + barH) {
            guiGraphics.setTooltipForNextFrame(this.font, Component.translatable("gui.r3ct_bestiary.catalog.category_progress").withStyle(s -> s.withColor(0xFFAAAAAA)), rawMouseX, rawMouseY);
        }

        int totalRows = (int) Math.ceil((double) items.size() / columns);
        int maxScroll = Math.max(0, totalRows - visibleRows);
        int gridStartX = bookX + 49;
        int gridStartY = bookY + 46;

        if (maxScroll > 0) {
            int trackX = gridStartX + (columns * cellW) + 4;
            int trackY = gridStartY + 1;
            int trackH = (visibleRows * cellH) - 4;
            guiGraphics.fill(trackX, trackY, trackX + 4, trackY + trackH, 0xFF1A0A04);
            float scrollFraction = (float) currentRowScroll / maxScroll;
            int thumbH = Math.max(12, (int) (((float) visibleRows / totalRows) * trackH));
            int thumbY = trackY + (int) (scrollFraction * (trackH - thumbH));
            guiGraphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, isScrolling ? 0xFFA07A5A : 0xFF8A5A3A);
        }

        int startIndex = currentRowScroll * columns;
        int endIndex = Math.min(startIndex + (columns * visibleRows), items.size());

        for (int i = startIndex; i < endIndex; i++) {
            int index = i - startIndex;
            int slotX = gridStartX + (index % columns * cellW);
            int slotY = gridStartY + (index / columns * cellH);

            int bgX = slotX + 6;
            int bgY = slotY;

            String entityId = items.get(i);
            EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId)).map(net.minecraft.core.Holder::value).orElse(null);
            ItemStack stack = getSpawnEggForEntity(entityId);

            int currentKills = ClientPlayerData.killCounts.getOrDefault(entityId, 0);

            List<Integer> thresholds = type != null ? MobKillHandler.getKillThresholds(entityId, type.getCategory()) : java.util.Collections.singletonList(1);
            int baseReq = thresholds.size() > 0 ? thresholds.get(0) : 1;
            int star1Req = thresholds.size() > 1 ? thresholds.get(1) : baseReq;
            int star2Req = thresholds.size() > 2 ? thresholds.get(2) : star1Req;
            int star3Req = thresholds.size() > 3 ? thresholds.get(3) : star2Req;

            int targetReq;
            int starLevel = 0;
            boolean isCollected = currentKills >= baseReq;

            if (currentKills >= star3Req && star3Req > baseReq) {
                starLevel = 3;
                targetReq = star3Req;
            } else if (currentKills >= star2Req && star2Req > baseReq) {
                starLevel = 2;
                targetReq = star3Req;
            } else if (currentKills >= star1Req && star1Req > baseReq) {
                starLevel = 1;
                targetReq = star2Req;
            } else if (currentKills >= baseReq) {
                starLevel = 0;
                targetReq = star1Req;
            } else {
                starLevel = 0;
                targetReq = baseReq;
            }

            if (thresholds.size() == 1) {
                targetReq = baseReq;
            }

            int displayKills = Math.min(currentKills, targetReq);

            guiGraphics.fill(bgX, bgY, bgX + 18, bgY + 18, 0x1A3F220B);
            guiGraphics.fill(bgX, bgY, bgX + 18, bgY + 1, 0x2A3F220B);
            guiGraphics.fill(bgX, bgY, bgX + 1, bgY + 18, 0x2A3F220B);

            Component gridIcon = null;
            Component tooltipIcon = null;
            int finalIconColor;
            int progressColor;

            if (isCollected) {
                gridIcon = Component.literal("✔");
                tooltipIcon = Component.literal("✔");
                finalIconColor = 0xFF55FF55;

                if (starLevel == 3) {
                    progressColor = 0xFFFFD700;
                } else {
                    progressColor = 0xFFFFAA00;
                }
            } else if (currentKills > 0) {
                gridIcon = null;
                tooltipIcon = Component.literal("✘");
                finalIconColor = 0xFFFF5555;
                progressColor = 0xFFFFAA00;
            } else {
                gridIcon = null;
                tooltipIcon = Component.literal("✘");
                finalIconColor = 0xFFFF5555;
                progressColor = 0xFFFF5555;
            }

            int itemX = bgX + 1;
            int itemY = bgY + 1;
            guiGraphics.item(stack, itemX, itemY);

            if (gridIcon != null) {
                guiGraphics.fill(itemX, itemY, itemX + 16, itemY + 16, 0x66000000);
                int iconW = this.font.width(gridIcon);

                int checkY = starLevel > 0 ? itemY + 1 : itemY + 4;
                guiGraphics.text(this.font, gridIcon, itemX + 8 - (iconW / 2), checkY, finalIconColor, true);

                if (starLevel > 0) {
                    String stars = "★".repeat(starLevel);
                    int starW = this.font.width(stars);
                    guiGraphics.text(this.font, stars, itemX + 8 - (starW / 2), itemY + 9, 0xFFFFD700, true);

                    tooltipIcon = tooltipIcon.copy().append(Component.literal(" " + stars).withStyle(s -> s.withColor(0xFFFFD700)));
                }
            }

            String progressTxt = displayKills + "/" + targetReq;
            int progressW = this.font.width(progressTxt);
            int progressX = slotX + (cellW - progressW) / 2;
            int progressY = bgY + 22;

            guiGraphics.text(this.font, progressTxt, progressX, progressY, progressColor, false);

            if (scaledMouseX >= slotX && scaledMouseX < slotX + cellW && scaledMouseY >= slotY && scaledMouseY < slotY + cellH) {
                List<Component> itemTooltip = new ArrayList<>();
                Component originalName = type != null ? type.getDescription() : Component.literal(entityId);

                Component modifiedName = originalName.copy().append(Component.literal(" "))
                        .append(tooltipIcon.copy().withStyle(s -> s.withColor(finalIconColor).withBold(true)));

                itemTooltip.add(modifiedName);
                guiGraphics.setComponentTooltipForNextFrame(this.font, itemTooltip, rawMouseX, rawMouseY);
            }
        }
    }

    private void updateScrollbar(double mouseY) {
        int trackY = ((this.height - RENDER_SIZE) / 2) + 47;

        if (activeSpecialTab == SpecialTab.HOME) {
            int trackH = 164;
            int maxScroll = Math.max(0, cachedCategories.size() - 5);
            if (maxScroll > 0) {
                float fraction = Mth.clamp((float)(mouseY - trackY) / trackH, 0.0f, 1.0f);
                homeScroll = Math.round(fraction * maxScroll);
            }
        } else if (activeSpecialTab == SpecialTab.NONE && !cachedCategories.isEmpty()) {
            int trackH = 166;
            EntityTypeScanner.CategoryData cat = cachedCategories.get(selectedTabIndex);
            int maxScroll = Math.max(0, (int) Math.ceil(cat.entityIds.size() / 5.0) - 5);
            if (maxScroll > 0) {
                float fraction = Mth.clamp((float)(mouseY - trackY) / trackH, 0.0f, 1.0f);
                currentRowScroll = Math.round(fraction * maxScroll);
            }
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        float scale = calculateEffectiveScale();
        double mouseX = (event.x() - this.width / 2.0) / scale + this.width / 2.0;
        double mouseY = (event.y() - this.height / 2.0) / scale + this.height / 2.0;

        int bookStartX = (this.width - RENDER_SIZE) / 2;
        int bookStartY = (this.height - RENDER_SIZE) / 2;

        int rightTabX = bookStartX + RENDER_SIZE - 40;
        SpecialTab[] tabs = {SpecialTab.HOME, SpecialTab.INFO, SpecialTab.LEADERBOARD};
        for (int i = 0; i < tabs.length; i++) {
            if (mouseX >= rightTabX && mouseX <= rightTabX + 32 && mouseY >= bookStartY + 20 + (i * 32) && mouseY <= bookStartY + 20 + (i * 32) + 28) {
                activeSpecialTab = tabs[i];
                this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }

        int tabStartX = bookStartX + 27 - 32 + 5;
        int tabStartY = bookStartY + 20;

        for (int i = 0; i < 7 && (i + currentTabScroll) < cachedCategories.size(); i++) {
            if (mouseX >= tabStartX && mouseX <= tabStartX + 32 && mouseY >= tabStartY + (i * 30) && mouseY <= tabStartY + (i * 30) + 28) {
                int newTab = i + currentTabScroll;
                selectedTabIndex = newTab;
                currentRowScroll = 0;
                activeSpecialTab = SpecialTab.NONE;
                this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }

        int arrowCenter = tabStartX + 16;
        if (currentTabScroll > 0 && mouseX >= arrowCenter - 10 && mouseX <= arrowCenter + 10 && mouseY >= tabStartY - 10 && mouseY <= tabStartY + 2) {
            currentTabScroll--; currentRowScroll = 0;
            this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        if (currentTabScroll < cachedCategories.size() - 7 && mouseX >= arrowCenter - 10 && mouseX <= arrowCenter + 10 && mouseY >= tabStartY + 212 && mouseY <= tabStartY + 224) {
            currentTabScroll++; currentRowScroll = 0;
            this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        if (activeSpecialTab == SpecialTab.HOME) {
            int trackX = bookStartX + 199;
            if (mouseX >= trackX - 2 && mouseX <= trackX + 6 && mouseY >= bookStartY + 47 && mouseY <= bookStartY + 47 + 164) {
                isScrolling = true; updateScrollbar(mouseY); return true;
            }
        } else if (activeSpecialTab == SpecialTab.NONE) {
            int trackX = bookStartX + 199;
            if (mouseX >= trackX - 2 && mouseX <= trackX + 6 && mouseY >= bookStartY + 47 && mouseY <= bookStartY + 47 + 166) {
                isScrolling = true; updateScrollbar(mouseY); return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        float scale = calculateEffectiveScale();
        double mouseY = (event.y() - this.height / 2.0) / scale + this.height / 2.0;

        if (isScrolling) { updateScrollbar(mouseY); return true; }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (event.button() == 0) isScrolling = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double rawMouseX, double rawMouseY, double scrollX, double scrollY) {
        float scale = calculateEffectiveScale();
        double mouseX = (rawMouseX - this.width / 2.0) / scale + this.width / 2.0;
        int bookStartX = (this.width - RENDER_SIZE) / 2;

        if (mouseX < bookStartX + 50) {
            if (scrollY > 0 && currentTabScroll > 0) currentTabScroll--;
            else if (scrollY < 0 && currentTabScroll < cachedCategories.size() - 7) currentTabScroll++;
            return true;
        }

        if (activeSpecialTab == SpecialTab.HOME) {
            int maxScroll = Math.max(0, cachedCategories.size() - 5);
            if (scrollY > 0 && homeScroll > 0) homeScroll--;
            else if (scrollY < 0 && homeScroll < maxScroll) homeScroll++;
            return true;
        } else if (activeSpecialTab == SpecialTab.NONE) {
            EntityTypeScanner.CategoryData cat = cachedCategories.get(selectedTabIndex);
            int maxScroll = Math.max(0, (int) Math.ceil(cat.entityIds.size() / 5.0) - 5);
            if (scrollY > 0 && currentRowScroll > 0) currentRowScroll--;
            else if (scrollY < 0 && currentRowScroll < maxScroll) currentRowScroll++;
        }

        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (com.r3ct.bestiary.platform.Services.PLATFORM.isCatalogKey(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}