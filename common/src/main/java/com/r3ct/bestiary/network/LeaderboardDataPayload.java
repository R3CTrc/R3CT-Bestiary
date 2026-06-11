package com.r3ct.bestiary.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.List;

public record LeaderboardDataPayload(List<TopPlayerEntry> entries) implements CustomPacketPayload {
    public static final Type<LeaderboardDataPayload> TYPE = new Type<>(Identifier.parse("r3ct_bestiary:sync_leaderboard"));

    public record TopPlayerEntry(String name, int totalCompleted, List<String> unlockedMobs) {}

    public static final StreamCodec<RegistryFriendlyByteBuf, TopPlayerEntry> ENTRY_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TopPlayerEntry::name,
            ByteBufCodecs.VAR_INT, TopPlayerEntry::totalCompleted,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), TopPlayerEntry::unlockedMobs,
            TopPlayerEntry::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, LeaderboardDataPayload> CODEC = StreamCodec.composite(
            ENTRY_CODEC.apply(ByteBufCodecs.list()), LeaderboardDataPayload::entries,
            LeaderboardDataPayload::new
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}