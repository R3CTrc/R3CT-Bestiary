package com.r3ct.bestiary.compat;

import com.r3ct.bestiary.Constants;
import com.r3ct.bestiary.client.screen.BestiaryScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

@JeiPlugin
public class JeiIntegrationNeoForge implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Identifier.parse(Constants.MOD_ID + ":jei_plugin");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {

        registration.addGuiScreenHandler(BestiaryScreen.class, new mezz.jei.api.gui.handlers.IScreenHandler<BestiaryScreen>() {
            @Override
            public mezz.jei.api.gui.handlers.IGuiProperties apply(BestiaryScreen screen) {
                return new mezz.jei.api.gui.handlers.IGuiProperties() {
                    @Override public Class<? extends Screen> screenClass() { return BestiaryScreen.class; }
                    @Override public int guiXSize() { return (int)(260 * screen.calculateEffectiveScale()); }
                    @Override public int guiYSize() { return (int)(260 * screen.calculateEffectiveScale()); }
                    @Override public int guiLeft() {
                        int w = screen.width > 0 ? screen.width : Minecraft.getInstance().getWindow().getGuiScaledWidth();
                        return (w - guiXSize()) / 2;
                    }
                    @Override public int guiTop() {
                        int h = screen.height > 0 ? screen.height : Minecraft.getInstance().getWindow().getGuiScaledHeight();
                        return (h - guiYSize()) / 2;
                    }
                    @Override public int screenWidth() {
                        return screen.width > 0 ? screen.width : Minecraft.getInstance().getWindow().getGuiScaledWidth();
                    }
                    @Override public int screenHeight() {
                        return screen.height > 0 ? screen.height : Minecraft.getInstance().getWindow().getGuiScaledHeight();
                    }
                };
            }
        });

        registration.addGlobalGuiHandler(new mezz.jei.api.gui.handlers.IGlobalGuiHandler() {
            @Override
            public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory builder, double mouseX, double mouseY) {
                if (Minecraft.getInstance().screen instanceof BestiaryScreen bestiary) {
                    ItemStack hovered = bestiary.getHoveredEgg(mouseX, mouseY);

                    if (hovered != null && !hovered.isEmpty()) {
                        Rect2i area = new Rect2i((int) mouseX - 8, (int) mouseY - 8, 16, 16);
                        return builder.createBuilder(hovered).buildWithArea(area);
                    }
                }
                return Optional.empty();
            }
        });
    }
}