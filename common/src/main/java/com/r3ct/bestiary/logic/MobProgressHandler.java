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

    public static int getTotalUnlockedPages(PlayerData data) {
        int totalPages = 0;
        for (var entry : data.killCounts.entrySet()) {
            String entityId = entry.getKey();
            int count = entry.getValue();

            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId)).map(net.minecraft.core.Holder::value).orElse(null);
            if (type != null) {
                List<Integer> thresholds = getProgressThresholds(entityId, type.getCategory());
                for (int t : thresholds) {
                    if (count >= t) {
                        totalPages++;
                    } else {
                        break;
                    }
                }
            }
        }
        return totalPages;
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

    public static void handlePlayerDolphinSwimTick(ServerPlayer player) {
        if (player.hasEffect(net.minecraft.world.effect.MobEffects.DOLPHINS_GRACE)) {

            double dx = player.getX() - player.xOld;
            double dy = player.getY() - player.yOld;
            double dz = player.getZ() - player.zOld;
            double distanceTraveled = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distanceTraveled < 0.01) return;

            PlayerData data = ModState.getPlayerData(player.level().getServer(), player.getUUID());
            String entityId = "minecraft:dolphin";

            double currentDistance = data.rideDistances.getOrDefault(entityId, 0.0);
            currentDistance += distanceTraveled;

            double requiredDistance = BestiaryConfig.rideDistanceBlocks;

            if (currentDistance >= requiredDistance) {
                net.minecraft.world.entity.EntityType<?> dolphinType = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId)).map(net.minecraft.core.Holder::value).orElse(null);
                if (dolphinType != null) {
                    handleProgress(player, dolphinType);
                }
                currentDistance -= requiredDistance;
            }

            data.rideDistances.put(entityId, currentDistance);

            if (player.tickCount % 200 == 0) {
                ModState.get(player.level().getServer()).setDirty();
            }
        }
    }

    public static void handlePlayerFishedItems(ServerPlayer player, java.util.Collection<ItemStack> loots) {
        for (ItemStack stack : loots) {
            net.minecraft.world.entity.EntityType<?> targetEntity = null;

            if (stack.is(net.minecraft.world.item.Items.COD)) {
                targetEntity = net.minecraft.world.entity.EntityType.COD;
            } else if (stack.is(net.minecraft.world.item.Items.SALMON)) {
                targetEntity = net.minecraft.world.entity.EntityType.SALMON;
            } else if (stack.is(net.minecraft.world.item.Items.PUFFERFISH)) {
                targetEntity = net.minecraft.world.entity.EntityType.PUFFERFISH;
            } else if (stack.is(net.minecraft.world.item.Items.TROPICAL_FISH)) {
                targetEntity = net.minecraft.world.entity.EntityType.TROPICAL_FISH;
            }

            if (targetEntity != null) {
                handleProgress(player, targetEntity);
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

        int pagesBefore = getTotalUnlockedPages(data);

        int currentKills = data.killCounts.getOrDefault(entityId, 0);
        int newKills = currentKills + 1;
        data.killCounts.put(entityId, newKills);

        ModState.get(player.level().getServer()).setDirty();

        int pagesAfter = getTotalUnlockedPages(data);

        if (pagesAfter > pagesBefore) {
            List<Integer> thresholds = getProgressThresholds(entityId, category);
            int unlockedPageIndex = -1;

            for (int i = 0; i < thresholds.size(); i++) {
                if (newKills == thresholds.get(i)) {
                    unlockedPageIndex = i;
                    break;
                }
            }

            if (unlockedPageIndex != -1) {
                int pageNumber = unlockedPageIndex + 1;

                String bestiaryCat = getBestiaryCategory(entityId, category);
                List<Integer> xpThresholds;
                if (bestiaryCat.equals("bosses")) {
                    xpThresholds = BestiaryConfig.xpBosses;
                } else if (bestiaryCat.equals("monsters")) {
                    xpThresholds = BestiaryConfig.xpMonsters;
                } else {
                    xpThresholds = BestiaryConfig.xpCreatures;
                }

                int xpToGive = xpThresholds.size() > unlockedPageIndex ? xpThresholds.get(unlockedPageIndex) : 0;
                player.giveExperiencePoints(xpToGive);

                var prefix = Component.translatable("chat.r3ct_bestiary.prefix").withStyle(ChatFormatting.RED);
                var mobNameComp = entityType.getDescription().copy().withStyle(ChatFormatting.YELLOW);

                if (pageNumber == 1) {
                    player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.sendSystemMessage(Component.empty().append(prefix).append(Component.translatable("chat.r3ct_bestiary.mob_completed", mobNameComp).withStyle(ChatFormatting.GREEN)));
                } else {
                    player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                    var pageNumComp = Component.literal(String.valueOf(pageNumber)).withStyle(ChatFormatting.GOLD);
                    player.sendSystemMessage(Component.empty().append(prefix).append(Component.translatable("chat.r3ct_bestiary.page_unlocked", pageNumComp, mobNameComp).withStyle(ChatFormatting.GREEN)));
                }

                if (pagesAfter >= 1 && pagesBefore < 1) grantAdvancement(player, "r3ct_bestiary:first_item");
                if (pagesAfter >= 100 && pagesBefore < 100) grantAdvancement(player, "r3ct_bestiary:items_100");
                if (pagesAfter >= 500 && pagesBefore < 500) grantAdvancement(player, "r3ct_bestiary:items_500");
                if (pagesAfter >= 1000 && pagesBefore < 1000) grantAdvancement(player, "r3ct_bestiary:items_1000");

                int interval = BestiaryConfig.milestoneInterval;
                if (interval > 0 && (pagesBefore / interval < pagesAfter / interval)) {
                    BestiaryConfig.LootEntry reward = BestiaryConfig.getRandomMilestoneReward();
                    if (reward != null) {
                        Item rewardItem = BuiltInRegistries.ITEM.get(Identifier.parse(reward.item)).map(net.minecraft.core.Holder::value).orElse(Items.AIR);
                        if (rewardItem != Items.AIR) {
                            int amount = reward.min_amount + player.getRandom().nextInt((reward.max_amount - reward.min_amount) + 1);
                            ItemStack rewardStack = new ItemStack(rewardItem, amount);
                            var savedItemName = rewardStack.getHoverName().copy();

                            giveItemToPlayer(player, rewardStack);

                            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_TWINKLE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);

                            ChatFormatting rewardColor = ChatFormatting.AQUA;
                            if (reward.color != null && reward.color.length() >= 2 && reward.color.startsWith("&")) {
                                ChatFormatting parsedColor = ChatFormatting.getByCode(reward.color.charAt(1));
                                if (parsedColor != null) rewardColor = parsedColor;
                            }

                            var numberComp = Component.literal(String.valueOf(pagesAfter)).withStyle(ChatFormatting.YELLOW);
                            var rewardComp = Component.literal(amount + "x ").withStyle(rewardColor).append(savedItemName.withStyle(rewardColor));

                            player.sendSystemMessage(Component.empty().append(prefix).append(Component.translatable("chat.r3ct_bestiary.milestone_reward", numberComp, rewardComp).withStyle(ChatFormatting.GREEN)));
                        }
                    }
                }

                checkAndAwardCompletedCategories(player, data);
            }
        }

        Services.PLATFORM.sendSyncDataPacketToClient(player, data.killCounts, data.rewardedCategories);
        handleLeaderboardRequest(player);
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
                        if (!thresholds.isEmpty()) {
                            int maxReq = thresholds.get(thresholds.size() - 1);
                            if (count >= maxReq) {
                                gathered++;
                            }
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
                .append(Component.translatable("chat.r3ct_bestiary.trophy_name", categoryId.toUpperCase()).withStyle(ChatFormatting.LIGHT_PURPLE));

        ItemStack rewardStack = createCategoryTrophy(player, displayEntityId, allEntitiesInCat, customName);
        var savedTrophyName = rewardStack.getHoverName().copy();

        giveItemToPlayer(player, rewardStack);

        var prefix = Component.translatable("chat.r3ct_bestiary.prefix").withStyle(ChatFormatting.RED);
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

            int leaderboardScore = getTotalUnlockedPages(data);

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

    public static void debugCompleteCategory(net.minecraft.server.level.ServerPlayer player, String categoryId) {
        if (!player.isCreative()) return;

        if (com.r3ct.bestiary.scanner.EntityTypeScanner.SCANNED_CATEGORIES.isEmpty()) {
            com.r3ct.bestiary.scanner.EntityTypeScanner.scanEntities();
        }

        PlayerData data = ModState.getPlayerData(player.level().getServer(), player.getUUID());
        com.r3ct.bestiary.scanner.EntityTypeScanner.CategoryData catData = com.r3ct.bestiary.scanner.EntityTypeScanner.SCANNED_CATEGORIES.get(categoryId);

        if (catData != null) {
            for (String entityId : catData.entityIds) {
                net.minecraft.world.entity.EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(net.minecraft.resources.Identifier.parse(entityId)).map(net.minecraft.core.Holder::value).orElse(null);
                if (type != null) {
                    java.util.List<Integer> thresholds = getProgressThresholds(entityId, type.getCategory());
                    if (!thresholds.isEmpty()) {
                        int maxReq = thresholds.get(thresholds.size() - 1);
                        data.killCounts.put(entityId, maxReq);
                    }
                }
            }

            handleCategoryReward(player, categoryId);

            ModState.get(player.level().getServer()).setDirty();
            com.r3ct.bestiary.platform.Services.PLATFORM.sendSyncDataPacketToClient(player, data.killCounts, data.rewardedCategories);

            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("chat.r3ct_bestiary.dev_category_complete", categoryId).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}