package com.r3ct.bestiary.network;

import com.r3ct.bestiary.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DebugCompleteCategoryPayload(String categoryId) implements CustomPacketPayload {
    public static final Type<DebugCompleteCategoryPayload> TYPE = new Type<>(Identifier.parse(Constants.MOD_ID + ":debug_complete_category"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DebugCompleteCategoryPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.categoryId(), 32767),
            buf -> new DebugCompleteCategoryPayload(buf.readUtf(32767))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}