package com.r3ct.bestiary.mixin;

import com.r3ct.bestiary.logic.MobProgressHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class BucketableMixin {

    @Unique private boolean r3ct_wasWild = false;
    @Unique private boolean r3ct_hadWaterBucket = false;

    @Inject(method = "mobInteract", at = @At("HEAD"))
    private void r3ct_checkBucket(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Object self = this;
        if (self instanceof Bucketable bucketable) {
            this.r3ct_wasWild = !bucketable.fromBucket();
            this.r3ct_hadWaterBucket = player.getItemInHand(hand).is(Items.WATER_BUCKET);
        }
    }

    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void r3ct_onCatch(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Object self = this;
        if (self instanceof Bucketable && cir.getReturnValue().consumesAction() && player instanceof ServerPlayer serverPlayer) {
            if (this.r3ct_hadWaterBucket && this.r3ct_wasWild) {
                MobProgressHandler.handleMobInteract(serverPlayer, ((Mob) self).getType());
            }
        }
    }
}