package dev.budao.sortscroller.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import fi.dy.masa.itemscroller.ItemScroller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.CreativeModeTabRegistry;

public class SortingCategory
{
    public static final SortingCategory INSTANCE = new SortingCategory();
    public ImmutableList<ItemGroup> values = ImmutableList.of();

    public void buildItemGroupList() {
        values = CreativeModeTabRegistry.getSortedCreativeModeTabs()
                .stream()
                .filter(itemGroup -> itemGroup.getType() == ItemGroup.Type.CATEGORY)
                .collect(ImmutableList.toImmutableList());
    }

    @Nullable
    public ItemGroup.DisplayContext buildDisplayContext(MinecraftClient mc)
    {
        if (mc.world == null)
        {
            return null;
        }

        ItemGroup.DisplayContext ctx = new ItemGroup.DisplayContext(mc.world.getEnabledFeatures(), true, mc.world.getRegistryManager());

        Registries.ITEM_GROUP.stream().filter((group) ->
                group.getType() == ItemGroup.Type.CATEGORY).forEach((group) ->
                group.updateEntries(ctx));

        if (!(values != null && !values.isEmpty()))
            buildItemGroupList();

        return ctx;
    }

    public Identifier fromItemStack(ItemStack stack)
    {
        for (int i = 0; i < Registries.ITEM_GROUP.size(); i++)
        {
            ItemGroup itemGroup = Registries.ITEM_GROUP.get(i);

            if (itemGroup != null && itemGroup.getType().equals(ItemGroup.Type.CATEGORY))
            {
                Collection<ItemStack> stacks;
                Iterator<ItemStack> iter;

                if (itemGroup.hasStacks())
                {
                    stacks = itemGroup.getDisplayStacks();
                    iter = stacks.iterator();

                    while (iter.hasNext())
                    {
                        if (ItemStack.areItemsEqual(iter.next(), stack))
                        {
                            return Registries.ITEM_GROUP.getId(itemGroup);
                        }
                    }

                }

                stacks = itemGroup.getSearchTabStacks();
                iter = stacks.iterator();

                while (iter.hasNext())
                {
                    if (ItemStack.areItemsEqual(iter.next(), stack))
                    {
                        return Registries.ITEM_GROUP.getId(itemGroup);
                    }
                }

            }
        }

        return Registries.ITEM_GROUP.getId(values.get(values.size() - 1));
    }

    @Nullable
    public int fromItemGroup(ItemGroup group)
    {
        Identifier id = Registries.ITEM_GROUP.getId(group);

        if (id != null)
        {
            return values.indexOf(group);
        }

        return -1;
    }

    public int fromIdentifier(Identifier id) {
        return fromItemGroup(Registries.ITEM_GROUP.get(id));
    }
}

