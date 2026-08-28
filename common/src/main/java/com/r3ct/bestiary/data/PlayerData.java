package com.r3ct.bestiary.data;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerData {
    public String lastKnownName = "Unknown";

    public Map<String, Set<String>> unlockedActions = new HashMap<>();
    public Set<String> rewardedCategories = new HashSet<>();
    public boolean receivedMigrationRefund = false;
    public static final Codec<PlayerData> CODEC = CompoundTag.CODEC.xmap(PlayerData::fromNbt, PlayerData::toNbt);

    public PlayerData() {
        unlockedActions.clear();
        rewardedCategories.clear();
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        nbt.putString("lastKnownName", lastKnownName);
        nbt.putBoolean("receivedMigrationRefund", receivedMigrationRefund);

        CompoundTag actionsNbt = new CompoundTag();
        unlockedActions.forEach((entityId, actions) -> {
            ListTag list = new ListTag();
            for (String action : actions) {
                list.add(StringTag.valueOf(action));
            }
            actionsNbt.put(entityId, list);
        });
        nbt.put("unlockedActions", actionsNbt);

        ListTag categoriesList = new ListTag();
        for (String cat : rewardedCategories) categoriesList.add(StringTag.valueOf(cat != null ? cat : ""));
        nbt.put("rewardedCategories", categoriesList);

        return nbt;
    }

    public static PlayerData fromNbt(CompoundTag nbt) {
        PlayerData data = new PlayerData();

        if (nbt.contains("lastKnownName")) {
            data.lastKnownName = nbt.getString("lastKnownName").orElse("Unknown");
        }

        if (nbt.contains("receivedMigrationRefund")) {
            data.receivedMigrationRefund = nbt.getBoolean("receivedMigrationRefund").orElse(false);
        }

        if (nbt.contains("unlockedActions")) {
            CompoundTag actionsNbt = nbt.getCompound("unlockedActions").orElse(new CompoundTag());
            for (String key : actionsNbt.keySet()) {
                Tag tag = actionsNbt.get(key);
                if (tag instanceof ListTag list) {
                    Set<String> actions = new HashSet<>();
                    for (int i = 0; i < list.size(); i++) {
                        list.getString(i).ifPresent(actions::add);
                    }
                    data.unlockedActions.put(key, actions);
                }
            }
        }

        if (nbt.contains("rewardedCategories")) {
            Tag tag = nbt.get("rewardedCategories");
            if (tag instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) list.getString(i).ifPresent(data.rewardedCategories::add);
            }
        }
        return data;
    }
}