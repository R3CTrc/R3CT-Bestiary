package com.r3ct.bestiary.logic;

import com.r3ct.bestiary.config.BestiaryConfig;
import com.r3ct.bestiary.data.ModState;
import com.r3ct.bestiary.data.PlayerData;
import com.r3ct.bestiary.network.LeaderboardDataPayload;
import com.r3ct.bestiary.platform.Services;
import com.r3ct.bestiary.scanner.EntityTypeScanner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class MobProgressHandler {

    private static void grantAdvancement(ServerPlayer player, String advancementId) {
        net.minecraft.server.MinecraftServer server = player.level().getServer();
        Identifier id = Identifier.parse(advancementId);
        net.minecraft.advancements.AdvancementHolder advancement = server.getAdvancements().get(id);

        if (advancement != null) {
            player.getAdvancements().award(advancement, "unlocked");
        }
    }

    public static String getBestiaryCategory(String entityId, MobCategory category) {
        String namespace = entityId.split(":")[0];
        String mobOverride = BestiaryConfig.mobCategoryOverrides.get(entityId);
        String modOverride = BestiaryConfig.modCategoryOverrides.get(namespace);

        if (mobOverride != null && !mobOverride.isEmpty()) {
            return mobOverride;
        }
        if (modOverride != null && !modOverride.isEmpty()) {
            return modOverride;
        }
        if (category == MobCategory.MONSTER) {
            return "monsters";
        }
        return "creatures";
    }

    public static List<Integer> getProgressThresholds(String entityId, MobCategory category) {
        if (com.r3ct.bestiary.config.BestiaryConfig.customProgressRequirements.containsKey(entityId)) {
            return com.r3ct.bestiary.config.BestiaryConfig.customProgressRequirements.get(entityId);
        }

        String bestiaryCat = getBestiaryCategory(entityId, category);

        if (bestiaryCat.equals("bosses")) {
            return com.r3ct.bestiary.config.BestiaryConfig.defaultProgressBosses;
        } else if (bestiaryCat.equals("creatures")) {
            return com.r3ct.bestiary.config.BestiaryConfig.defaultProgressCreatures;
        }
        return com.r3ct.bestiary.config.BestiaryConfig.defaultProgressMonsters;
    }

    public static int getCompletedMobsCount(PlayerData data) {
        int completedCount = 0;
        for (var entry : data.killCounts.entrySet()) {
            String entityId = entry.getKey();
            int count = entry.getValue();

            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId)).map(net.minecraft.core.Holder::value).orElse(null);
            if (type != null) {
                List<Integer> thresholds = getProgressThresholds(entityId, type.getCategory());
                if (!thresholds.isEmpty() && count >= thresholds.get(0)) {
                    completedCount++;
                }
            }
        }
        return completedCount;
    }

    public static int getTotalValidKills(PlayerData data) {
        int totalValidKills = 0;
        for (var entry : data.killCounts.entrySet()) {
            String entityId = entry.getKey();
            int count = entry.getValue();

            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId)).map(net.minecraft.core.Holder::value).orElse(null);
            if (type != null) {
                List<Integer> thresholds = getProgressThresholds(entityId, type.getCategory());
                if (!thresholds.isEmpty()) {
                    int maxRequired = thresholds.get(thresholds.size() - 1);
                    totalValidKills += Math.min(count, maxRequired);
                }
            }
        }
        return totalValidKills;
    }

    // --- DOSTĘPNE AKCJE (Wszystkie robią dokładnie to samo - dodają punkt postępu!) ---
    public static void handleMobKill(ServerPlayer player, EntityType<?> entityType) { handleProgress(player, entityType); }
    public static void handleMobBreed(ServerPlayer player, EntityType<?> entityType) { handleProgress(player, entityType); }
    public static void handleMobTame(ServerPlayer player, EntityType<?> entityType) { handleProgress(player, entityType); }
    public static void handleMobTrade(ServerPlayer player, EntityType<?> entityType) { handleProgress(player, entityType); }
    public static void handleMobBuild(ServerPlayer player, EntityType<?> entityType) { handleProgress(player, entityType); }
    // ----------------------------------------------------------------------------------

    // Główny silnik dodawania punktów
    private static void handleProgress(ServerPlayer player, EntityType<?> entityType) {
        MobCategory category = entityType.getCategory();
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString();

        boolean isAllowedMisc = (entityType == EntityType.VILLAGER ||
                entityType == EntityType.IRON_GOLEM ||
                entityType == EntityType.SNOW_GOLEM ||
                entityType == EntityType.COPPER_GOLEM);

        // Jeśli to MISC, ale NIE JEST dozwolonym mobem i NIE JEST wpisane w configu - ignorujemy (np. strzały, łódki)
        if (category == MobCategory.MISC && !isAllowedMisc && !BestiaryConfig.mobCategoryOverrides.containsKey(entityId)) {
            return;
        }

        PlayerData data = ModState.getPlayerData(player.level().getServer(), player.getUUID());
        data.lastKnownName = player.getName().getString();

        int currentKills = data.killCounts.getOrDefault(entityId, 0);
        int newKills = currentKills + 1;
        data.killCounts.put(entityId, newKills);

        ModState.get(player.level().getServer()).setDirty();

        List<Integer> thresholds = getProgressThresholds(entityId, category);
        if (thresholds.isEmpty()) return;

        int baseReq = thresholds.size() > 0 ? thresholds.get(0) : -1;
        int star1Req = thresholds.size() > 1 ? thresholds.get(1) : -1;
        int star2Req = thresholds.size() > 2 ? thresholds.get(2) : -1;
        int star3Req = thresholds.size() > 3 ? thresholds.get(3) : -1;

        String bestiaryCat = getBestiaryCategory(entityId, category);

        // POBIERAMY ODPOWIEDNIĄ TABLICĘ XP DLA TEJ KATEGORII
        List<Integer> xpThresholds;
        if (bestiaryCat.equals("bosses")) {
            xpThresholds = BestiaryConfig.xpBosses;
        } else if (bestiaryCat.equals("monsters")) {
            xpThresholds = BestiaryConfig.xpMonsters;
        } else {
            xpThresholds = BestiaryConfig.xpCreatures;
        }

        // BAZOWE UKOŃCZENIE
        if (newKills == baseReq) {
            BestiaryConfig.load();

            int completedBefore = getCompletedMobsCount(data) - 1;
            int completedAfter = completedBefore + 1;

            // Pobieramy pierwszy próg XP (indeks 0)
            int xpToGive = xpThresholds.size() > 0 ? xpThresholds.get(0) : 0;
            player.giveExperiencePoints(xpToGive);

            player.level().playSound(null, player.blockPosition(),
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);

            var prefix = net.minecraft.network.chat.Component.literal("[Bestiary] ").withStyle(net.minecraft.ChatFormatting.AQUA);
            var mobNameComp = entityType.getDescription().copy().withStyle(net.minecraft.ChatFormatting.YELLOW);

            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.empty()
                            .append(prefix)
                            .append(net.minecraft.network.chat.Component.translatable("chat.r3ct_bestiary.mob_completed", mobNameComp).withStyle(net.minecraft.ChatFormatting.GREEN))
            );

            if (completedAfter >= 1) grantAdvancement(player, "r3ct_bestiary:first_kill");
            if (completedAfter >= 100) grantAdvancement(player, "r3ct_bestiary:mobs_100");
            if (completedAfter >= 500) grantAdvancement(player, "r3ct_bestiary:mobs_500");
            if (completedAfter >= 1000) grantAdvancement(player, "r3ct_bestiary:mobs_1000");

            int interval = BestiaryConfig.milestoneInterval;
            if (interval > 0 && (completedBefore / interval < completedAfter / interval)) {
                BestiaryConfig.LootEntry reward = BestiaryConfig.getRandomMilestoneReward();
                if (reward != null) {
                    Item rewardItem = BuiltInRegistries.ITEM.get(Identifier.parse(reward.item)).map(net.minecraft.core.Holder::value).orElse(Items.AIR);
                    if (rewardItem != Items.AIR) {
                        int amount = reward.min_amount + player.getRandom().nextInt((reward.max_amount - reward.min_amount) + 1);
                        ItemStack rewardStack = new ItemStack(rewardItem, amount);
                        var savedItemName = rewardStack.getHoverName().copy();

                        giveItemToPlayer(player, rewardStack);

                        player.level().playSound(null, player.blockPosition(),
                                net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_TWINKLE,
                                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);

                        net.minecraft.ChatFormatting rewardColor = ChatFormatting.AQUA;
                        if (reward.color != null && reward.color.length() >= 2 && reward.color.startsWith("&")) {
                            net.minecraft.ChatFormatting parsedColor = net.minecraft.ChatFormatting.getByCode(reward.color.charAt(1));
                            if (parsedColor != null) {
                                rewardColor = parsedColor;
                            }
                        }

                        var rewardPrefix = net.minecraft.network.chat.Component.literal("[Bestiary] ").withStyle(net.minecraft.ChatFormatting.RED);
                        var numberComp = net.minecraft.network.chat.Component.literal(String.valueOf(completedAfter)).withStyle(net.minecraft.ChatFormatting.YELLOW);
                        var rewardComp = net.minecraft.network.chat.Component.literal(amount + "x ")
                                .withStyle(rewardColor)
                                .append(savedItemName.withStyle(rewardColor));

                        player.sendSystemMessage(
                                net.minecraft.network.chat.Component.empty()
                                        .append(rewardPrefix)
                                        .append(net.minecraft.network.chat.Component.translatable("chat.r3ct_bestiary.milestone_reward", numberComp, rewardComp).withStyle(net.minecraft.ChatFormatting.GREEN))
                        );
                    }
                }
            }
            checkAndAwardCompletedCategories(player, data);
        }
        // GWIAZDKI MISTRZOSTWA (Indeksy 1, 2, 3)
        else if (newKills == star1Req) {
            int xpToGive = xpThresholds.size() > 1 ? xpThresholds.get(1) : 0;
            handleStarUnlock(player, entityType, 1, xpToGive);
        } else if (newKills == star2Req) {
            int xpToGive = xpThresholds.size() > 2 ? xpThresholds.get(2) : 0;
            handleStarUnlock(player, entityType, 2, xpToGive);
        } else if (newKills == star3Req) {
            int xpToGive = xpThresholds.size() > 3 ? xpThresholds.get(3) : 0;
            handleStarUnlock(player, entityType, 3, xpToGive);
        }

        ModState.get(player.level().getServer()).setDirty();
        Services.PLATFORM.sendSyncDataPacketToClient(player, data.killCounts, data.rewardedCategories);
        handleLeaderboardRequest(player);
    }

    private static void handleStarUnlock(ServerPlayer player, EntityType<?> entityType, int starLevel, int xpReward) {
        player.giveExperiencePoints(xpReward);

        player.level().playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);

        var prefix = net.minecraft.network.chat.Component.literal("[Bestiary] ").withStyle(net.minecraft.ChatFormatting.GOLD);
        var mobNameComp = entityType.getDescription().copy().withStyle(net.minecraft.ChatFormatting.YELLOW);

        String stars = "★".repeat(starLevel);
        var starComp = net.minecraft.network.chat.Component.literal(" " + stars).withStyle(net.minecraft.ChatFormatting.GOLD);

        player.sendSystemMessage(
                net.minecraft.network.chat.Component.empty()
                        .append(prefix)
                        .append(net.minecraft.network.chat.Component.translatable("chat.r3ct_bestiary.star_unlocked", mobNameComp, starComp).withStyle(net.minecraft.ChatFormatting.YELLOW))
        );
    }

    private static void checkAndAwardCompletedCategories(ServerPlayer player, PlayerData data) {
        if (EntityTypeScanner.SCANNED_CATEGORIES.isEmpty()) {
            EntityTypeScanner.scanEntities();
        }

        int completedRealCategories = 0;

        for (EntityTypeScanner.CategoryData cat : EntityTypeScanner.SCANNED_CATEGORIES.values()) {
            if (!data.rewardedCategories.contains(cat.categoryId)) {

                int gathered = 0;
                for (String entityId : cat.entityIds) {
                    int count = data.killCounts.getOrDefault(entityId, 0);

                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId)).map(net.minecraft.core.Holder::value).orElse(null);
                    if (type != null) {
                        List<Integer> thresholds = getProgressThresholds(entityId, type.getCategory());
                        if (!thresholds.isEmpty() && count >= thresholds.get(0)) {
                            gathered++;
                        }
                    }
                }

                if (gathered > 0 && gathered == cat.entityIds.size()) {
                    handleCategoryReward(player, cat.categoryId);
                    player.level().playSound(null, player.blockPosition(),
                            net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_TWINKLE,
                            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }

            if (data.rewardedCategories.contains(cat.categoryId)) {
                completedRealCategories++;
            }
        }

        if (completedRealCategories >= EntityTypeScanner.SCANNED_CATEGORIES.size() && !EntityTypeScanner.SCANNED_CATEGORIES.isEmpty()) {
            if (!data.rewardedCategories.contains("ALL_COMPLETED")) {
                handleCategoryReward(player, "ALL_COMPLETED");
            }
        }
    }

    public static void handleCategoryReward(ServerPlayer player, String categoryId) {
        PlayerData data = ModState.getPlayerData(player.level().getServer(), player.getUUID());
        data.lastKnownName = player.getName().getString();

        if (data.rewardedCategories.contains(categoryId)) return;

        if (categoryId.equals("ALL_COMPLETED")) {
            grantAdvancement(player, "r3ct_bestiary:all_completed");
            data.rewardedCategories.add("ALL_COMPLETED");
            ModState.get(player.level().getServer()).setDirty();
            return;
        }

        BestiaryConfig.load();
        String rewardItemId = BestiaryConfig.categoryRewards.getOrDefault(categoryId, BestiaryConfig.categoryRewards.get("modded_generic"));

        if (rewardItemId != null) {
            Item rewardItem = BuiltInRegistries.ITEM.get(Identifier.parse(rewardItemId)).map(net.minecraft.core.Holder::value).orElse(Items.AIR);
            if (rewardItem != Items.AIR) {
                ItemStack rewardStack = new ItemStack(rewardItem, 1);

                net.minecraft.network.chat.MutableComponent customName = net.minecraft.network.chat.Component.literal(player.getName().getString())
                        .withStyle(net.minecraft.ChatFormatting.AQUA)
                        .append(net.minecraft.network.chat.Component.literal(" - ").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE))
                        .append(net.minecraft.network.chat.Component.translatable(rewardItem.getDescriptionId()).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));

                rewardStack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, customName);
                var savedTrophyName = rewardStack.getHoverName().copy();

                giveItemToPlayer(player, rewardStack);

                var prefix = net.minecraft.network.chat.Component.literal("[Bestiary] ").withStyle(net.minecraft.ChatFormatting.RED);
                var catNameComp = net.minecraft.network.chat.Component.literal(categoryId.toUpperCase()).withStyle(net.minecraft.ChatFormatting.YELLOW);
                var trophyComp = savedTrophyName.withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);

                player.sendSystemMessage(
                        net.minecraft.network.chat.Component.empty()
                                .append(prefix)
                                .append(net.minecraft.network.chat.Component.translatable("chat.r3ct_bestiary.category_complete", catNameComp, trophyComp).withStyle(net.minecraft.ChatFormatting.GREEN))
                );

                data.rewardedCategories.add(categoryId);

                int catSize = data.rewardedCategories.size();
                if (data.rewardedCategories.contains("ALL_COMPLETED")) catSize--;

                if (catSize >= 1) grantAdvancement(player, "r3ct_bestiary:category_1");
                if (catSize >= 5) grantAdvancement(player, "r3ct_bestiary:category_5");

                ModState.get(player.level().getServer()).setDirty();
                Services.PLATFORM.sendSyncDataPacketToClient(player, data.killCounts, data.rewardedCategories);
            }
        }
    }

    public static void handleLeaderboardRequest(ServerPlayer player) {
        grantAdvancement(player, "r3ct_bestiary:root");

        net.minecraft.server.MinecraftServer server = player.level().getServer();
        ModState state = ModState.get(server);

        List<LeaderboardDataPayload.TopPlayerEntry> allEntries = new ArrayList<>();

        state.players.forEach((uuid, data) -> {
            String name = data.lastKnownName;
            int leaderboardScore = getTotalValidKills(data);

            if (!name.equals("Unknown") && leaderboardScore > 0) {
                allEntries.add(new LeaderboardDataPayload.TopPlayerEntry(name, leaderboardScore, new ArrayList<>(data.killCounts.keySet())));
            }
        });

        allEntries.sort((e1, e2) -> Integer.compare(e2.totalCompleted(), e1.totalCompleted()));

        List<LeaderboardDataPayload.TopPlayerEntry> top10 = allEntries.stream().limit(10).toList();
        Services.PLATFORM.sendLeaderboardDataPacketToClient(player, top10);
    }

    private static void giveItemToPlayer(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            ItemEntity drop = player.drop(stack, false);
            if (drop != null) drop.setNoPickUpDelay();
        }
    }
}