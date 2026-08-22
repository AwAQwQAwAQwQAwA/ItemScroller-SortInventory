package dev.budao.sortscroller.mixin.itemscroller.event;

import dev.budao.sortscroller.config.SortConfigs;
import dev.budao.sortscroller.config.SortHotkeys;
import dev.budao.sortscroller.utils.SortInventoryUtils;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.config.Hotkeys;
import fi.dy.masa.itemscroller.event.KeybindCallbacks;
import fi.dy.masa.itemscroller.util.InventoryUtils;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.util.GuiUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KeybindCallbacks.class, remap = false)
public abstract class MixinKeybindCallbacks {

    @Shadow public abstract boolean functionalityEnabled();

    @Inject(method = "onKeyActionImpl", at = @At("RETURN"), cancellable = true)
    private void onKeyActionImpl(KeyAction action, IKeybind key, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        if (!this.functionalityEnabled() ||
                !(GuiUtils.getCurrentScreen() instanceof HandledScreen) ||
                Configs.GUI_BLACKLIST.contains(GuiUtils.getCurrentScreen().getClass().getName())) return;

        if (!this.functionalityEnabled() ||
                !(GuiUtils.getCurrentScreen() instanceof HandledScreen) ||
                Configs.GUI_BLACKLIST.contains(GuiUtils.getCurrentScreen().getClass().getName())) return;

        if (key == SortHotkeys.SORT_INVENTORY.getKeybind()) {
            if (SortConfigs.SORT_INVENTORY_TOGGLE.getBooleanValue())
            {
                SortInventoryUtils.sortInventory((HandledScreen<?>) GuiUtils.getCurrentScreen());
                cir.setReturnValue(true);
            }
        }

    }
}
