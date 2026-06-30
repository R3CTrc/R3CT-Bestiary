package com.r3ct.bestiary.client.screen;

import com.r3ct.bestiary.client.data.ClientPlayerData;
import com.r3ct.bestiary.config.BestiaryConfig;
import com.r3ct.bestiary.logic.MobProgressHandler;
import com.r3ct.bestiary.scanner.EntityTypeScanner;
import net.minecraft.ChatFormatting;
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
    private int detailsScroll = 0;
    private int maxDetailsScroll = 0;

    private float[] tabProgressArray = new float[0];
    private long lastUpdateTime = 0L;
    private boolean isScrolling = false;

    private final List<EntityTypeScanner.CategoryData> cachedCategories = new ArrayList<>();
    private final java.util.Map<String, net.minecraft.world.entity.LivingEntity> dummyCache = new java.util.HashMap<>();

    private enum SpecialTab { NONE, HOME, INFO, LEADERBOARD, DETAILS }
    private SpecialTab activeSpecialTab = SpecialTab.HOME;
    private String selectedEntityId = null;

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

        if (this.minecraft != null && this.minecraft.player != null && this.minecraft.player.isCreative()) {
            this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                    Component.literal("DEV: Wymaksuj"),
                    btn -> {
                        if (!cachedCategories.isEmpty()) {
                            // Zmienna selectedTabIndex zawsze przechowuje aktualnie otwartą zakładkę,
                            // więc bierzemy z niej categoryId i wysyłamy do serwera.
                            String currentCategoryId = cachedCategories.get(selectedTabIndex).categoryId;
                            com.r3ct.bestiary.platform.Services.PLATFORM.sendDebugCompleteCategoryPacket(currentCategoryId);
                        }
                    }
            ).bounds(10, 10, 100, 20).build());
        }
    }

    private boolean isCompleted(String entityId) {
        int count = ClientPlayerData.killCounts.getOrDefault(entityId, 0);
        EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId)).map(net.minecraft.core.Holder::value).orElse(null);
        if (type == null) return false;

        List<Integer> thresholds = MobProgressHandler.getProgressThresholds(entityId, type.getCategory());
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

    private net.minecraft.world.entity.LivingEntity getOrCreateDummy(String entityId) {
        if (dummyCache.containsKey(entityId)) return dummyCache.get(entityId);
        if (this.minecraft == null || this.minecraft.level == null) return null;

        EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId)).map(net.minecraft.core.Holder::value).orElse(null);
        if (type != null) {
            try {
                net.minecraft.world.entity.Entity entity = type.create(this.minecraft.level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (entity instanceof net.minecraft.world.entity.LivingEntity living) {

                    com.r3ct.bestiary.network.MobBaseStats serverStats = ClientPlayerData.serverMobStats.get(entityId);
                    if (serverStats != null) {
                        living.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, serverStats.mainHandItem());
                        var hpAttr = living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                        if (hpAttr != null) hpAttr.setBaseValue(serverStats.maxHealth());
                        var dmgAttr = living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                        if (dmgAttr != null) dmgAttr.setBaseValue(serverStats.attackDamage());
                    }

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
        } else if (activeSpecialTab == SpecialTab.DETAILS && selectedEntityId != null) {
            renderDetailsTab(guiGraphics, bookStartX, bookStartY, scaledMouseX, scaledMouseY, mouseX, mouseY);
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
        Component title = Component.translatable("gui.r3ct_bestiary.catalog.tab_home").withStyle(ChatFormatting.DARK_GRAY);
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

        currentY = drawWrappedText(guiGraphics, Component.translatable("gui.r3ct_bestiary.info.rewards_title").withStyle(ChatFormatting.BOLD), textX, currentY, maxWidth, 0xFF000000);
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

            ChatFormatting nameFormat = (i == 0) ? ChatFormatting.DARK_PURPLE : (i == 1) ? ChatFormatting.GOLD : (i == 2) ? ChatFormatting.DARK_AQUA : ChatFormatting.DARK_GRAY;

            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(net.minecraft.core.component.DataComponents.PROFILE, net.minecraft.world.item.component.ResolvableProfile.createUnresolved(entry.name()));
            guiGraphics.item(head, startX, y);

            Component namePart = Component.literal(entry.name()).withStyle(nameFormat);
            if (i < 3) namePart = namePart.copy().withStyle(ChatFormatting.BOLD);

            Component entryName = Component.literal((i + 1) + ". ").withStyle(ChatFormatting.DARK_GRAY).append(namePart);
            guiGraphics.text(this.font, entryName, startX + 20, y + 4, 0xFFFFFFFF, false);

            Component scoreTxt = Component.literal(String.valueOf(entry.totalCompleted())).withStyle(nameFormat);
            int scoreWidth = this.font.width(scoreTxt);
            guiGraphics.text(this.font, scoreTxt, startX + 145 - scoreWidth, y + 4, 0xFFFFFFFF, false);

            if (scaledMouseX >= startX && scaledMouseX <= startX + 155 && scaledMouseY >= y && scaledMouseY <= y + 16) {
                hoveredEntry = entry;
                guiGraphics.fill(startX - 2, y - 2, startX + 155, y + 18, 0x1A000000);
            }
        }

        if (hoveredEntry != null) {
            java.util.List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> tt = new java.util.ArrayList<>();

            tt.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(
                    Component.literal("     ").append(Component.literal(hoveredEntry.name()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)).getVisualOrderText()
            ));

            Component totalKillsComp = Component.translatable("gui.r3ct_bestiary.leaderboard.total_kills")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(": " + hoveredEntry.totalCompleted()).withStyle(ChatFormatting.YELLOW));
            tt.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(totalKillsComp.getVisualOrderText()));

            tt.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(
                    Component.literal("----------------").withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText()
            ));

            for (EntityTypeScanner.CategoryData cat : cachedCategories) {
                int max = cat.entityIds.isEmpty() ? 1 : cat.entityIds.size();
                int gathered = 0;
                for (String id : cat.entityIds) {
                    if (hoveredEntry.unlockedMobs().contains(id)) gathered++;
                }

                int percent = Math.clamp(Math.round(((float) gathered / max) * 100), 0, 100);
                ChatFormatting colorCode = percent < 33 ? ChatFormatting.RED : (percent < 66 ? ChatFormatting.GOLD : ChatFormatting.GREEN);

                Component catNameComp = Component.literal(cat.getFormattedModName() + ": ")
                        .append(Component.translatable("mobcategory." + cat.type));

                Component line = catNameComp.copy().withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(percent + "%").withStyle(colorCode));

                tt.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(line.getVisualOrderText()));
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
            boolean isSelected = ((activeSpecialTab == SpecialTab.NONE || activeSpecialTab == SpecialTab.DETAILS) && actualIndex == selectedTabIndex);

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

            List<Integer> thresholds = type != null ? MobProgressHandler.getProgressThresholds(entityId, type.getCategory()) : java.util.Collections.singletonList(1);
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
                progressColor = (starLevel == 3) ? 0xFF55FF55 : 0xFFFFAA00;
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
                    guiGraphics.pose().pushMatrix();
                    float totalWidth = starLevel * 8.0f;
                    float startX = itemX + 8.0f - (totalWidth / 2.0f);

                    guiGraphics.pose().translate(startX, itemY + 10.0f);
                    guiGraphics.pose().scale(0.5f, 0.5f);

                    for (int s = 0; s < starLevel; s++) {
                        guiGraphics.item(new ItemStack(Items.MOJANG_BANNER_PATTERN), s * 16, 0);
                    }
                    guiGraphics.pose().popMatrix();
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

                if (type != null) {
                    String bestiaryCat = MobProgressHandler.getBestiaryCategory(entityId, type.getCategory());
                    if (bestiaryCat.equals("creatures")) {
                        itemTooltip.add(Component.translatable("gui.r3ct_bestiary.catalog.action.any").withStyle(ChatFormatting.GREEN));
                    } else {
                        itemTooltip.add(Component.translatable("gui.r3ct_bestiary.catalog.action.kill").withStyle(ChatFormatting.RED));
                    }
                }

                itemTooltip.add(Component.literal(" "));

                int totalPages = (isCollected ? 1 : 0) + starLevel;
                ChatFormatting pageColor = (starLevel == 3) ? ChatFormatting.GREEN : ChatFormatting.YELLOW;

                Component pageText = Component.translatable("gui.r3ct_bestiary.catalog.gathered_pages",
                                Component.literal(String.valueOf(totalPages)).withStyle(pageColor))
                        .withStyle(ChatFormatting.GRAY);

                itemTooltip.add(pageText);
                itemTooltip.add(Component.translatable("gui.r3ct_bestiary.catalog.click_to_open").withStyle(ChatFormatting.DARK_GRAY));

                guiGraphics.setComponentTooltipForNextFrame(this.font, itemTooltip, rawMouseX, rawMouseY);
            }
        }
    }

    private void renderDetailsTab(GuiGraphicsExtractor guiGraphics, int bookX, int bookY, double scaledMouseX, double scaledMouseY, int rawMouseX, int rawMouseY) {
        if (selectedEntityId == null) return;

        EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(selectedEntityId)).map(net.minecraft.core.Holder::value).orElse(null);
        if (type == null) return;

        net.minecraft.world.entity.LivingEntity dummy = getOrCreateDummy(selectedEntityId);
        int currentKills = ClientPlayerData.killCounts.getOrDefault(selectedEntityId, 0);
        List<Integer> thresholds = MobProgressHandler.getProgressThresholds(selectedEntityId, type.getCategory());

        int baseReq = thresholds.size() > 0 ? thresholds.get(0) : 1;
        int star1Req = thresholds.size() > 1 ? thresholds.get(1) : baseReq;
        int star2Req = thresholds.size() > 2 ? thresholds.get(2) : star1Req;
        int star3Req = thresholds.size() > 3 ? thresholds.get(3) : star2Req;

        boolean isCollected = currentKills >= baseReq;
        int starLevel = 0;
        if (currentKills >= star3Req && star3Req > baseReq) starLevel = 3;
        else if (currentKills >= star2Req && star2Req > baseReq) starLevel = 2;
        else if (currentKills >= star1Req && star1Req > baseReq) starLevel = 1;

        int totalPages = (isCollected ? 1 : 0) + starLevel;
        int centerX = bookX + (RENDER_SIZE / 2);

        // ==== GÓRA: WYCENTROWANY MODEL 3D ====
        int boxX0 = centerX - 60;
        int boxY0 = bookY + 20;
        int boxX1 = centerX + 60;
        int boxY1 = bookY + 100;

        guiGraphics.fill(boxX0, boxY0, boxX1, boxY1, 0x11000000);

        if (dummy != null && isCollected) {
            float maxDim = Math.max(dummy.getBbWidth(), dummy.getBbHeight());
            int scale = (int) (40 / Math.max(1.0f, maxDim / 1.5f));

            net.minecraft.client.gui.screens.inventory.InventoryScreen.extractEntityInInventoryFollowsMouse(
                    guiGraphics, boxX0 + 2, boxY0 + 2, boxX1 - 2, boxY1 - 2,
                    scale, 0.0625F, (float) scaledMouseX, (float) scaledMouseY, dummy
            );
        } else {
            Component unknown = Component.literal("?").withStyle(ChatFormatting.GOLD);
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate((float)centerX, (float)(boxY0 + 40));
            guiGraphics.pose().scale(4.0f, 4.0f);
            guiGraphics.text(this.font, unknown, -this.font.width(unknown) / 2, -this.font.lineHeight / 2, 0xFFFFFFFF, false);
            guiGraphics.pose().popMatrix();
        }

        // ==== POD MODELEM: NAZWA I POSTĘP ====
        Component title = type.getDescription();
        guiGraphics.text(this.font, title, centerX - (this.font.width(title) / 2), bookY + 105, 0xFF000000, false);

        ChatFormatting pageColor = (starLevel == 3) ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
        Component progressText = Component.translatable("gui.r3ct_bestiary.catalog.gathered_pages",
                        Component.literal(String.valueOf(totalPages)).withStyle(pageColor))
                .withStyle(ChatFormatting.DARK_GRAY);

        int textW = this.font.width(progressText);
        int iconW = 10;
        int totalW = textW + iconW + 2;
        int startX = centerX - (totalW / 2);

        guiGraphics.text(this.font, progressText, startX, bookY + 118, 0xFFFFFFFF, false);

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(startX + textW + 2, bookY + 116);
        guiGraphics.pose().scale(0.6f, 0.6f);
        guiGraphics.item(new ItemStack(Items.MOJANG_BANNER_PATTERN), 0, 0);
        guiGraphics.pose().popMatrix();

        // ==== DÓŁ: SCROLLOWANE STATYSTYKI ====
        int textX = bookX + 50;
        int listStartY = bookY + 135;
        int listHeight = 80;

        if (maxDetailsScroll > 0) {
            int trackX = bookX + 199;
            int trackY = listStartY;
            guiGraphics.fill(trackX, trackY, trackX + 4, trackY + listHeight, 0xFF1A0A04);
            float scrollFraction = (float) detailsScroll / maxDetailsScroll;
            int thumbH = Math.max(12, (int) (((float) listHeight / (listHeight + maxDetailsScroll)) * listHeight));
            int thumbY = trackY + (int) (scrollFraction * (listHeight - thumbH));
            guiGraphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, isScrolling ? 0xFFA07A5A : 0xFF8A5A3A);
        }

        guiGraphics.enableScissor(bookX + 40, listStartY, bookX + 195, listStartY + listHeight);

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(0, -detailsScroll);

        int currentY = listStartY;

        // 1. ZALICZENIE
        Component infoTitle = Component.translatable("gui.r3ct_bestiary.details.general_info").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_GRAY);
        guiGraphics.text(this.font, infoTitle, textX, currentY, 0xFFFFFFFF, false); currentY += 12;

        if (totalPages >= 1) {
            String typeKey = "gui.r3ct_bestiary.family.default";
            var holder = type.builtInRegistryHolder();
            if (holder.is(net.minecraft.tags.EntityTypeTags.UNDEAD)) typeKey = "gui.r3ct_bestiary.family.undead";
            else if (holder.is(net.minecraft.tags.EntityTypeTags.ARTHROPOD)) typeKey = "gui.r3ct_bestiary.family.arthropod";
            else if (holder.is(net.minecraft.tags.EntityTypeTags.AQUATIC)) typeKey = "gui.r3ct_bestiary.family.aquatic";
            else if (holder.is(net.minecraft.tags.EntityTypeTags.ILLAGER)) typeKey = "gui.r3ct_bestiary.family.illager";
            else if (holder.is(net.minecraft.tags.EntityTypeTags.SKELETONS)) typeKey = "gui.r3ct_bestiary.family.skeleton";
            else if (type.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) typeKey = "gui.r3ct_bestiary.family.monster";
            else if (type.getCategory() == net.minecraft.world.entity.MobCategory.CREATURE) typeKey = "gui.r3ct_bestiary.family.creature";

            Component familyComp = Component.translatable("gui.r3ct_bestiary.details.family").withStyle(ChatFormatting.GRAY)
                    .append(Component.translatable(typeKey).withStyle(ChatFormatting.BLACK));
            guiGraphics.text(this.font, familyComp, textX, currentY, 0xFFFFFFFF, false);
        } else {
            Component unknownFamily = Component.translatable("gui.r3ct_bestiary.details.family").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("?").withStyle(ChatFormatting.GOLD));
            guiGraphics.text(this.font, unknownFamily, textX, currentY, 0xFFFFFFFF, false);
        }
        currentY += 16;

        // 2. PIERWSZA GWIAZDKA
        Component bodyTitle = Component.translatable("gui.r3ct_bestiary.details.body_structure").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_GRAY);
        guiGraphics.text(this.font, bodyTitle, textX, currentY, 0xFFFFFFFF, false); currentY += 12;

        if (totalPages >= 2 && dummy != null) {
            int hp = Math.round(dummy.getMaxHealth());
            int armor = dummy.getArmorValue();
            String size = String.format(java.util.Locale.US, "%.1fm x %.1fm", dummy.getBbWidth(), dummy.getBbHeight());

            Component hpComp = Component.translatable("gui.r3ct_bestiary.details.health").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(hp + " ❤").withStyle(ChatFormatting.RED));
            guiGraphics.text(this.font, hpComp, textX, currentY, 0xFFFFFFFF, false); currentY += 10;

            Component armorComp = Component.translatable("gui.r3ct_bestiary.details.armor").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(armor + " 🛡").withStyle(ChatFormatting.BLUE));
            guiGraphics.text(this.font, armorComp, textX, currentY, 0xFFFFFFFF, false); currentY += 10;

            Component sizeComp = Component.translatable("gui.r3ct_bestiary.details.dimensions").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(size).withStyle(ChatFormatting.BLACK));
            guiGraphics.text(this.font, sizeComp, textX, currentY, 0xFFFFFFFF, false);
        } else {
            Component hpUnk = Component.translatable("gui.r3ct_bestiary.details.health").withStyle(ChatFormatting.GRAY).append(Component.literal("?").withStyle(ChatFormatting.GOLD));
            Component armorUnk = Component.translatable("gui.r3ct_bestiary.details.armor").withStyle(ChatFormatting.GRAY).append(Component.literal("?").withStyle(ChatFormatting.GOLD));
            Component dimUnk = Component.translatable("gui.r3ct_bestiary.details.dimensions").withStyle(ChatFormatting.GRAY).append(Component.literal("?").withStyle(ChatFormatting.GOLD));

            guiGraphics.text(this.font, hpUnk, textX, currentY, 0xFFFFFFFF, false); currentY += 10;
            guiGraphics.text(this.font, armorUnk, textX, currentY, 0xFFFFFFFF, false); currentY += 10;
            guiGraphics.text(this.font, dimUnk, textX, currentY, 0xFFFFFFFF, false);
        }
        currentY += 16;

        // 3. DRUGA GWIAZDKA
        Component habitsTitle = Component.translatable("gui.r3ct_bestiary.details.habits_and_attack").withStyle(net.minecraft.ChatFormatting.BOLD, net.minecraft.ChatFormatting.DARK_GRAY);
        guiGraphics.text(this.font, habitsTitle, textX, currentY, 0xFFFFFFFF, false); currentY += 12;

        if (totalPages >= 3 && dummy != null) {
            Component dmgVal = Component.translatable("gui.r3ct_bestiary.details.none");

            // 1. Odczytujemy dokładny bazowy atak (skaluje się z każdym modowanym mieczem czy siłą)
            var dmgAttr = dummy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            double rawDamage = (dmgAttr != null) ? dmgAttr.getValue() : 0.0;

            // 2. Pobieramy informacje o tagach z nowego systemu
            var holder = type.builtInRegistryHolder();

            // 3. Badamy broń (wyłapie modowane pistolety, łuki, włócznie dziedziczące po ProjectileWeaponItem)
            ItemStack mainHand = dummy.getMainHandItem();
            ItemStack offHand = dummy.getOffhandItem();
            boolean hasRangedWeaponMain = mainHand.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem || mainHand.is(Items.TRIDENT);
            boolean hasRangedWeaponOff = offHand.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem || offHand.is(Items.TRIDENT);

            // 4. Kaskadowa logika przypisywania!
            if (holder.is(com.r3ct.bestiary.logic.ModTags.ATTACK_EXPLOSIVE)) {
                // Datapack powiedział, że to mob wybuchowy
                dmgVal = Component.translatable("gui.r3ct_bestiary.details.explosion");

            } else if (holder.is(com.r3ct.bestiary.logic.ModTags.ATTACK_SONIC)) {
                // Warden i inni krzykacze z modów - łączymy dynamiczny melee z dopiskiem Sonic
                dmgVal = Component.literal(Math.round(rawDamage) + " ⚔").append(Component.translatable("gui.r3ct_bestiary.details.sonic_boom"));

            } else if (holder.is(com.r3ct.bestiary.logic.ModTags.ATTACK_MAGIC) || dummy instanceof net.minecraft.world.entity.monster.illager.SpellcasterIllager) {
                // Datapack mówi "Magia" LUB implementuje interfejs rzucania czarów (nawet jeśli to nowy illager z moda)
                dmgVal = Component.translatable("gui.r3ct_bestiary.details.magic");

            } else if (hasRangedWeaponMain) {
                // Wyciąga nazwę broni z głównej ręki (zostaje ikonka łuku, podmienia nazwę)
                dmgVal = Component.literal("🏹 ").append(mainHand.getHoverName());

            } else if (hasRangedWeaponOff) {
                // Wyciąga nazwę broni z lewej ręki
                dmgVal = Component.literal("🏹 ").append(offHand.getHoverName());

            } else if (dummy instanceof net.minecraft.world.entity.monster.RangedAttackMob) {
                // Plujące moby bez broni w ręku (Śnieżny Golem, Lama z Vanilla i modów)
                dmgVal = Component.translatable("gui.r3ct_bestiary.details.ranged");

            } else if (rawDamage > 0) {
                // Mob walczy wręcz i ma zadawać obrażenia (Czysta, dokładna wartość!)
                dmgVal = Component.literal(Math.round(rawDamage) + " ⚔");
            }

            // --- Zasięg i prędkość pozostają niezmienione ---
            Component rangeVal = Component.translatable("gui.r3ct_bestiary.details.none");
            var rangeAttr = dummy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
            if (rangeAttr != null) {
                rangeVal = Component.literal(Math.round(rangeAttr.getValue()) + " ").append(Component.translatable("gui.r3ct_bestiary.details.blocks"));
            }

            Component speedVal = Component.translatable("gui.r3ct_bestiary.details.none");
            var speedAttr = dummy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            if (speedAttr != null) {
                speedVal = Component.translatable("gui.r3ct_bestiary.details.blocks_per_sec", String.format(java.util.Locale.US, "%.1f", speedAttr.getValue() * 10.0));
            }

            // Rysowanie z kopiami by uniknąć crashy
            Component dmgComp = Component.translatable("gui.r3ct_bestiary.details.damage").withStyle(net.minecraft.ChatFormatting.GRAY).append(dmgVal.copy().withStyle(net.minecraft.ChatFormatting.RED));
            guiGraphics.text(this.font, dmgComp, textX, currentY, 0xFFFFFFFF, false); currentY += 10;

            Component rangeComp = Component.translatable("gui.r3ct_bestiary.details.vision").withStyle(net.minecraft.ChatFormatting.GRAY).append(rangeVal.copy().withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
            guiGraphics.text(this.font, rangeComp, textX, currentY, 0xFFFFFFFF, false); currentY += 10;

            Component speedComp = Component.translatable("gui.r3ct_bestiary.details.speed").withStyle(net.minecraft.ChatFormatting.GRAY).append(speedVal.copy().withStyle(net.minecraft.ChatFormatting.BLACK));
            guiGraphics.text(this.font, speedComp, textX, currentY, 0xFFFFFFFF, false);
        } else {
            Component dmgUnk = Component.translatable("gui.r3ct_bestiary.details.damage").withStyle(net.minecraft.ChatFormatting.GRAY).append(Component.literal("?").withStyle(net.minecraft.ChatFormatting.GOLD));
            Component rangeUnk = Component.translatable("gui.r3ct_bestiary.details.vision").withStyle(net.minecraft.ChatFormatting.GRAY).append(Component.literal("?").withStyle(net.minecraft.ChatFormatting.GOLD));
            Component speedUnk = Component.translatable("gui.r3ct_bestiary.details.speed").withStyle(net.minecraft.ChatFormatting.GRAY).append(Component.literal("?").withStyle(net.minecraft.ChatFormatting.GOLD));

            guiGraphics.text(this.font, dmgUnk, textX, currentY, 0xFFFFFFFF, false); currentY += 10;
            guiGraphics.text(this.font, rangeUnk, textX, currentY, 0xFFFFFFFF, false); currentY += 10;
            guiGraphics.text(this.font, speedUnk, textX, currentY, 0xFFFFFFFF, false);
        }
        currentY += 16;

        // 4. TRZECIA GWIAZDKA
        Component resistTitle = Component.translatable("gui.r3ct_bestiary.details.resistances").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_GRAY);
        guiGraphics.text(this.font, resistTitle, textX, currentY, 0xFFFFFFFF, false); currentY += 12;

        if (totalPages >= 4 && dummy != null) {
            Component fireVal = dummy.fireImmune() ? Component.translatable("gui.r3ct_bestiary.details.yes").withStyle(ChatFormatting.DARK_GREEN) : Component.translatable("gui.r3ct_bestiary.details.no").withStyle(ChatFormatting.RED);

            String kbStr = "0%";
            var kbAttr = dummy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
            if (kbAttr != null) kbStr = ((int) Math.round(kbAttr.getValue() * 100)) + "%";

            Component fireComp = Component.translatable("gui.r3ct_bestiary.details.fire").withStyle(ChatFormatting.GRAY).append(fireVal);
            guiGraphics.text(this.font, fireComp, textX, currentY, 0xFFFFFFFF, false); currentY += 10;

            Component kbComp = Component.translatable("gui.r3ct_bestiary.details.knockback").withStyle(ChatFormatting.GRAY).append(Component.literal(kbStr).withStyle(ChatFormatting.DARK_PURPLE));
            guiGraphics.text(this.font, kbComp, textX, currentY, 0xFFFFFFFF, false);
        } else {
            Component fireUnk = Component.translatable("gui.r3ct_bestiary.details.fire").withStyle(ChatFormatting.GRAY).append(Component.literal("?").withStyle(ChatFormatting.GOLD));
            Component kbUnk = Component.translatable("gui.r3ct_bestiary.details.knockback").withStyle(ChatFormatting.GRAY).append(Component.literal("?").withStyle(ChatFormatting.GOLD));

            guiGraphics.text(this.font, fireUnk, textX, currentY, 0xFFFFFFFF, false); currentY += 10;
            guiGraphics.text(this.font, kbUnk, textX, currentY, 0xFFFFFFFF, false);
        }

        currentY += 10;

        guiGraphics.pose().popMatrix();
        guiGraphics.disableScissor();

        maxDetailsScroll = Math.max(0, (currentY - listStartY) - listHeight);
    }

    private void updateScrollbar(double mouseY) {
        int bookStartY = ((this.height - RENDER_SIZE) / 2);

        if (activeSpecialTab == SpecialTab.HOME) {
            int trackY = bookStartY + 47;
            int trackH = 164;
            int maxScroll = Math.max(0, cachedCategories.size() - 5);
            if (maxScroll > 0) {
                float fraction = Mth.clamp((float)(mouseY - trackY) / trackH, 0.0f, 1.0f);
                homeScroll = Math.round(fraction * maxScroll);
            }
        } else if (activeSpecialTab == SpecialTab.NONE && !cachedCategories.isEmpty()) {
            int trackY = bookStartY + 47;
            int trackH = 166;
            EntityTypeScanner.CategoryData cat = cachedCategories.get(selectedTabIndex);
            int maxScroll = Math.max(0, (int) Math.ceil(cat.entityIds.size() / 5.0) - 5);
            if (maxScroll > 0) {
                float fraction = Mth.clamp((float)(mouseY - trackY) / trackH, 0.0f, 1.0f);
                currentRowScroll = Math.round(fraction * maxScroll);
            }
        } else if (activeSpecialTab == SpecialTab.DETAILS) {
            int trackY = bookStartY + 135;
            int trackH = 80;
            if (maxDetailsScroll > 0) {
                float fraction = Mth.clamp((float)(mouseY - trackY) / trackH, 0.0f, 1.0f);
                detailsScroll = Math.round(fraction * maxDetailsScroll);
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

            EntityTypeScanner.CategoryData activeCat = cachedCategories.get(selectedTabIndex);
            List<String> items = activeCat.entityIds;
            int columns = 5;
            int visibleRows = 5;
            int cellW = 30;
            int cellH = 34;
            int gridStartX = bookStartX + 49;
            int gridStartY = bookStartY + 46;

            int startIndex = currentRowScroll * columns;
            int endIndex = Math.min(startIndex + (columns * visibleRows), items.size());

            for (int i = startIndex; i < endIndex; i++) {
                int index = i - startIndex;
                int slotX = gridStartX + (index % columns * cellW);
                int slotY = gridStartY + (index / columns * cellH);

                if (mouseX >= slotX && mouseX < slotX + cellW && mouseY >= slotY && mouseY < slotY + cellH) {
                    selectedEntityId = items.get(i);
                    activeSpecialTab = SpecialTab.DETAILS;
                    this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        } else if (activeSpecialTab == SpecialTab.DETAILS) {
            int trackX = bookStartX + 199;
            if (mouseX >= trackX - 2 && mouseX <= trackX + 6 && mouseY >= bookStartY + 135 && mouseY <= bookStartY + 135 + 80) {
                isScrolling = true;
                updateScrollbar(mouseY);
                return true;
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
        } else if (activeSpecialTab == SpecialTab.DETAILS) {
            if (scrollY > 0 && detailsScroll > 0) detailsScroll -= 15;
            else if (scrollY < 0 && detailsScroll < maxDetailsScroll) detailsScroll += 15;

            detailsScroll = Mth.clamp(detailsScroll, 0, maxDetailsScroll);
            return true;
        }

        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (activeSpecialTab == SpecialTab.DETAILS) {
            activeSpecialTab = SpecialTab.NONE;
            this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (com.r3ct.bestiary.platform.Services.PLATFORM.isCatalogKey(event)) {
            super.onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}