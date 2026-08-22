package dev.budao.sortscroller.config;

import com.google.common.collect.ImmutableList;
import dev.budao.sortscroller.utils.SortingMethod;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigStringList;

import java.util.List;

public class SortConfigs {

    private static final ImmutableList<String> DEFAULT_TOP_SORTING = ImmutableList.of("minecraft:diamond_sword","minecraft:diamond_pickaxe","minecraft:diamond_axe","minecraft:diamond_shovel","minecraft:diamond_hoe","minecraft:netherite_sword","minecraft:netherite_pickaxe","minecraft:netherite_axe","minecraft:netherite_shovel","minecraft:netherite_hoe");
    private static final ImmutableList<String> DEFAULT_BOTTOM_SORTING = ImmutableList.of();

    public static final ConfigBoolean SORT_INVENTORY_TOGGLE                 = new ConfigBoolean("sortInventoryToggle",                     false, "itemscroller.config.generic.comment.sortInventoryToggle");
    public static final ConfigBoolean SORT_ASSUME_EMPTY_BOX_STACKS          = new ConfigBoolean("sortAssumeEmptyBoxStacks",                false, "itemscroller.config.generic.comment.sortAssumeEmptyBoxStacks");
    public static final ConfigBoolean SORT_SHULKER_BOXES_AT_END             = new ConfigBoolean("sortShulkerBoxesAtEnd",                   true, "itemscroller.config.generic.comment.sortShulkerBoxesAtEnd");
    public static final ConfigBoolean SORT_SHULKER_BOXES_INVERTED           = new ConfigBoolean("sortShulkerBoxesInverted",                false, "itemscroller.config.generic.comment.sortShulkerBoxesInverted");
    public static final ConfigStringList SORT_TOP_PRIORITY_INVENTORY        = new ConfigStringList("sortTopPriorityInventory",              DEFAULT_TOP_SORTING, "itemscroller.config.generic.comment.sortTopPriorityInventory");
    public static final ConfigStringList SORT_BOTTOM_PRIORITY_INVENTORY     = new ConfigStringList("sortBottomPriorityInventory",           DEFAULT_BOTTOM_SORTING, "itemscroller.config.generic.comment.sortBottomPriorityInventory");
    public static final ConfigOptionList SORT_METHOD_DEFAULT                = new ConfigOptionList("sortMethodDefault",                     SortingMethod.CATEGORY_NAME, "itemscroller.config.generic.comment.sortMethodDefault");

    public static final List<IConfigBase> OPTIONS = ImmutableList.of(
            SORT_INVENTORY_TOGGLE,
            SORT_ASSUME_EMPTY_BOX_STACKS,
            SORT_SHULKER_BOXES_AT_END,
            SORT_SHULKER_BOXES_INVERTED,
            SORT_TOP_PRIORITY_INVENTORY,
            SORT_BOTTOM_PRIORITY_INVENTORY,
            SORT_METHOD_DEFAULT
    );
}
