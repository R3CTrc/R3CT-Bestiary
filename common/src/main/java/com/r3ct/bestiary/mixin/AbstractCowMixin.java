package com.r3ct.bestiary.mixin;

import com.r3ct.bestiary.logic.MobProgressHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractCow.class)
public abstract class AbstractCowMixin {

    @Unique private boolean r3ct_hadBucket = false;

    @Inject(method = "mobInteract", at = @At("HEAD"))
    private void r3ct_checkBucket(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        this.r3ct_hadBucket = player.getItemInHand(hand).is(Items.BUCKET);
    }

    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void r3ct_onMilk(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && player instanceof ServerPlayer serverPlayer && this.r3ct_hadBucket) {

            AbstractCow cow = (AbstractCow) (Object) this;

            long cooldownMs = (long) com.r3ct.bestiary.config.BestiaryConfig.interactionCooldownMinutes * 60 * 1000L;
            if (MobProgressHandler.tryApplyCooldown(cow, cooldownMs)) {
                MobProgressHandler.handleMobInteract(serverPlayer, cow.getType());
            }
        }
    }
}