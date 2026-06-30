package com.r3ct.bestiary.logic;

import com.r3ct.bestiary.config.BestiaryConfig;
import com.r3ct.bestiary.data.ModState;
import com.r3ct.bestiary.data.PlayerData;
import com.r3ct.bestiary.network.LeaderboardDataPayload;
import com.r3ct.bestiary.platform.Services;
import com.r3ct.bestiary.scanner.EntityTypeScanner;
import com.r3ct.bestiary.block.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

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
        if (BestiaryConfig.customProgressRequirements.containsKey(entityId)) {
            return BestiaryConfig.customProgressRequirements.get(entityId);
        }

        String bestiaryCat = getBestiaryCategory(entityId, category);

        if (bestiaryCat.equals("bosses")) {
            return BestiaryConfig.defaultProgressBosses;
        } else if (bestiaryCat.equals("creatures")) {
            return BestiaryConfig.defaultProgressCreatures;
        }
        return BestiaryConfig.defaultProgressMonsters;
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

    public static void handleMobKill(ServerPlayer player, EntityType<?> entityType) { handleProgress(player, entityType); }
    public static void handleMobBreed(ServerPlayer player, EntityType<?> entityType) { handleProgress(player, entityType); }
    public static void handleMobTame(ServerPlayer player, EntityType<?> entityType) { handleProgress(player, entityType); }
    public static void handleMobTrade(ServerPlayer player, EntityType<?> entityType) { handleProgress(player, entityType); }
    public static void handleMobBuild(ServerPlayer player, EntityType<?> entityType) { handleProgress(player, entityType); }
    public static void handleMobCure(ServerPlayer player, EntityType<?> entityType) { handleProgress(player, entityType); }
    public static void handleMobShear(ServerPlayer player, EntityType<?> entityType) { handleProgress(player, entityType); }
    public static void handleMobInteract(ServerPlayer player, EntityType<?> entityType) { handleProgress(player, entityType); }

    public static void handlePlayerRidingTick(ServerPlayer player) {
        if (player.getVehicle() instanceof net.minecraft.world.entity.LivingEntity mount) {
            double dx = player.getX() - player.xOld;
            double dy = player.getY() - player.yOld;
            double dz = player.getZ() - player.zOld;
            double distanceTraveled = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distanceTraveled < 0.01) return;

            PlayerData data = ModState.getPlayerData(player.level().getServer(), player.getUUID());
            String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(mount.getType()).toString();

            double currentDistance = data.rideDistances.getOrDefault(entityId, 0.0);
            currentDistance += distanceTraveled;

            double requiredDistance = BestiaryConfig.rideDistanceBlocks;
            if (currentDistance >= requiredDistance) {
                handleProgress(player, mount.getType());
                currentDistance -= requiredDistance;
            }

            data.rideDistances.put(entityId, currentDistance);

            if (player.tickCount % 200 == 0) {
                ModState.get(player.level().getServer()).setDirty();
            }
        }
    }

    private static void handleProgress(ServerPlayer player, EntityType<?> entityType) {
        MobCategory category = entityType.getCategory();
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString();

        boolean isAllowedMisc = (entityType == EntityType.VILLAGER ||
                entityType == EntityType.IRON_GOLEM ||
                entityType == EntityType.SNOW_GOLEM ||
                entityType == EntityType.COPPER_GOLEM);

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

        List<Integer> xpThresholds;
        if (bestiaryCat.equals("bosses")) {
            xpThresholds = BestiaryConfig.xpBosses;
        } else if (bestiaryCat.equals("monsters")) {
            xpThresholds = BestiaryConfig.xpMonsters;
        } else {
            xpThresholds = BestiaryConfig.xpCreatures;
        }

        if (newKills == baseReq) {
            BestiaryConfig.load();

            int completedBefore = getCompletedMobsCount(data) - 1;
            int completedAfter = completedBefore + 1;

            int xpToGive = xpThresholds.size() > 0 ? xpThresholds.get(0) : 0;
            player.giveExperiencePoints(xpToGive);

            player.level().playSound(null, player.blockPosition(),
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);

            var prefix = Component.literal("[Bestiary] ").withStyle(ChatFormatting.RED);
            var mobNameComp = entityType.getDescription().copy().withStyle(ChatFormatting.YELLOW);

            player.sendSystemMessage(
                    Component.empty()
                            .append(prefix)
                            .append(Component.translatable("chat.r3ct_bestiary.mob_completed", mobNameComp).withStyle(ChatFormatting.GREEN))
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

                        ChatFormatting rewardColor = ChatFormatting.AQUA;
                        if (reward.color != null && reward.color.length() >= 2 && reward.color.startsWith("&")) {
                            ChatFormatting parsedColor = ChatFormatting.getByCode(reward.color.charAt(1));
                            if (parsedColor != null) rewardColor = parsedColor;
                        }

                        var rewardPrefix = Component.literal("[Bestiary] ").withStyle(ChatFormatting.RED);
                        var numberComp = Component.literal(String.valueOf(completedAfter)).withStyle(ChatFormatting.YELLOW);
                        var rewardComp = Component.literal(amount + "x ")
                                .withStyle(rewardColor)
                                .append(savedItemName.withStyle(rewardColor));

                        player.sendSystemMessage(
                                Component.empty()
                                        .append(rewardPrefix)
                                        .append(Component.translatable("chat.r3ct_bestiary.milestone_reward", numberComp, rewardComp).withStyle(ChatFormatting.GREEN))
                        );
                    }
                }
            }
            checkAndAwardCompletedCategories(player, data);
        }
        else if (newKills == star1Req) {
            int xpToGive = xpThresholds.size() > 1 ? xpThresholds.get(1) : 0;
            handlePageUnlock(player, entityType, 2, xpToGive);
        } else if (newKills == star2Req) {
            int xpToGive = xpThresholds.size() > 2 ? xpThresholds.get(2) : 0;
            handlePageUnlock(player, entityType, 3, xpToGive);
        } else if (newKills == star3Req) {
            int xpToGive = xpThresholds.size() > 3 ? xpThresholds.get(3) : 0;
            handlePageUnlock(player, entityType, 4, xpToGive);
        }

        ModState.get(player.level().getServer()).setDirty();
        Services.PLATFORM.sendSyncDataPacketToClient(player, data.killCounts, data.rewardedCategories);
        handleLeaderboardRequest(player);
    }

    private static void handlePageUnlock(ServerPlayer player, EntityType<?> entityType, int pageNumber, int xpReward) {
        player.giveExperiencePoints(xpReward);

        player.level().playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);

        var prefix = Component.literal("[Bestiary] ").withStyle(ChatFormatting.RED);
        var mobNameComp = entityType.getDescription().copy().withStyle(ChatFormatting.YELLOW);
        var pageNumComp = Component.literal(String.valueOf(pageNumber)).withStyle(ChatFormatting.GOLD);

        player.sendSystemMessage(
                Component.empty()
                        .append(prefix)
                        .append(Component.translatable("chat.r3ct_bestiary.page_unlocked", pageNumComp, mobNameComp).withStyle(ChatFormatting.GREEN))
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

    public static ItemStack createCategoryTrophy(ServerPlayer player, String displayEntityRegistryName, List<String> allCategoryEntities, Component customTitle) {
        ItemStack trophyStack = new ItemStack(ModBlocks.TROPHY);
        trophyStack.set(DataComponents.CUSTOM_NAME, customTitle);

        CompoundTag tag = new CompoundTag();
        tag.putString("DisplayEntity", displayEntityRegistryName);
        tag.putString("OwnerName", player.getScoreboardName());

        net.minecraft.nbt.ListTag entityListTag = new net.minecraft.nbt.ListTag();
        if (allCategoryEntities != null) {
            for (String entityId : allCategoryEntities) {
                entityListTag.add(net.minecraft.nbt.StringTag.valueOf(entityId));
            }
        }
        tag.put("EntityList", entityListTag);

        trophyStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return trophyStack;
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

        String displayEntityId = "minecraft:pig";
        List<String> allEntitiesInCat = new ArrayList<>();
        com.r3ct.bestiary.scanner.EntityTypeScanner.CategoryData catData = com.r3ct.bestiary.scanner.EntityTypeScanner.SCANNED_CATEGORIES.get(categoryId);
        if (catData != null && !catData.entityIds.isEmpty()) {
            displayEntityId = catData.entityIds.get(0);
            allEntitiesInCat = catData.entityIds;
        }

        net.minecraft.network.chat.MutableComponent customName = Component.literal(player.getName().getString())
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" - ").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal("Trofeum: " + categoryId.toUpperCase()).withStyle(ChatFormatting.LIGHT_PURPLE));

        ItemStack rewardStack = createCategoryTrophy(player, displayEntityId, allEntitiesInCat, customName);
        var savedTrophyName = rewardStack.getHoverName().copy();

        giveItemToPlayer(player, rewardStack);

        var prefix = Component.literal("[Bestiary] ").withStyle(ChatFormatting.RED);
        var catNameComp = Component.literal(categoryId.toUpperCase()).withStyle(ChatFormatting.YELLOW);
        var trophyComp = savedTrophyName.withStyle(ChatFormatting.LIGHT_PURPLE);

        player.sendSystemMessage(
                Component.empty()
                        .append(prefix)
                        .append(Component.translatable("chat.r3ct_bestiary.category_complete", catNameComp, trophyComp).withStyle(ChatFormatting.GREEN))
        );

        data.rewardedCategories.add(categoryId);

        int catSize = data.rewardedCategories.size();
        if (data.rewardedCategories.contains("ALL_COMPLETED")) catSize--;

        if (catSize >= 1) grantAdvancement(player, "r3ct_bestiary:category_1");
        if (catSize >= 5) grantAdvancement(player, "r3ct_bestiary:category_5");

        ModState.get(player.level().getServer()).setDirty();
        Services.PLATFORM.sendSyncDataPacketToClient(player, data.killCounts, data.rewardedCategories);
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

    public static boolean tryApplyCooldown(net.minecraft.world.entity.Mob mob, long cooldownMs) {
        if (mob instanceof com.r3ct.bestiary.util.IResearchCooldown cooldownEntity) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - cooldownEntity.r3ct_getLastResearchTime() >= cooldownMs) {
                cooldownEntity.r3ct_setLastResearchTime(currentTime);
                return true;
            }
        }
        return false;
    }

    public static void debugCompleteCategory(ServerPlayer player, String categoryId) {
        if (!player.isCreative()) return;

        PlayerData data = ModState.getPlayerData(player.level().getServer(), player.getUUID());
        com.r3ct.bestiary.scanner.EntityTypeScanner.CategoryData catData = com.r3ct.bestiary.scanner.EntityTypeScanner.SCANNED_CATEGORIES.get(categoryId);

        if (catData != null) {
            for (String entityId : catData.entityIds) {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId)).map(net.minecraft.core.Holder::value).orElse(null);
                if (type != null) {
                    List<Integer> thresholds = getProgressThresholds(entityId, type.getCategory());
                    if (!thresholds.isEmpty()) {
                        int maxReq = thresholds.get(thresholds.size() - 1);
                        data.killCounts.put(entityId, maxReq);
                    }
                }
            }

            handleCategoryReward(player, categoryId);

            ModState.get(player.level().getServer()).setDirty();
            Services.PLATFORM.sendSyncDataPacketToClient(player, data.killCounts, data.rewardedCategories);

            player.sendSystemMessage(Component.literal("§d[DEV] Kategoria " + categoryId + " została wymaksowana!"));
        }
    }
}