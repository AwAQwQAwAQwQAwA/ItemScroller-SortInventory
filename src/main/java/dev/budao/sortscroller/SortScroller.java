package dev.budao.sortscroller;

import dev.budao.sortscroller.utils.SortingCategory;
import dev.budao.sortscroller.utils.SortingMethod;
import fi.dy.masa.itemscroller.ItemScroller;
import fi.dy.masa.itemscroller.gui.GuiConfigs;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import org.thinkingstudio.mafglib.util.ForgePlatformUtils;

@Mod(Reference.MOD_ID)
public class SortScroller {
    public SortScroller() {
        if (FMLLoader.getDist().isClient()) {
            // Make sure the mod being absent on the other network side does not cause
            // the client to display the server as incompatible
            ForgePlatformUtils.getInstance().getClientModIgnoredServerOnly();
            SortingCategory.INSTANCE.buildItemGroupList();

            // Config Screen
            ForgePlatformUtils.getInstance().registerModConfigScreen(Reference.MOD_ID, (screen) -> {
                GuiConfigs gui = new GuiConfigs();
                gui.setParent(screen);
                return gui;
            });
        }
    }
}
