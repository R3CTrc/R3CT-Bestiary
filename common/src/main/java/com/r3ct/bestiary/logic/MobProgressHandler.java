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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MobProgressHandler {

    public static final Map<String, List<String>> REQUIRED_ACTIONS_CACHE = new HashMap<>();

    private static void grantAdvancement(ServerPlayer player, String advancementId) {
        net.minecraft.server.MinecraftServer server = player.level().getServer();
        Identifier id = Identifier.parse(advancementId);
        net.minecraft.advancements.AdvancementHolder advancement = server.getAdvancements().get(id);

        if (advancement != null) {
            player.getAdvancements().award(advancement, "unlocked");
        }
    }

    public static String getBestiaryCategory(String entityId, EntityType<?> type) {
        String namespace = entityId.split(":")[0];
        String mobOverride = BestiaryConfig.mobCategoryOverrides.get(entityId);
        String modOverride = BestiaryConfig.modCategoryOverrides.get(namespace);

        if (mobOverride != null && !mobOverride.isEmpty()) return mobOverride;
        if (modOverride != null && !modOverride.isEmpty()) return modOverride;
        if (type != null && type.builtInRegistryHolder().is(ModTags.C_BOSSES)) return "bosses";
        if (type != null && type.getCategory() == MobCategory.MONSTER) return "monsters";

        return "creatures";
    }

    public static List<String> getRequiredActions(String entityId, EntityType<?> type, net.minecraft.world.level.Level level) {
        if (REQUIRED_ACTIONS_CACHE.containsKey(entityId)) return REQUIRED_ACTIONS_CACHE.get(entityId);

        Entity dummy = null;
        if (level != null && type != null) {
            try {
                dummy = type.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            } catch (Exception ignored) {}
        }

        List<String> actions = computeAndCacheActions(entityId, type, dummy);

        if (dummy != null) dummy.discard();
        return actions;
    }

    public static List<String> getRequiredActions(Entity dummy) {
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(dummy.getType()).toString();
        if (REQUIRED_ACTIONS_CACHE.containsKey(entityId)) return REQUIRED_ACTIONS_CACHE.get(entityId);

        return computeAndCacheActions(entityId, dummy.getType(), dummy);
    }

    private static List<String> computeAndCacheActions(String entityId, EntityType<?> type, Entity dummy) {
        List<String> actions = new ArrayList<>();

        if (BestiaryConfig.customActionOverrides.containsKey(entityId)) {
            actions.addAll(BestiaryConfig.customActionOverrides.get(entityId));
        }

        String cat = getBestiaryCategory(entityId, type);
        if (cat.equals("creatures")) {
            if (!actions.contains("interact")) actions.add("interact");
        } else {
            if (!actions.contains("kill")) actions.add("kill");
        }

        if (dummy != null) {
            if (dummy instanceof net.minecraft.world.entity.animal.Animal && !actions.contains("breed")) actions.add("breed");
            if ((dummy instanceof net.minecraft.world.entity.TamableAnimal || dummy instanceof AbstractHorse) && !actions.contains("tame")) actions.add("tame");
            if (dummy instanceof net.minecraft.world.entity.Shearable && !actions.contains("shear")) actions.add("shear");
            if (dummy instanceof net.minecraft.world.entity.animal.Bucketable && !actions.contains("bucket")) actions.add("bucket");
            if (dummy instanceof net.minecraft.world.item.trading.Merchant && !actions.contains("trade")) actions.add("trade");
            if ((dummy instanceof net.minecraft.world.entity.ItemSteerable || dummy instanceof AbstractHorse) && !actions.contains("ride")) actions.add("ride");
        }

        REQUIRED_ACTIONS_CACHE.put(entityId, actions);
        return actions;
    }

    public static int getTotalUnlockedActions(PlayerData data) {
        int total = 0;
        for (Set<String> actions : data.unlockedActions.values()) {
            total += actions.size();
        }
        return total;
    }

    public static int getTotalCompletedMobs(PlayerData data, net.minecraft.world.level.Level level) {
        int completed = 0;
        for (var entry : data.unlockedActions.entrySet()) {
            String entityId = entry.getKey();
            Set<String> unlocked = entry.getValue();

            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId)).map(net.minecraft.core.Holder::value).orElse(null);
            if (type != null) {
                List<String> required = getRequiredActions(entityId, type, level);
                if (!required.isEmpty() && unlocked.size() >= required.size()) {
                    completed++;
                }
            }
        }
        return completed;
    }

    public static void handleMobKill(ServerPlayer player, EntityType<?> type) { tryUnlockAction(player, type, "kill"); }
    public static void handleMobBreed(ServerPlayer player, EntityType<?> type) { tryUnlockAction(player, type, "breed"); }
    public static void handleMobTame(ServerPlayer player, EntityType<?> type) { tryUnlockAction(player, type, "tame"); }
    public static void handleMobTrade(ServerPlayer player, EntityType<?> type) { tryUnlockAction(player, type, "trade"); }
    public static void handleMobShear(ServerPlayer player, EntityType<?> type) { tryUnlockAction(player, type, "shear"); }
    public static void handleMobInteract(ServerPlayer player, EntityType<?> type) { tryUnlockAction(player, type, "interact"); }

    public static void handlePlayerRidingTick(ServerPlayer player) {
        if (player.getVehicle() instanceof net.minecraft.world.entity.LivingEntity mount) {
            tryUnlockAction(player, mount.getType(), "ride");
        }
    }

    public static void tryUnlockAction(ServerPlayer player, EntityType<?> type, String actionId) {
        if (type == null) return;
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        MobCategory category = type.getCategory();

        boolean isAllowedMisc = (type == EntityType.VILLAGER || type == EntityType.IRON_GOLEM || type == EntityType.SNOW_GOLEM || type == EntityType.COPPER_GOLEM);
        if (category == MobCategory.MISC && !isAllowedMisc && !BestiaryConfig.mobCategoryOverrides.containsKey(entityId)) return;

        List<String> requiredActions = getRequiredActions(entityId, type, player.level());

        if (!requiredActions.contains(actionId)) return;

        PlayerData data = ModState.getPlayerData(player.level().getServer(), player.getUUID());
        data.lastKnownName = player.getName().getString();

        Set<String> unlocked = data.unlockedActions.computeIfAbsent(entityId, k -> new java.util.HashSet<>());

        if (unlocked.contains(actionId)) return;

        int actionsBefore = getTotalUnlockedActions(data);
        boolean wasCompletedBefore = unlocked.size() >= requiredActions.size();

        unlocked.add(actionId);
        ModState.get(player.level().getServer()).setDirty();

        int actionsAfter = getTotalUnlockedActions(data);
        boolean isCompletedNow = unlocked.size() >= requiredActions.size();

        player.giveExperiencePoints(BestiaryConfig.xpPerAction);

        var prefix = Component.translatable("chat.r3ct_bestiary.prefix").withStyle(ChatFormatting.RED);
        var mobNameComp = type.getDescription().copy().withStyle(ChatFormatting.YELLOW);

        if (isCompletedNow && !wasCompletedBefore) {
            String bestiaryCat = getBestiaryCategory(entityId, type);
            int xpToGive = bestiaryCat.equals("bosses") ? BestiaryConfig.xpCompletionBoss :
                    (bestiaryCat.equals("monsters") ? BestiaryConfig.xpCompletionMonster : BestiaryConfig.xpCompletionCreature);

            player.giveExperiencePoints(xpToGive);
            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            player.sendSystemMessage(Component.empty().append(prefix).append(Component.translatable("chat.r3ct_bestiary.mob_completed", mobNameComp).withStyle(ChatFormatting.GREEN)));
            grantAdvancement(player, "r3ct_bestiary:first_maxed");
        } else {
            var actionComp = Component.translatable("action.r3ct_bestiary." + actionId).withStyle(ChatFormatting.GOLD);
            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            player.sendSystemMessage(Component.empty().append(prefix).append(Component.translatable("chat.r3ct_bestiary.action_unlocked", actionComp, mobNameComp).withStyle(ChatFormatting.GREEN)));
        }

        if (actionsAfter >= 1 && actionsBefore < 1) grantAdvancement(player, "r3ct_bestiary:first_page");
        if (actionsAfter >= 50 && actionsBefore < 50) grantAdvancement(player, "r3ct_bestiary:pages_50");
        if (actionsAfter >= 100 && actionsBefore < 100) grantAdvancement(player, "r3ct_bestiary:pages_100");
        if (actionsAfter >= 200 && actionsBefore < 200) grantAdvancement(player, "r3ct_bestiary:pages_200");

        int interval = BestiaryConfig.milestoneInterval;
        if (interval > 0 && (actionsBefore / interval < actionsAfter / interval)) {
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

                    var numberComp = Component.literal(String.valueOf(actionsAfter)).withStyle(ChatFormatting.YELLOW);
                    var rewardComp = Component.literal(amount + "x ").withStyle(rewardColor).append(savedItemName.withStyle(rewardColor));

                    player.sendSystemMessage(Component.empty().append(prefix).append(Component.translatable("chat.r3ct_bestiary.milestone_reward", numberComp, rewardComp).withStyle(ChatFormatting.GREEN)));
                }
            }
        }

        checkAndAwardCompletedCategories(player, data);

        Map<String, List<String>> networkMap = new HashMap<>();
        data.unlockedActions.forEach((id, set) -> networkMap.put(id, new ArrayList<>(set)));
        Services.PLATFORM.sendSyncDataPacketToClient(player, networkMap, new ArrayList<>(data.rewardedCategories));

        handleLeaderboardRequest(player);
    }

    private static void checkAndAwardCompletedCategories(ServerPlayer player, PlayerData data) {
        if (EntityTypeScanner.SCANNED_CATEGORIES.isEmpty()) EntityTypeScanner.scanEntities();

        int completedRealCategories = 0;

        for (EntityTypeScanner.CategoryData cat : EntityTypeScanner.SCANNED_CATEGORIES.values()) {
            if (!data.rewardedCategories.contains(cat.categoryId)) {

                int gathered = 0;
                for (String entityId : cat.entityIds) {
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId)).map(net.minecraft.core.Holder::value).orElse(null);
                    if (type != null) {
                        List<String> required = getRequiredActions(entityId, type, player.level());
                        Set<String> unlocked = data.unlockedActions.getOrDefault(entityId, new java.util.HashSet<>());
                        if (!required.isEmpty() && unlocked.size() >= required.size()) {
                            gathered++;
                        }
                    }
                }

                if (gathered > 0 && gathered == cat.entityIds.size()) {
                    handleCategoryReward(player, cat.categoryId);
                    player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_TWINKLE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
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

        player.sendSystemMessage(Component.empty().append(prefix).append(Component.translatable("chat.r3ct_bestiary.category_complete", catNameComp, trophyComp).withStyle(ChatFormatting.GREEN)));

        data.rewardedCategories.add(categoryId);

        int catSize = data.rewardedCategories.size();
        if (data.rewardedCategories.contains("ALL_COMPLETED")) catSize--;

        if (catSize >= 1) grantAdvancement(player, "r3ct_bestiary:category_1");
        if (catSize >= 2) grantAdvancement(player, "r3ct_bestiary:category_2");

        ModState.get(player.level().getServer()).setDirty();

        Map<String, List<String>> networkMap = new HashMap<>();
        data.unlockedActions.forEach((id, set) -> networkMap.put(id, new ArrayList<>(set)));
        Services.PLATFORM.sendSyncDataPacketToClient(player, networkMap, new ArrayList<>(data.rewardedCategories));
    }

    public static void handleLeaderboardRequest(ServerPlayer player) {
        grantAdvancement(player, "r3ct_bestiary:root");

        net.minecraft.server.MinecraftServer server = player.level().getServer();
        ModState state = ModState.get(server);

        List<LeaderboardDataPayload.TopPlayerEntry> allEntries = new ArrayList<>();

        state.players.forEach((uuid, data) -> {
            String name = data.lastKnownName;

            int leaderboardScore = getTotalCompletedMobs(data, server.overworld());

            if (!name.equals("Unknown") && leaderboardScore > 0) {
                allEntries.add(new LeaderboardDataPayload.TopPlayerEntry(name, leaderboardScore, new ArrayList<>(data.unlockedActions.keySet())));
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

    public static void debugCompleteCategory(net.minecraft.server.level.ServerPlayer player, String categoryId) {
        if (!player.isCreative()) return;

        if (com.r3ct.bestiary.scanner.EntityTypeScanner.SCANNED_CATEGORIES.isEmpty()) {
            com.r3ct.bestiary.scanner.EntityTypeScanner.scanEntities();
        }

        PlayerData data = ModState.getPlayerData(player.level().getServer(), player.getUUID());
        com.r3ct.bestiary.scanner.EntityTypeScanner.CategoryData catData = com.r3ct.bestiary.scanner.EntityTypeScanner.SCANNED_CATEGORIES.get(categoryId);

        if (catData != null) {
            for (String entityId : catData.entityIds) {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId)).map(net.minecraft.core.Holder::value).orElse(null);
                if (type != null) {
                    List<String> required = getRequiredActions(entityId, type, player.level());
                    if (!required.isEmpty()) {
                        data.unlockedActions.put(entityId, new java.util.HashSet<>(required));
                    }
                }
            }

            handleCategoryReward(player, categoryId);
            ModState.get(player.level().getServer()).setDirty();

            Map<String, List<String>> networkMap = new HashMap<>();
            data.unlockedActions.forEach((id, set) -> networkMap.put(id, new ArrayList<>(set)));
            Services.PLATFORM.sendSyncDataPacketToClient(player, networkMap, new ArrayList<>(data.rewardedCategories));

            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("chat.r3ct_bestiary.dev_category_complete", categoryId).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}