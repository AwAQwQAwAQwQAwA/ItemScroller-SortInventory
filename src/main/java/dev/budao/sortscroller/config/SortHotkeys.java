package dev.budao.sortscroller.config;

import dev.budao.sortscroller.mixin.accessor.IHotkeysAccessor;
import fi.dy.masa.itemscroller.config.Hotkeys;
import fi.dy.masa.malilib.config.options.ConfigHotkey;

public class SortHotkeys {

    public static final ConfigHotkey SORT_INVENTORY             = new ConfigHotkey("sortInventory",         "R", IHotkeysAccessor.getKeybindSettings(), "itemscroller.config.hotkeys.comment.sortInventory");
}
