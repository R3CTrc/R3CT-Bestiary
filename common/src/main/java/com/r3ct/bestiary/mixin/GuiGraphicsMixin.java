package com.r3ct.bestiary.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsMixin {

    @Shadow public abstract Matrix3x2fStack pose();
    @Shadow public abstract void fakeItem(ItemStack itemStack, int x, int y);

    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"))
    private void r3ct$renderTrophyMiniature(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {

        if (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("r3ct_bestiary:trophy")) {

            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null && !customData.isEmpty()) {

                CompoundTag tag = customData.copyTag();

                tag.getString("DisplayEntity").ifPresent(entityIdStr -> {
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityIdStr)).map(net.minecraft.core.Holder::value).orElse(null);

                    if (type != null) {
                        ItemStack innerStack = new ItemStack(Items.SPAWNER);

                        java.util.Optional<net.minecraft.core.Holder<net.minecraft.world.item.Item>> eggOptional = SpawnEggItem.byId(type);
                        if (eggOptional.isPresent()) {
                            innerStack = new ItemStack(eggOptional.get().value());
                        }

                        Matrix3x2fStack pose = this.pose();
                        pose.pushMatrix();

                        pose.translate(x + 4.8F, y + 2.0F);
                        pose.scale(0.4F, 0.4F);

                        this.fakeItem(innerStack, 0, 0);

                        pose.popMatrix();
                    }
                });
            }
        }
    }
}