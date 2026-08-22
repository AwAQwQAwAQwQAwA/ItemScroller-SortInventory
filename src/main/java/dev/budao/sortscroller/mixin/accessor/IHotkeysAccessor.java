package dev.budao.sortscroller.mixin.accessor;

import fi.dy.masa.itemscroller.config.Hotkeys;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Hotkeys.class, remap = false)
public interface IHotkeysAccessor {

    @Accessor("GUI_NO_ORDER")
    static KeybindSettings getKeybindSettings() {
        throw new AssertionError();
    }
}
