package com.r3ct.bestiary.network;

import com.r3ct.bestiary.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ConfigSyncPayload(String mobsJson, String rewardsJson) implements CustomPacketPayload {
    public static final Type<ConfigSyncPayload> TYPE = new Type<>(Identifier.parse(Constants.MOD_ID + ":config_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.mobsJson(), 262144);
                buf.writeUtf(payload.rewardsJson(), 262144);
            },
            buf -> new ConfigSyncPayload(buf.readUtf(262144), buf.readUtf(262144))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}