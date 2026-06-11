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

public class CollectionConfig {

    public static Set<String> blacklistedMods = new HashSet<>();
    public static Set<String> blacklistedTabs = new HashSet<>();
    public static Set<String> blacklistedItems = new HashSet<>();

    public static float catalogScale = 1.0f;

    public static int xpCommon = 10;
    public static int xpUncommon = 50;
    public static int xpRare = 100;
    public static int xpEpic = 500;
    public static int milestoneInterval = 100;
    public static List<LootEntry> milestoneRewards = new ArrayList<>();
    public static Map<String, String> categoryRewards = new HashMap<>();

    private static final int SERVER_CONFIG_VERSION = 1;
    private static final int CLIENT_CONFIG_VERSION = 1;
    private static final int REWARDS_CONFIG_VERSION = 1;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Paths.get("config", "r3ct_bestiary");

    private static final Path SERVER_PATH = CONFIG_DIR.resolve("r3ct_collection_items.json");
    private static final Path CLIENT_PATH = CONFIG_DIR.resolve("r3ct_collection_client.json");
    private static final Path REWARDS_PATH = CONFIG_DIR.resolve("r3ct_collection_rewards.json");

    private static void copyDefaultConfig(Path target, String resourceName) {
        try {
            if (!Files.exists(target.getParent())) Files.createDirectories(target.getParent());
            InputStream is = CollectionConfig.class.getResourceAsStream("/assets/r3ct_bestiary/config/" + resourceName);
            if (is != null) {
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                is.close();
            }
        } catch (IOException e) {
            System.err.println("[R3CT-Collection] Could not copy default config: " + resourceName);
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
                System.out.println("[R3CT-Collection] Outdated config detected! Backed up to: " + oldName);
                copyDefaultConfig(path, resourceName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void load() {
        checkAndMigrate(SERVER_PATH, "r3ct_collection_items.json", SERVER_CONFIG_VERSION);
        try (FileReader reader = new FileReader(SERVER_PATH.toFile())) {
            ItemsData data = GSON.fromJson(reader, ItemsData.class);
            if (data != null) {
                if (data.blacklistedMods != null) blacklistedMods = data.blacklistedMods;
                if (data.blacklistedTabs != null) blacklistedTabs = data.blacklistedTabs;
                if (data.blacklistedItems != null) blacklistedItems = data.blacklistedItems;
            }
        } catch (Exception e) { System.err.println("[R3CT-Collection] Error loading items config!"); }

        checkAndMigrate(CLIENT_PATH, "r3ct_collection_client.json", CLIENT_CONFIG_VERSION);
        try (FileReader reader = new FileReader(CLIENT_PATH.toFile())) {
            ClientData data = GSON.fromJson(reader, ClientData.class);
            if (data != null) catalogScale = data.catalogScale;
        } catch (Exception e) { System.err.println("[R3CT-Collection] Error loading client config!"); }

        checkAndMigrate(REWARDS_PATH, "r3ct_collection_rewards.json", REWARDS_CONFIG_VERSION);
        try (FileReader reader = new FileReader(REWARDS_PATH.toFile())) {
            RewardsData data = GSON.fromJson(reader, RewardsData.class);
            if (data != null) {
                xpCommon = data.xpCommon;
                xpUncommon = data.xpUncommon;
                xpRare = data.xpRare;
                xpEpic = data.xpEpic;
                milestoneInterval = data.milestoneInterval;
                if (data.milestoneRewards != null) milestoneRewards = data.milestoneRewards;
                if (data.categoryRewards != null) categoryRewards = data.categoryRewards;
            }
        } catch (Exception e) { System.err.println("[R3CT-Collection] Error loading rewards config!"); }
    }

    public static void saveItems() {
        try {
            if (!Files.exists(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
            try (FileWriter writer = new FileWriter(SERVER_PATH.toFile())) { GSON.toJson(new ItemsData(), writer); }
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
        saveItems();
        saveClient();
        saveRewards();
    }

    private static class ItemsData {
        int version = SERVER_CONFIG_VERSION;
        Set<String> blacklistedMods = CollectionConfig.blacklistedMods;
        Set<String> blacklistedTabs = CollectionConfig.blacklistedTabs;
        Set<String> blacklistedItems = CollectionConfig.blacklistedItems;
    }

    private static class ClientData {
        int version = CLIENT_CONFIG_VERSION;
        float catalogScale = CollectionConfig.catalogScale;
    }

    private static class RewardsData {
        int version = REWARDS_CONFIG_VERSION;
        int xpCommon = CollectionConfig.xpCommon;
        int xpUncommon = CollectionConfig.xpUncommon;
        int xpRare = CollectionConfig.xpRare;
        int xpEpic = CollectionConfig.xpEpic;
        int milestoneInterval = CollectionConfig.milestoneInterval;
        List<LootEntry> milestoneRewards = CollectionConfig.milestoneRewards;
        Map<String, String> categoryRewards = CollectionConfig.categoryRewards;
    }

    public static class LootEntry {
        public String item;
        public int min_amount;
        public int max_amount;
        public int weight;
        public String color;

        public LootEntry(String item, int min, int max, int weight, String color) {
            this.item = item;
            this.min_amount = min;
            this.max_amount = max;
            this.weight = weight;
            this.color = color;
        }
    }

    public static LootEntry getRandomMilestoneReward() {
        if (milestoneRewards.isEmpty()) return null;
        int totalWeight = milestoneRewards.stream().mapToInt(e -> e.weight).sum();
        if (totalWeight <= 0) return null;

        int random = java.util.concurrent.ThreadLocalRandom.current().nextInt(totalWeight);

        for (LootEntry entry : milestoneRewards) {
            random -= entry.weight;
            if (random < 0) return entry;
        }
        return null;
    }
}