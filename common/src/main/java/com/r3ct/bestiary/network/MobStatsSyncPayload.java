package com.r3ct.bestiary.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public record MobStatsSyncPayload(Map<String, MobBaseStats> statsMap) implements CustomPacketPayload {
    public static final Type<MobStatsSyncPayload> TYPE = new Type<>(Identifier.parse("r3ct_bestiary:mob_stats_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobStatsSyncPayload> STREAM_CODEC =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, MobBaseStats.STREAM_CODEC)
                    // ZMIANA: Bezpieczne pakowanie z powrotem do HashMap dla systemu serializacji
                    .map(MobStatsSyncPayload::new, payload -> new HashMap<>(payload.statsMap()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}