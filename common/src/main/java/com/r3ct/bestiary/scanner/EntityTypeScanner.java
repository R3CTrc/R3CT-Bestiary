package com.r3ct.bestiary.scanner;

import com.r3ct.bestiary.config.BestiaryConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EntityTypeScanner {

    public static final Map<String, CategoryData> SCANNED_CATEGORIES = new LinkedHashMap<>();

    public static class CategoryData {
        public final String categoryId;
        public final String namespace;
        public final String type;
        public final List<String> entityIds;

        public CategoryData(String categoryId, String namespace, String type) {
            this.categoryId = categoryId;
            this.namespace = namespace;
            this.type = type;
            this.entityIds = new ArrayList<>();
        }

        public String getFormattedModName() {
            if (namespace.equals("minecraft")) return "Minecraft";

            String[] parts = namespace.split("_");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
                }
            }
            return sb.toString().trim();
        }
    }

    public static void scanEntities() {
        SCANNED_CATEGORIES.clear();

        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            MobCategory category = entityType.getCategory();

            if (category == MobCategory.MISC) {
                continue;
            }

            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            String idString = id.toString();
            String namespace = id.getNamespace();
            String internalCategoryName = category.getName();

            if (BestiaryConfig.blacklistedMods.contains(namespace)) continue;
            if (BestiaryConfig.blacklistedCategories.contains(internalCategoryName)) continue;
            if (BestiaryConfig.blacklistedMobs.contains(idString)) continue;

            String type;
            if (BestiaryConfig.customBosses.contains(idString) || BestiaryConfig.bossMods.contains(namespace)) {
                type = "bosses";
            }
            else if (category == MobCategory.MONSTER) {
                type = "monsters";
            } else {
                type = "creatures";
            }

            String categoryId = namespace + ":" + type;

            SCANNED_CATEGORIES.computeIfAbsent(categoryId, k -> new CategoryData(k, namespace, type)).entityIds.add(idString);
        }

        com.r3ct.bestiary.Constants.LOG.info("Bestiary Scanner: Successfully loaded {} mob categories.", SCANNED_CATEGORIES.size());
    }
}