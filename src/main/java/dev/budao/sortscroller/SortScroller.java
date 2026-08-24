package dev.budao.sortscroller;

import dev.budao.sortscroller.utils.SortingCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class SortScroller implements ModInitializer {
    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            SortingCategory.INSTANCE.buildItemGroupList();
        }
    }
}
