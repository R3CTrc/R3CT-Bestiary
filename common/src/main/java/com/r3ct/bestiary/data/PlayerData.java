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
    public Map<String, Integer> killCounts = new HashMap<>();
    public Set<String> rewardedCategories = new HashSet<>();
    public Map<String, Double> rideDistances = new HashMap<>();

    public boolean receivedMigrationRefund = false;

    public static final Codec<PlayerData> CODEC = CompoundTag.CODEC.xmap(PlayerData::fromNbt, PlayerData::toNbt);

    public PlayerData() {
        killCounts.clear();
        rewardedCategories.clear();
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        nbt.putString("lastKnownName", lastKnownName);
        nbt.putBoolean("receivedMigrationRefund", receivedMigrationRefund);

        CompoundTag killsNbt = new CompoundTag();
        killCounts.forEach(killsNbt::putInt);
        nbt.put("killCounts", killsNbt);

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

        if (nbt.contains("killCounts")) {
            CompoundTag killsNbt = nbt.getCompound("killCounts").orElse(new CompoundTag());
            for (String key : killsNbt.keySet()) {
                killsNbt.getInt(key).ifPresent(count -> data.killCounts.put(key, count));
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