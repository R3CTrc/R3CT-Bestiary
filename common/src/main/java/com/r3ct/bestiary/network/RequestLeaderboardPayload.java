package com.r3ct.bestiary.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestLeaderboardPayload() implements CustomPacketPayload {
    public static final Type<RequestLeaderboardPayload> TYPE = new Type<>(Identifier.parse("r3ct_bestiary:req_leaderboard"));

    public static final StreamCodec<ByteBuf, RequestLeaderboardPayload> CODEC = StreamCodec.unit(new RequestLeaderboardPayload());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}