package com.r3ct.bestiary.mixin;

import com.r3ct.bestiary.util.IResearchCooldown;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobCooldownMixin implements IResearchCooldown {

    @Unique
    private long r3ct_lastResearchTime = 0;

    @Override
    public long r3ct_getLastResearchTime() {
        return this.r3ct_lastResearchTime;
    }

    @Override
    public void r3ct_setLastResearchTime(long time) {
        this.r3ct_lastResearchTime = time;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void r3ct_saveData(ValueOutput tag, CallbackInfo ci) {
        tag.putLong("R3ctResearchTime", this.r3ct_lastResearchTime);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void r3ct_readData(ValueInput tag, CallbackInfo ci) {
        this.r3ct_lastResearchTime = tag.getLongOr("R3ctResearchTime", 0L);
    }
}