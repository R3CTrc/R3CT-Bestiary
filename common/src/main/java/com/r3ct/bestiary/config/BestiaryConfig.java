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

    public static Set<String> bossMods = new HashSet<>();

    public static Set<String> customBosses = new HashSet<>(Arrays.asList(
            "minecraft:ender_dragon", "minecraft:wither", "minecraft:warden", "minecraft:elder_guardian"
    ));

    public static List<Integer> defaultKillsBosses = Arrays.asList(1, 3, 5, 10);
    public static List<Integer> defaultKillsMonsters = Arrays.asList(1, 25, 50, 100);
    public static List<Integer> defaultKillsCreatures = Arrays.asList(1, 25, 50, 100);

    public static Map<String, List<Integer>> customKillRequirements = new HashMap<>();

    public static float catalogScale = 1.0f;

    public static int xpBosses = 500;
    public static int xpMonsters = 100;
    public static int xpCreatures = 50;

    public static int xpStar1 = 500;
    public static int xpStar2 = 1500;
    public static int xpStar3 = 3000;

    public static int milestoneInterval = 10;
    public static List<LootEntry> milestoneRewards = new ArrayList<>();
    public static Map<String, String> categoryRewards = new HashMap<>();

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
                if (data.bossMods != null) bossMods = data.bossMods;
                if (data.customBosses != null) customBosses = data.customBosses;
                if (data.defaultKillsBosses != null && !data.defaultKillsBosses.isEmpty()) defaultKillsBosses = data.defaultKillsBosses;
                if (data.defaultKillsMonsters != null && !data.defaultKillsMonsters.isEmpty()) defaultKillsMonsters = data.defaultKillsMonsters;
                if (data.defaultKillsCreatures != null && !data.defaultKillsCreatures.isEmpty()) defaultKillsCreatures = data.defaultKillsCreatures;
                if (data.customKillRequirements != null) customKillRequirements = data.customKillRequirements;
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
                xpBosses = data.xpBosses;
                xpMonsters = data.xpMonsters;
                xpCreatures = data.xpCreatures;
                xpStar1 = data.xpStar1;
                xpStar2 = data.xpStar2;
                xpStar3 = data.xpStar3;
                milestoneInterval = data.milestoneInterval;
                if (data.milestoneRewards != null) milestoneRewards = data.milestoneRewards;
                if (data.categoryRewards != null) categoryRewards = data.categoryRewards;
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
        Set<String> bossMods = BestiaryConfig.bossMods;
        Set<String> customBosses = BestiaryConfig.customBosses;
        List<Integer> defaultKillsBosses = BestiaryConfig.defaultKillsBosses;
        List<Integer> defaultKillsMonsters = BestiaryConfig.defaultKillsMonsters;
        List<Integer> defaultKillsCreatures = BestiaryConfig.defaultKillsCreatures;
        Map<String, List<Integer>> customKillRequirements = BestiaryConfig.customKillRequirements;
    }

    private static class ClientData {
        int version = CLIENT_CONFIG_VERSION;
        float catalogScale = BestiaryConfig.catalogScale;
    }

    private static class RewardsData {
        int version = REWARDS_CONFIG_VERSION;
        int xpBosses = BestiaryConfig.xpBosses;
        int xpMonsters = BestiaryConfig.xpMonsters;
        int xpCreatures = BestiaryConfig.xpCreatures;
        int xpStar1 = BestiaryConfig.xpStar1;
        int xpStar2 = BestiaryConfig.xpStar2;
        int xpStar3 = BestiaryConfig.xpStar3;
        int milestoneInterval = BestiaryConfig.milestoneInterval;
        List<LootEntry> milestoneRewards = BestiaryConfig.milestoneRewards;
        Map<String, String> categoryRewards = BestiaryConfig.categoryRewards;
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
}