package dev.budao.sortscroller.mixin.itemscroller.config;

import com.google.common.collect.ImmutableList;
import dev.budao.sortscroller.config.SortHotkeys;
import dev.budao.sortscroller.mixin.accessor.IHotkeysAccessor;
import fi.dy.masa.itemscroller.config.Hotkeys;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = Hotkeys.class, remap = false)
@Implements(@Interface(iface = IHotkeysAccessor.class, prefix = "sortscroller$"))
public class MixinHotkeys {

    @Mutable
    @Final
    @Shadow
    public static List<ConfigHotkey> HOTKEY_LIST;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void staticInit(CallbackInfo ci) {
        List<ConfigHotkey> hotkeys = new ArrayList<>(HOTKEY_LIST);
        hotkeys.add(SortHotkeys.SORT_INVENTORY);

        HOTKEY_LIST = ImmutableList.copyOf(hotkeys);
    }
}
