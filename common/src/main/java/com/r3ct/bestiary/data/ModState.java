package com.r3ct.bestiary.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ModState extends SavedData {
    public final Map<UUID, PlayerData> players = new HashMap<>();

    public static ModState get(MinecraftServer server) {

        Path dimDataDir = server.getWorldPath(LevelResource.ROOT)
                .resolve("dimensions")
                .resolve("minecraft")
                .resolve("overworld")
                .resolve("data")
                .resolve("minecraft");

        Path oldFile = dimDataDir.resolve("r3ct_collector_data.dat");
        Path newFile = dimDataDir.resolve("r3ct_collection_data.dat");

        if (Files.exists(oldFile) && !Files.exists(newFile)) {
            try {
                Files.move(oldFile, newFile);
                System.out.println("[R3CT-Collection] Successfully migrated old player data file to new name!");
            } catch (IOException e) {
                System.err.println("[R3CT-Collection] Failed to migrate old player data file!");
                e.printStackTrace();
            }
        }

        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static PlayerData getPlayerData(MinecraftServer server, UUID uuid) {
        return get(server).players.computeIfAbsent(uuid, k -> {
            get(server).setDirty();
            return new PlayerData();
        });
    }

    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        CompoundTag playersNbt = new CompoundTag();
        players.forEach((uuid, data) -> {
            playersNbt.put(uuid.toString(), data.toNbt());
        });
        nbt.put("players", playersNbt);
        return nbt;
    }

    public static ModState load(CompoundTag nbt, HolderLookup.Provider registries) {
        ModState state = new ModState();

        nbt.getCompound("players").ifPresent(playersNbt -> {
            for (String key : playersNbt.keySet()) {
                playersNbt.getCompound(key).ifPresent(playerDataNbt -> {
                    try {
                        PlayerData data = PlayerData.fromNbt(playerDataNbt);

                        Set<String> migratedItems = new HashSet<>();
                        for (String item : data.unlockedItems) {
                            migratedItems.add(item.replace("r3ct_collector:", "r3ct_bestiary:")
                                    .replace("r3ct:", "r3ct_bestiary:"));
                        }
                        data.unlockedItems = migratedItems;

                        Set<String> migratedCats = new HashSet<>();
                        for (String cat : data.rewardedCategories) {
                            migratedCats.add(cat.replace("r3ct_collector:", "r3ct_bestiary:")
                                    .replace("r3ct:", "r3ct_bestiary:"));
                        }
                        data.rewardedCategories = migratedCats;

                        state.players.put(UUID.fromString(key), data);
                    } catch (IllegalArgumentException ignored) {
                    }
                });
            }
        });

        return state;
    }

    public static final Codec<ModState> CODEC = CompoundTag.CODEC.xmap(
            nbt -> load(nbt, null),
            state -> state.save(new CompoundTag(), null)
    );

    public static final SavedDataType<ModState> TYPE = new SavedDataType<>(
            Identifier.parse("r3ct_collection_data"),
            ModState::new,
            CODEC,
            DataFixTypes.LEVEL
    );
}