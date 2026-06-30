package com.r3ct.bestiary.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class BestiaryConfig {

    public static Set<String> blacklistedMods = new HashSet<>();
    public static Set<String> blacklistedCategories = new HashSet<>();
    public static Set<String> blacklistedMobs = new HashSet<>();

    public static Map<String, String> modCategoryOverrides = new HashMap<>();
    public static Map<String, String> mobCategoryOverrides = new HashMap<>();

    public static List<Integer> defaultProgressBosses = Arrays.asList(1, 3, 5, 10);
    public static List<Integer> defaultProgressMonsters = Arrays.asList(1, 25, 50, 100);
    public static List<Integer> defaultProgressCreatures = Arrays.asList(1, 25, 50, 100);

    public static Map<String, List<Integer>> customProgressRequirements = new HashMap<>();

    public static int interactionCooldownMinutes = 30;
    public static int rideDistanceBlocks = 500;

    public static float catalogScale = 1.0f;

    // NOWOŚĆ: Pule nagród XP zamienione na Listy [Baza, ★, ★★, ★★★]
    public static List<Integer> xpBosses = Arrays.asList(500, 1500, 3000, 5000);
    public static List<Integer> xpMonsters = Arrays.asList(100, 500, 1500, 3000);
    public static List<Integer> xpCreatures = Arrays.asList(50, 250, 1000, 2000);

    public static int milestoneInterval = 10;
    public static List<LootEntry> milestoneRewards = new ArrayList<>();

    private static final int SERVER_CONFIG_VERSION = 1;
    private static final int CLIENT_CONFIG_VERSION = 1;
    private static final int REWARDS_CONFIG_VERSION = 1;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Paths.get("config", "r3ct_bestiary");

    private static final Path MOBS_PATH = CONFIG_DIR.resolve("r3ct_bestiary_mobs.json");
    private static final Path CLIENT_PATH = CONFIG_DIR.resolve("r3ct_bestiary_client.json");
    private static final Path REWARDS_PATH = CONFIG_DIR.resolve("r3ct_bestiary_rewards.json");

    private static void copyDefaultConfig(Path target, String resourceName) {
        try {
            if (!Files.exists(target.getParent())) Files.createDirectories(target.getParent());
            InputStream is = BestiaryConfig.class.getResourceAsStream("/assets/r3ct_bestiary/config/" + resourceName);
            if (is != null) {
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                is.close();
            }
        } catch (IOException e) {
            System.err.println("[R3CT-Bestiary] Could not copy default config: " + resourceName);
            e.printStackTrace();
        }
    }

    private static void checkAndMigrate(Path path, String resourceName, int targetVersion) {
        if (!Files.exists(path)) {
            copyDefaultConfig(path, resourceName);
            return;
        }

        boolean needsUpdate = false;
        try (FileReader reader = new FileReader(path.toFile())) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            int version = json.has("version") ? json.get("version").getAsInt() : 0;
            if (version < targetVersion) needsUpdate = true;
        } catch (Exception e) {
            needsUpdate = true;
        }

        if (needsUpdate) {
            try {
                String oldName = path.getFileName().toString().replace(".json", "_OLD.json");
                Path backupPath = path.resolveSibling(oldName);
                Files.move(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[R3CT-Bestiary] Outdated config detected! Backed up to: " + oldName);
                copyDefaultConfig(path, resourceName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void load() {
        checkAndMigrate(MOBS_PATH, "r3ct_bestiary_mobs.json", SERVER_CONFIG_VERSION);
        try (FileReader reader = new FileReader(MOBS_PATH.toFile())) {
            MobsData data = GSON.fromJson(reader, MobsData.class);
            if (data != null) {
                if (data.blacklistedMods != null) blacklistedMods = data.blacklistedMods;
                if (data.blacklistedCategories != null) blacklistedCategories = data.blacklistedCategories;
                if (data.blacklistedMobs != null) blacklistedMobs = data.blacklistedMobs;
                if (data.modCategoryOverrides != null) modCategoryOverrides = data.modCategoryOverrides;
                if (data.mobCategoryOverrides != null) mobCategoryOverrides = data.mobCategoryOverrides;

                if (data.mobCategoryOverrides == null || data.mobCategoryOverrides.isEmpty()) {
                    mobCategoryOverrides.put("minecraft:ender_dragon", "bosses");
                    mobCategoryOverrides.put("minecraft:wither", "bosses");
                    mobCategoryOverrides.put("minecraft:warden", "bosses");
                    mobCategoryOverrides.put("minecraft:elder_guardian", "bosses");
                }

                if (data.defaultProgressBosses != null && !data.defaultProgressBosses.isEmpty()) defaultProgressBosses = data.defaultProgressBosses;
                if (data.defaultProgressMonsters != null && !data.defaultProgressMonsters.isEmpty()) defaultProgressMonsters = data.defaultProgressMonsters;
                if (data.defaultProgressCreatures != null && !data.defaultProgressCreatures.isEmpty()) defaultProgressCreatures = data.defaultProgressCreatures;
                if (data.customProgressRequirements != null) customProgressRequirements = data.customProgressRequirements;

                interactionCooldownMinutes = data.interactionCooldownMinutes;
                rideDistanceBlocks = data.rideDistanceBlocks;
            }
        } catch (Exception e) { System.err.println("[R3CT-Bestiary] Error loading mobs config!"); }

        checkAndMigrate(CLIENT_PATH, "r3ct_bestiary_client.json", CLIENT_CONFIG_VERSION);
        try (FileReader reader = new FileReader(CLIENT_PATH.toFile())) {
            ClientData data = GSON.fromJson(reader, ClientData.class);
            if (data != null) catalogScale = data.catalogScale;
        } catch (Exception e) { System.err.println("[R3CT-Bestiary] Error loading client config!"); }

        checkAndMigrate(REWARDS_PATH, "r3ct_bestiary_rewards.json", REWARDS_CONFIG_VERSION);
        try (FileReader reader = new FileReader(REWARDS_PATH.toFile())) {
            RewardsData data = GSON.fromJson(reader, RewardsData.class);
            if (data != null) {
                if (data.xpBosses != null && !data.xpBosses.isEmpty()) xpBosses = data.xpBosses;
                if (data.xpMonsters != null && !data.xpMonsters.isEmpty()) xpMonsters = data.xpMonsters;
                if (data.xpCreatures != null && !data.xpCreatures.isEmpty()) xpCreatures = data.xpCreatures;
                milestoneInterval = data.milestoneInterval;
                if (data.milestoneRewards != null) milestoneRewards = data.milestoneRewards;
            }
        } catch (Exception e) { System.err.println("[R3CT-Bestiary] Error loading rewards config!"); }
    }

    public static void saveMobs() {
        try {
            if (!Files.exists(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
            try (FileWriter writer = new FileWriter(MOBS_PATH.toFile())) { GSON.toJson(new MobsData(), writer); }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static void saveClient() {
        try {
            if (!Files.exists(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
            try (FileWriter writer = new FileWriter(CLIENT_PATH.toFile())) { GSON.toJson(new ClientData(), writer); }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static void saveRewards() {
        try {
            if (!Files.exists(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
            try (FileWriter writer = new FileWriter(REWARDS_PATH.toFile())) { GSON.toJson(new RewardsData(), writer); }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void save() {
        saveMobs();
        saveClient();
        saveRewards();
    }

    private static class MobsData {
        int version = SERVER_CONFIG_VERSION;
        Set<String> blacklistedMods = BestiaryConfig.blacklistedMods;
        Set<String> blacklistedCategories = BestiaryConfig.blacklistedCategories;
        Set<String> blacklistedMobs = BestiaryConfig.blacklistedMobs;
        Map<String, String> modCategoryOverrides = BestiaryConfig.modCategoryOverrides;
        Map<String, String> mobCategoryOverrides = BestiaryConfig.mobCategoryOverrides;
        List<Integer> defaultProgressBosses = BestiaryConfig.defaultProgressBosses;
        List<Integer> defaultProgressMonsters = BestiaryConfig.defaultProgressMonsters;
        List<Integer> defaultProgressCreatures = BestiaryConfig.defaultProgressCreatures;
        Map<String, List<Integer>> customProgressRequirements = BestiaryConfig.customProgressRequirements;
        int interactionCooldownMinutes = BestiaryConfig.interactionCooldownMinutes;
        int rideDistanceBlocks = BestiaryConfig.rideDistanceBlocks;
    }

    private static class ClientData {
        int version = CLIENT_CONFIG_VERSION;
        float catalogScale = BestiaryConfig.catalogScale;
    }

    private static class RewardsData {
        int version = REWARDS_CONFIG_VERSION;
        List<Integer> xpBosses = BestiaryConfig.xpBosses;
        List<Integer> xpMonsters = BestiaryConfig.xpMonsters;
        List<Integer> xpCreatures = BestiaryConfig.xpCreatures;
        int milestoneInterval = BestiaryConfig.milestoneInterval;
        List<LootEntry> milestoneRewards = BestiaryConfig.milestoneRewards;
    }

    public static class LootEntry {
        public String item;
        public int min_amount;
        public int max_amount;
        public int chance;
        public String color;

        public LootEntry(String item, int min, int max, int chance, String color) {
            this.item = item;
            this.min_amount = min;
            this.max_amount = max;
            this.chance = chance;
            this.color = color;
        }
    }

    public static LootEntry getRandomMilestoneReward() {
        if (milestoneRewards.isEmpty()) return null;
        int totalChance = milestoneRewards.stream().mapToInt(e -> e.chance).sum();
        if (totalChance <= 0) return null;

        int random = java.util.concurrent.ThreadLocalRandom.current().nextInt(totalChance);

        for (LootEntry entry : milestoneRewards) {
            random -= entry.chance;
            if (random < 0) return entry;
        }
        return null;
    }

    public static String getMobsConfigAsString() {
        try { return Files.readString(MOBS_PATH); }
        catch (IOException e) { return "{}"; }
    }

    public static String getRewardsConfigAsString() {
        try { return Files.readString(REWARDS_PATH); }
        catch (IOException e) { return "{}"; }
    }

    public static void syncFromServer(String mobsJson, String rewardsJson) {
        try {
            MobsData mobsData = GSON.fromJson(mobsJson, MobsData.class);
            if (mobsData != null) {
                if (mobsData.blacklistedMods != null) blacklistedMods = mobsData.blacklistedMods;
                if (mobsData.blacklistedCategories != null) blacklistedCategories = mobsData.blacklistedCategories;
                if (mobsData.blacklistedMobs != null) blacklistedMobs = mobsData.blacklistedMobs;
                if (mobsData.modCategoryOverrides != null) modCategoryOverrides = mobsData.modCategoryOverrides;
                if (mobsData.mobCategoryOverrides != null) mobCategoryOverrides = mobsData.mobCategoryOverrides;
                if (mobsData.defaultProgressBosses != null && !mobsData.defaultProgressBosses.isEmpty()) defaultProgressBosses = mobsData.defaultProgressBosses;
                if (mobsData.defaultProgressMonsters != null && !mobsData.defaultProgressMonsters.isEmpty()) defaultProgressMonsters = mobsData.defaultProgressMonsters;
                if (mobsData.defaultProgressCreatures != null && !mobsData.defaultProgressCreatures.isEmpty()) defaultProgressCreatures = mobsData.defaultProgressCreatures;
                if (mobsData.customProgressRequirements != null) customProgressRequirements = mobsData.customProgressRequirements;
                interactionCooldownMinutes = mobsData.interactionCooldownMinutes;
                rideDistanceBlocks = mobsData.rideDistanceBlocks;
            }

            RewardsData rewardsData = GSON.fromJson(rewardsJson, RewardsData.class);
            if (rewardsData != null) {
                if (rewardsData.xpBosses != null && !rewardsData.xpBosses.isEmpty()) xpBosses = rewardsData.xpBosses;
                if (rewardsData.xpMonsters != null && !rewardsData.xpMonsters.isEmpty()) xpMonsters = rewardsData.xpMonsters;
                if (rewardsData.xpCreatures != null && !rewardsData.xpCreatures.isEmpty()) xpCreatures = rewardsData.xpCreatures;
                milestoneInterval = rewardsData.milestoneInterval;
                if (rewardsData.milestoneRewards != null) milestoneRewards = rewardsData.milestoneRewards;
            }
            System.out.println("[R3CT-Bestiary] Pomyślnie zsynchronizowano konfigurację serwera do RAMu!");
        } catch (Exception e) {
            System.err.println("[R3CT-Bestiary] Błąd podczas synchronizacji konfiguracji!");
            e.printStackTrace();
        }
    }
}