package dev.budao.sortscroller.mixin.itemscroller.config;

import com.google.common.collect.ImmutableList;
import dev.budao.sortscroller.config.SortConfigs;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.malilib.config.IConfigBase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = Configs.Generic.class, remap = false)
public class MixinConfigs$Generic {

    @Mutable
    @Final
    @Shadow
    public static ImmutableList<IConfigBase> OPTIONS;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void staticInit(CallbackInfo ci) {
        List<IConfigBase> options = new ArrayList<>(OPTIONS);
        options.addAll(SortConfigs.OPTIONS);

        OPTIONS = ImmutableList.copyOf(options);
    }
}
