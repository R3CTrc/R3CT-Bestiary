package com.r3ct.bestiary.mixin;

import com.r3ct.bestiary.config.BestiaryConfig;
import com.r3ct.bestiary.logic.MobProgressHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Dolphin.class)
public abstract class DolphinMixin {

    @Unique private boolean r3ct_hadFish = false;

    @Inject(method = "mobInteract", at = @At("HEAD"))
    private void r3ct_checkFish(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack item = player.getItemInHand(hand);
        this.r3ct_hadFish = item.is(Items.COD) || item.is(Items.SALMON);
    }

    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void r3ct_onFeed(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && player instanceof ServerPlayer serverPlayer && this.r3ct_hadFish) {

            Dolphin dolphin = (Dolphin) (Object) this;
            long cooldownMs = (long) BestiaryConfig.interactionCooldownMinutes * 60 * 1000L;

            if (MobProgressHandler.tryApplyCooldown(dolphin, cooldownMs)) {
                MobProgressHandler.handleMobInteract(serverPlayer, EntityType.DOLPHIN);
            }
        }
    }
}