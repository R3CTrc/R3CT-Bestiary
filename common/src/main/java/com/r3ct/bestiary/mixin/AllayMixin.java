package com.r3ct.bestiary.mixin;

import com.r3ct.bestiary.logic.MobProgressHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Allay.class)
public abstract class AllayMixin {

    @Unique private boolean r3ct_hadItem = false;

    @Inject(method = "mobInteract", at = @At("HEAD"))
    private void r3ct_checkItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        this.r3ct_hadItem = !player.getItemInHand(hand).isEmpty();
    }

    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void r3ct_onGiveItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && player instanceof ServerPlayer serverPlayer && this.r3ct_hadItem) {
            long cooldownMs = (long) com.r3ct.bestiary.config.BestiaryConfig.interactionCooldownMinutes * 60 * 1000L;
            if (MobProgressHandler.tryApplyCooldown((Allay)(Object)this, cooldownMs)) {
                MobProgressHandler.handleMobInteract(serverPlayer, EntityType.ALLAY);
            }
        }
    }
}