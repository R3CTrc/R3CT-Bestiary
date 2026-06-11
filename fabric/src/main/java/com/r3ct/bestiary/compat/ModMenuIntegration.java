package com.r3ct.bestiary.compat;

import com.r3ct.bestiary.client.screen.CollectionConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new CollectionConfigScreen(parent);
    }
}