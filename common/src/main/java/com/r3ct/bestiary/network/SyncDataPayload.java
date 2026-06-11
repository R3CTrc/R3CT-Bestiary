package com.r3ct.bestiary.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public record SyncDataPayload(Map<String, Integer> killCounts, List<String> rewardedCategories) implements CustomPacketPayload {
    public static final Type<SyncDataPayload> TYPE = new Type<>(Identifier.parse("r3ct_bestiary:sync_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDataPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), SyncDataPayload::killCounts,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncDataPayload::rewardedCategories,
            SyncDataPayload::new
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}