package com.r3ct.bestiary.logic;

import com.r3ct.bestiary.config.CollectionConfig;
import com.r3ct.bestiary.data.ModState;
import com.r3ct.bestiary.data.PlayerData;
import com.r3ct.bestiary.network.LeaderboardDataPayload;
import com.r3ct.bestiary.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ServerItemHandler {

    private static void grantAdvancement(ServerPlayer player, String advancementId) {
        net.minecraft.server.MinecraftServer server = player.level().getServer();
        Identifier id = Identifier.parse(advancementId);
        net.minecraft.advancements.AdvancementHolder advancement = server.getAdvancements().get(id);

        if (advancement != null) {
            player.getAdvancements().award(advancement, "unlocked");
        }
    }

    public static void handleItemSubmit(ServerPlayer player, String itemId, int slotId) {
        String[] parts = itemId.split("#");
        Identifier id = Identifier.parse(parts[0]);
        Item targetItem = BuiltInRegistries.ITEM.get(id).map(net.minecraft.core.Holder::value).orElse(Items.AIR);

        if (targetItem == Items.AIR) return;

        PlayerData data = ModState.getPlayerData(player.level().getServer(), player.getUUID());

        data.lastKnownName = player.getName().getString();
        ModState.get(player.level().getServer()).setDirty();

        if (data.unlockedItems.contains(itemId)) return;

        boolean foundAndRemoved = false;

        if (player.isCreative()) {
            foundAndRemoved = true;
        } else {
            ItemStack stack = player.getInventory().getItem(slotId);
            if (!stack.isEmpty() && getUniqueItemId(stack).equals(itemId)) {
                stack.shrink(1);
                foundAndRemoved = true;
            }
        }

        if (foundAndRemoved) {
            CollectionConfig.load();

            int sizeBefore = data.unlockedItems.size();
            data.unlockedItems.add(itemId);
            int sizeAfter = data.unlockedItems.size();

            int xpToGive = CollectionConfig.xpCommon;
            net.minecraft.world.item.Rarity rarity = new ItemStack(targetItem).getRarity();

            if (rarity == net.minecraft.world.item.Rarity.UNCOMMON) {
                xpToGive = CollectionConfig.xpUncommon;
            } else if (rarity == net.minecraft.world.item.Rarity.RARE) {
                xpToGive = CollectionConfig.xpRare;
            } else if (rarity == net.minecraft.world.item.Rarity.EPIC) {
                xpToGive = CollectionConfig.xpEpic;
            }

            player.giveExperiencePoints(xpToGive);

            if (sizeAfter >= 1) grantAdvancement(player, "r3ct_bestiary:first_item");
            if (sizeAfter >= 100) grantAdvancement(player, "r3ct_bestiary:items_100");
            if (sizeAfter >= 500) grantAdvancement(player, "r3ct_bestiary:items_500");
            if (sizeAfter >= 1000) grantAdvancement(player, "r3ct_bestiary:items_1000");

            int interval = CollectionConfig.milestoneInterval;
            if (interval > 0 && (sizeBefore / interval < sizeAfter / interval)) {
                CollectionConfig.LootEntry reward = CollectionConfig.getRandomMilestoneReward();
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

                        var prefix = net.minecraft.network.chat.Component.literal("[Collection] ").withStyle(net.minecraft.ChatFormatting.AQUA);
                        var numberComp = net.minecraft.network.chat.Component.literal(String.valueOf(sizeAfter)).withStyle(net.minecraft.ChatFormatting.YELLOW);
                        var rewardComp = net.minecraft.network.chat.Component.literal(amount + "x ")
                                .withStyle(rewardColor)
                                .append(savedItemName.withStyle(rewardColor));

                        player.sendSystemMessage(
                                net.minecraft.network.chat.Component.empty()
                                        .append(prefix)
                                        .append(net.minecraft.network.chat.Component.translatable("chat.r3ct_collection.milestone_reward", numberComp, rewardComp).withStyle(net.minecraft.ChatFormatting.GREEN))
                        );
                    }
                }
            }
            checkAndAwardCompletedCategories(player, data);

            ModState.get(player.level().getServer()).setDirty();
            Services.PLATFORM.sendSyncDataPacketToClient(player, data.unlockedItems, data.rewardedCategories);

            handleLeaderboardRequest(player);
        }
    }

    private static void checkAndAwardCompletedCategories(ServerPlayer player, PlayerData data) {
        if (com.r3ct.bestiary.scanner.CreativeTabScanner.SCANNED_SUBCATEGORIES.isEmpty()) {
            com.r3ct.bestiary.scanner.CreativeTabScanner.scanAllTabs(
                    player.level().enabledFeatures(),
                    player.level().registryAccess(),
                    false
            );
        }

        int completedRealCategories = 0;

        for (com.r3ct.bestiary.scanner.CreativeTabScanner.SubCategory cat : com.r3ct.bestiary.scanner.CreativeTabScanner.SCANNED_SUBCATEGORIES.values()) {
            if (!data.rewardedCategories.contains(cat.tabId)) {

                int gathered = 0;
                for (ItemStack stack : cat.items) {
                    String id = getUniqueItemId(stack);
                    if (data.unlockedItems.contains(id)) gathered++;
                }

                if (gathered > 0 && gathered == cat.items.size()) {
                    handleCategoryReward(player, cat.tabId);
                    player.level().playSound(null, player.blockPosition(),
                            net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_TWINKLE,
                            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }

            if (data.rewardedCategories.contains(cat.tabId)) {
                completedRealCategories++;
            }
        }

        if (completedRealCategories >= com.r3ct.bestiary.scanner.CreativeTabScanner.SCANNED_SUBCATEGORIES.size()
                && !com.r3ct.bestiary.scanner.CreativeTabScanner.SCANNED_SUBCATEGORIES.isEmpty()) {
            if (!data.rewardedCategories.contains("ALL_COMPLETED")) {
                handleCategoryReward(player, "ALL_COMPLETED");
            }
        }
    }

    public static void handleCategoryReward(ServerPlayer player, String tabId) {
        PlayerData data = ModState.getPlayerData(player.level().getServer(), player.getUUID());
        data.lastKnownName = player.getName().getString();

        if (data.rewardedCategories.contains(tabId)) return;

        if (tabId.equals("ALL_COMPLETED")) {
            grantAdvancement(player, "r3ct_bestiary:all_completed");
            data.rewardedCategories.add("ALL_COMPLETED");
            ModState.get(player.level().getServer()).setDirty();
            return;
        }

        CollectionConfig.load();
        String rewardItemId = CollectionConfig.categoryRewards.getOrDefault(tabId, CollectionConfig.categoryRewards.get("modded_generic"));

        if (rewardItemId != null) {
            Item rewardItem = BuiltInRegistries.ITEM.get(Identifier.parse(rewardItemId)).map(net.minecraft.core.Holder::value).orElse(Items.AIR);
            if (rewardItem != Items.AIR) {
                ItemStack rewardStack = new ItemStack(rewardItem, 1);

                net.minecraft.network.chat.MutableComponent customName = net.minecraft.network.chat.Component.literal(player.getName().getString())
                        .withStyle(net.minecraft.ChatFormatting.AQUA);

                customName.append(net.minecraft.network.chat.Component.literal(" - ").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));

                customName.append(net.minecraft.network.chat.Component.translatable(rewardItem.getDescriptionId()).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));

                if (rewardItemId.equals("r3ct_bestiary:trophy_mod")) {
                    net.minecraft.world.item.CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(Identifier.parse(tabId)).map(net.minecraft.core.Holder::value).orElse(null);
                    if (tab != null) {
                        customName.append(net.minecraft.network.chat.Component.literal(" - ").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE))
                                .append(tab.getDisplayName().copy().withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
                    }
                }

                rewardStack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, customName);

                var savedTrophyName = rewardStack.getHoverName().copy();

                giveItemToPlayer(player, rewardStack);

                net.minecraft.world.item.CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(Identifier.parse(tabId)).map(net.minecraft.core.Holder::value).orElse(null);
                net.minecraft.network.chat.Component tabName = (tab != null) ? tab.getDisplayName() : net.minecraft.network.chat.Component.literal(tabId);

                var prefix = net.minecraft.network.chat.Component.literal("[Collection] ").withStyle(net.minecraft.ChatFormatting.AQUA);
                var catNameComp = tabName.copy().withStyle(net.minecraft.ChatFormatting.YELLOW);
                var trophyComp = savedTrophyName.withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);

                player.sendSystemMessage(
                        net.minecraft.network.chat.Component.empty()
                                .append(prefix)
                                .append(net.minecraft.network.chat.Component.translatable("chat.r3ct_collection.category_complete", catNameComp, trophyComp).withStyle(net.minecraft.ChatFormatting.GREEN))
                );

                data.rewardedCategories.add(tabId);

                int catSize = data.rewardedCategories.size();
                if (data.rewardedCategories.contains("ALL_COMPLETED")) catSize--;

                if (catSize >= 1) grantAdvancement(player, "r3ct_bestiary:category_1");
                if (catSize >= 5) grantAdvancement(player, "r3ct_bestiary:category_5");

                ModState.get(player.level().getServer()).setDirty();
                Services.PLATFORM.sendSyncDataPacketToClient(player, data.unlockedItems, data.rewardedCategories);
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
            if (!name.equals("Unknown") && !data.unlockedItems.isEmpty()) {
                allEntries.add(new LeaderboardDataPayload.TopPlayerEntry(name, data.unlockedItems.size(), new ArrayList<>(data.unlockedItems)));
            }
        });

        allEntries.sort((e1, e2) -> Integer.compare(e2.totalItems(), e1.totalItems()));

        List<LeaderboardDataPayload.TopPlayerEntry> top10 = allEntries.stream().limit(10).toList();
        Services.PLATFORM.sendLeaderboardDataPacketToClient(player, top10);
    }

    private static void giveItemToPlayer(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            ItemEntity drop = player.drop(stack, false);
            if (drop != null) drop.setNoPickUpDelay();
        }
    }

    public static String getUniqueItemId(ItemStack stack) {
        String baseId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

        if (stack.has(net.minecraft.core.component.DataComponents.POTION_CONTENTS)) {
            net.minecraft.world.item.alchemy.PotionContents contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
            if (contents != null && contents.potion().isPresent()) {
                String potionId = contents.potion().get().unwrapKey().map(key -> key.identifier().toString()).orElse("");
                if (!potionId.isEmpty()) {
                    return baseId + "#" + potionId;
                }
            }
        }
        return baseId;
    }

    public static void refundMigrationTrophies(net.minecraft.server.level.ServerPlayer player, PlayerData data) {
        if (data.receivedMigrationRefund) return;

        if (data.rewardedCategories.isEmpty() || (data.rewardedCategories.size() == 1 && data.rewardedCategories.contains("ALL_COMPLETED"))) {
            data.receivedMigrationRefund = true;
            ModState.get(player.level().getServer()).setDirty();
            return;
        }

        com.r3ct.bestiary.config.CollectionConfig.load();
        boolean gaveAny = false;

        for (String tabId : data.rewardedCategories) {
            if (tabId.equals("ALL_COMPLETED")) continue;

            String rewardItemId = com.r3ct.bestiary.config.CollectionConfig.categoryRewards.getOrDefault(tabId, com.r3ct.bestiary.config.CollectionConfig.categoryRewards.get("modded_generic"));

            if (rewardItemId != null) {
                Item rewardItem = BuiltInRegistries.ITEM.get(Identifier.parse(rewardItemId)).map(net.minecraft.core.Holder::value).orElse(Items.AIR);
                if (rewardItem != Items.AIR) {
                    ItemStack rewardStack = new ItemStack(rewardItem, 1);

                    net.minecraft.network.chat.MutableComponent customName = net.minecraft.network.chat.Component.literal(player.getName().getString())
                            .withStyle(net.minecraft.ChatFormatting.AQUA)
                            .append(net.minecraft.network.chat.Component.literal(" - ").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE))
                            .append(net.minecraft.network.chat.Component.translatable(rewardItem.getDescriptionId()).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));

                    if (rewardItemId.equals("r3ct_bestiary:trophy_mod")) {
                        net.minecraft.world.item.CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(Identifier.parse(tabId)).map(net.minecraft.core.Holder::value).orElse(null);
                        if (tab != null) {
                            customName.append(net.minecraft.network.chat.Component.literal(" - ").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE))
                                    .append(tab.getDisplayName().copy().withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
                        }
                    }

                    rewardStack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, customName);
                    giveItemToPlayer(player, rewardStack);
                    gaveAny = true;
                }
            }
        }
        data.receivedMigrationRefund = true;
        ModState.get(player.level().getServer()).setDirty();

        if (gaveAny) {
            var prefix = net.minecraft.network.chat.Component.literal("[Collection] ").withStyle(net.minecraft.ChatFormatting.AQUA);
            var message = net.minecraft.network.chat.Component.translatable("chat.r3ct_collection.migration_refund").withStyle(net.minecraft.ChatFormatting.GREEN);

            player.sendSystemMessage(net.minecraft.network.chat.Component.empty().append(prefix).append(message));

            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}