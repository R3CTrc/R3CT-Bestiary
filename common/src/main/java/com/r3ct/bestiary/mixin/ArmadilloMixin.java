package com.r3ct.bestiary.mixin;

import com.r3ct.bestiary.config.BestiaryConfig;
import com.r3ct.bestiary.logic.MobProgressHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Armadillo.class)
public abstract class ArmadilloMixin {

    @Unique private boolean r3ct_hadBrush = false;

    // Sprawdzamy, czy gracz trzyma w ręku pędzel (Brush)
    @Inject(method = "mobInteract", at = @At("HEAD"))
    private void r3ct_checkBrush(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        this.r3ct_hadBrush = player.getItemInHand(hand).is(Items.BRUSH);
    }

    // Jeśli akcja się powiodła (pancernik zrzucił łuskę)
    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void r3ct_onBrush(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && player instanceof ServerPlayer serverPlayer && this.r3ct_hadBrush) {

            Armadillo armadillo = (Armadillo) (Object) this;
            long cooldownMs = (long) BestiaryConfig.interactionCooldownMinutes * 60 * 1000L;

            // Nakładamy cooldown korzystając ze wspólnego interfejsu
            if (MobProgressHandler.tryApplyCooldown(armadillo, cooldownMs)) {
                MobProgressHandler.handleMobInteract(serverPlayer, EntityType.ARMADILLO);
            }
        }
    }
}