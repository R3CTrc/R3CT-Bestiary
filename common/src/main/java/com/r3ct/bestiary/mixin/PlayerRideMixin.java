package com.r3ct.bestiary.mixin;

import com.r3ct.bestiary.logic.MobProgressHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class PlayerRideMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void r3ct_onPlayerTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        MobProgressHandler.handlePlayerRidingTick(player);
        MobProgressHandler.handlePlayerDolphinSwimTick(player);
    }
}