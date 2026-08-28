package com.r3ct.bestiary.client.data;

import com.r3ct.bestiary.network.MobBaseStats;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClientPlayerData {
    public static Map<String, Set<String>> unlockedActions = new HashMap<>();
    public static Set<String> rewardedCategories = new HashSet<>();
    public static java.util.List<com.r3ct.bestiary.network.LeaderboardDataPayload.TopPlayerEntry> leaderboardData = new java.util.ArrayList<>();
    public static Map<String, MobBaseStats> serverMobStats = new HashMap<>();
}