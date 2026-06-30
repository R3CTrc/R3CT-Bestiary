package com.r3ct.bestiary.network;

import com.r3ct.bestiary.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SetTrophyEntityPayload(BlockPos pos, String entityId) implements CustomPacketPayload {
    public static final Type<SetTrophyEntityPayload> TYPE = new Type<>(Identifier.parse(Constants.MOD_ID + ":set_trophy_entity"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetTrophyEntityPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeUtf(payload.entityId(), 32767);
            },
            buf -> new SetTrophyEntityPayload(buf.readBlockPos(), buf.readUtf(32767))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}