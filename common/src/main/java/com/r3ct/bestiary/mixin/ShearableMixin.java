package com.r3ct.bestiary.mixin;

import com.r3ct.bestiary.logic.MobProgressHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class ShearableMixin {

    @Inject(method = "interact", at = @At("HEAD"))
    private void r3ct_onShear(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        Object self = this;
        if (!((Mob) self).level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (self instanceof Shearable shearable && shearable.readyForShearing()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.is(Items.SHEARS)) {
                    MobProgressHandler.handleMobShear(serverPlayer, ((Mob) self).getType());
                }
            }
        }
    }
}