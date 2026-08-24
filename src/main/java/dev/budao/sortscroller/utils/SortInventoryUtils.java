package dev.budao.sortscroller.utils;

import dev.budao.sortscroller.config.SortConfigs;
import fi.dy.masa.itemscroller.ItemScroller;
import fi.dy.masa.itemscroller.util.AccessorUtils;
import fi.dy.masa.itemscroller.util.ItemType;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntIntMutablePair;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.s2c.play.StatisticsS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static fi.dy.masa.itemscroller.util.InventoryUtils.*;

public class SortInventoryUtils {
    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private static Runnable selectedSlotUpdateTask;
    public static boolean assumeEmptyShulkerStacking = false;
    private static List<String> topSortingPriorityList = SortConfigs.SORT_TOP_PRIORITY_INVENTORY.getStrings();
    private static List<String> bottomSortingPriorityList = SortConfigs.SORT_BOTTOM_PRIORITY_INVENTORY.getStrings();
    private static ItemGroup.DisplayContext displayContext;

    public static void sortInventory(HandledScreen<?> gui)
    {
        Pair<Integer, Integer> range = new IntIntMutablePair(Integer.MAX_VALUE, 0);
        Slot focusedSlot = AccessorUtils.getSlotUnderMouse(gui);
        boolean shulkerBoxFix;

        if (focusedSlot == null)
        {
            return;
        }

        //System.out.printf("sort - focusedSlot[%d]: %s\n", focusedSlot.id, focusedSlot.hasStack() ? focusedSlot.getStack().getName().getString() : "<EMPTY>");
        ScreenHandler container = gui.getScreenHandler();
        int limit = container.slots.size();
        int focusedIndex = -1;

        if (gui instanceof CreativeInventoryScreen creative && !creative.isInventoryTabSelected())
        {
            return;
        }
        if (gui instanceof InventoryScreen && (focusedSlot.id < 9 || focusedSlot.id > 44))
        {
            return;
        }

        // Do not try to sort shulkers inside a shulker
        shulkerBoxFix = gui instanceof ShulkerBoxScreen && focusedSlot.id < 27;

        for (int i = 0; i < limit; i++)
        {
            Slot slot = container.slots.get(i);

            //System.out.printf("sort - slot[%d]: %s\n", i, slot.hasStack() ? slot.getStack().getName().getString() : "<EMPTY>");
            if (slot == focusedSlot)
            {
                focusedIndex = i;
            }
            if (slot.inventory == focusedSlot.inventory)
            {
                if (i < range.first())
                {
                    range.first(i);
                }
                if (i >= range.second())
                {
                    range.second(i + 1);
                }
            }
        }

        if (focusedIndex == -1)
        {
            return;
        }

        if (focusedSlot.inventory instanceof PlayerInventory)
        {
            if (range.left() == 5 && range.right() == 46)
            {
                // Creative, PlayerScreenHandler
                if (focusedIndex >= 9 && focusedIndex < 36)
                {
                    range.left(9).right(36);
                }
                else if (focusedIndex >= 36 && focusedIndex < 45)
                {
                    range.left(36).right(45);
                }
            }
            else if (range.right() - range.left() == 36)
            {
                // Normal containers
                if (focusedIndex < range.left() + 27)
                {
                    range.right(range.left() + 27);
                }
                else
                {
                    range.left(range.right() - 9);
                }
            }
        }

        // try to find usable hotbar slot
        int hotbarSlot = 8;
        if ( shulkerBoxFix )
        {
            var playerInventory = MinecraftClient.getInstance().player.getInventory();
            while ( hotbarSlot >= 0 )
            {
                int slot_ix = container.getSlotIndex(playerInventory, hotbarSlot).orElse(-1);
                if ( slot_ix != -1 && !isShulkerBox(container.getSlot(slot_ix).getStack()) )
                {
                    break;
                }

                --hotbarSlot;
            }

            if ( hotbarSlot < 0 )
            {
                ItemScroller.logger.warn("sortInventory(): no usable hotbar slot to sort shulkerbox");
                return;
            }
        }

        final int swapSlot = hotbarSlot;

        //System.out.printf("Sorting [%d, %d] (first, second)\n", range.first(), range.second());
        //System.out.printf("Sorting [%d, %d] (left, right)\n", range.left(), range.right());
        tryClearCursor(gui);
        tryMergeItems(gui, range.left(), range.right() - 1);

        if (SortConfigs.SORT_ASSUME_EMPTY_BOX_STACKS.getBooleanValue())
        {
            ClientStatusC2SPacket packet = new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS);

            MC.getNetworkHandler().sendPacket(packet);
            selectedSlotUpdateTask = () -> trySort(gui, range.first(), range.second(), shulkerBoxFix, swapSlot);
        }
        else
        {
            trySort(gui, range.first(), range.second(), shulkerBoxFix, swapSlot);
        }
    }

    private static void trySort(HandledScreen<?> gui, int start, int end, boolean shulkerBoxFix, int swapSlot)
    {
        try
        {
            quickSort(gui, start, end, shulkerBoxFix, swapSlot);
        }
        catch (Exception err)
        {
            ItemScroller.logger.error("trySort(): failed to sort items", err);
        }
    }

    private static void quickSort(HandledScreen<?> gui, int start, int end, boolean shulkerBoxFix, int swapSlot)
    {
        var ct = end - start;
        var handler = gui.getScreenHandler();

        // make snapshot of contents; give each item a temporary ID.
        // this ID also happens to be its slot index, relative to `start`.
        var snapshot = new ArrayList<>
                (
                        IntStream.range(0, end - start)
                                .mapToObj(ix -> Pair.of(ix, handler.getSlot(start + ix).getStack().copy()))
                                .filter(pair -> !(shulkerBoxFix && isShulkerBox(pair.value())))
                                .toList()
                );
        ct = snapshot.size();

        // because the array might have unsortable holes, build an index from array index to slot index
        int[] slotindex_by_arrayindex = snapshot.stream().mapToInt(pair -> start + pair.key()).toArray();

        // sort pairs
        List<Pair<Integer, ItemStack>> sorted_pairs =
                (
                        snapshot.stream()
                                .sorted(
                                        (left, right) ->
                                                compareStacks(left.value(), right.value())
                                )
                                .toList()
                );

        ItemScroller.logger.debug(String.format
                (
                        "======\nsort\n%s\n\n",
                        IntStream.range(0, ct)
                                .mapToObj(
                                        ix -> String.format(
                                                "%2d: %2d/%-20s  %2d/%-20s",
                                                ix,
                                                snapshot.get(ix).key(), snapshot.get(ix).value().getName().getString(),
                                                sorted_pairs.get(ix).key(),sorted_pairs.get(ix).value().getName().getString()
                                        )
                                )
                                .collect(Collectors.joining("\n"))
                ));

        // build index of an item's final position by its fake ID
        Map<Integer, Integer> finalpos_by_id =
                (
                        IntStream.range(0, ct).boxed()
                                .collect(Collectors.toMap(
                                        ix -> sorted_pairs.get(ix).key(),
                                        ix -> ix
                                ))
                );

        // sort
        int limit = 0, max_limit = 200;
        Pair<Integer,ItemStack> dst, hold = null;

        for (int src_ix = 0; src_ix < ct; ++src_ix)
        {
            // check if item is in correct position
            Pair<Integer,ItemStack> src = snapshot.get(src_ix);
            int src_id = src.key();
            int dst_ix = finalpos_by_id.get(src_id);

            dst = snapshot.get(dst_ix);

            if (src_ix == dst_ix)
            {
                ItemScroller.logger.debug("quickSort(): {} ok", src_ix);
                continue;
            }

            // pick up and hold "src"
            snapshot.set(src_ix, hold);
            hold = src;
            ItemScroller.logger.debug("quickSort(): pick up {}; holding {}", src_ix, hold);
            clickSlot(gui, slotindex_by_arrayindex[src_ix], swapSlot, SlotActionType.SWAP);

            // continually place the held item into its correct place, following the chain to its end
            // todo: we could skip swapping empty slots, but for some reason, this is not reliable. it seems to swap
            //       in an item from the player's hotbar into the container.
            for (limit = 0; limit < max_limit; ++limit)
            {
                snapshot.set(dst_ix, hold);
                hold = dst;
                clickSlot(gui, slotindex_by_arrayindex[dst_ix], swapSlot, SlotActionType.SWAP);

                ItemScroller.logger.debug("quickSort(): ... swap {} {}; holding {}", dst_ix, dst != null ? dst.value() : "null", hold);
                if (hold == null)
                {
                    break;
                }

                dst_ix = finalpos_by_id.get(hold.key());
                dst = snapshot.get(dst_ix);
            }

            if (limit == max_limit)
            {
                ItemScroller.logger.warn("quickSort(): took too long to follow swap chain ??");
            }

        }
        if (hold != null)
        {
            ItemScroller.logger.warn("quickSort(): sorting complete, but still holding {} ??", hold);
        }
    }

    private static int compareStacks(ItemStack stack1, ItemStack stack2)
    {
        stack1 = stack1 != null ? stack1 : ItemStack.EMPTY;
        stack2 = stack2 != null ? stack2 : ItemStack.EMPTY;

        // boxes towards the end of the list
        boolean stack1IsBox = isShulkerBox(stack1);
        boolean stack2IsBox = isShulkerBox(stack2);

        if (SortConfigs.SORT_SHULKER_BOXES_AT_END.getBooleanValue() && stack1IsBox != stack2IsBox)
        {
            return Boolean.compare(stack1IsBox, stack2IsBox);
        }

        // order items according to user-defined top/bottom priority
        // a priority of -1 means that no priority was specified
        int priority1 = getCustomPriority(stack1);
        int priority2 = getCustomPriority(stack2);

        if (priority1 != -1 || priority2 != -1)
        {
            return Integer.compare(priority1, priority2);
        }

        // empty slots last
        boolean stack1IsEmpty = stack1.isEmpty();
        boolean stack2IsEmpty = stack2.isEmpty();

        if (stack1IsEmpty != stack2IsEmpty)
        {
            return Boolean.compare(stack1IsEmpty, stack2IsEmpty);
        }

        if (stack1IsEmpty)
        {
            // both stacks are empty
            return 0;
        }

        // sort by shulker box contents
        if (stack1IsBox && stack2IsBox)
        {
            List<ItemStack> contents1 = getShulkerBoxItems(stack1);
            List<ItemStack> contents2 = getShulkerBoxItems(stack2);
            int flip = (SortConfigs.SORT_SHULKER_BOXES_INVERTED.getBooleanValue() ? -1 : 1);

            return Integer.compare(contents1.size(), contents2.size()) * flip;
        }

        SortingMethod method = (SortingMethod) SortConfigs.SORT_METHOD_DEFAULT.getOptionListValue();

        if (method.equals(SortingMethod.CATEGORY_NAME) ||
                method.equals(SortingMethod.CATEGORY_COUNT) ||
                method.equals(SortingMethod.CATEGORY_RARITY) ||
                method.equals(SortingMethod.CATEGORY_RAWID) &&
                        MC.world != null)
        {
            // Sort by category
            if (displayContext == null)
            {
                displayContext = SortingCategory.INSTANCE.buildDisplayContext(MC);
                // This isn't used here, but it is required to build the list of items,
                // as if we are opening the Creative Inventory Screen.
            }

            Identifier cat1 = SortingCategory.INSTANCE.fromItemStack(stack1);
            Identifier cat2 = SortingCategory.INSTANCE.fromItemStack(stack2);

            if (!cat1.equals(cat2))
            {
                int index1 = SortingCategory.INSTANCE.fromIdentifier(cat1);
                int index2 = SortingCategory.INSTANCE.fromIdentifier(cat2);
                boolean stack1UnspecifiedCategoryPriority = index1 == -1;
                boolean stack2UnspecifiedCategoryPriority = index2 == -1;

                if ( stack1UnspecifiedCategoryPriority != stack2UnspecifiedCategoryPriority)
                {
                    return Boolean.compare(stack1UnspecifiedCategoryPriority, stack2UnspecifiedCategoryPriority);
                }

                return Integer.compare(index1, index2);
            }
        }

        if (stack1.getItem() != stack2.getItem())
        {
            if (method.equals(SortingMethod.CATEGORY_NAME) || method.equals(SortingMethod.ITEM_NAME))
            {
                // Sort by Item Name
                return stack1.getName().getString().compareTo(stack2.getName().getString());
            }
            else if (method.equals(SortingMethod.CATEGORY_COUNT) || method.equals(SortingMethod.ITEM_COUNT))
            {
                // Sort by Item Count
                int result = Integer.compare(stack2.getCount(), stack1.getCount());
                if ( result != 0 )
                {
                    return result;
                }

                return Integer.compare(Registries.ITEM.getRawId(stack1.getItem()), Registries.ITEM.getRawId(stack2.getItem()));
            }
            else if (method.equals(SortingMethod.CATEGORY_RARITY) || method.equals(SortingMethod.ITEM_RARITY))
            {
                // Sort by Item Rarity
                int result = stack1.getRarity().compareTo(stack2.getRarity());
                if ( result != 0 )
                {
                    return result;
                }

                return Integer.compare(Registries.ITEM.getRawId(stack1.getItem()), Registries.ITEM.getRawId(stack2.getItem()));
            }
            else
            {
                // Sort by Item RawID
                return Integer.compare(Registries.ITEM.getRawId(stack1.getItem()), Registries.ITEM.getRawId(stack2.getItem()));
            }
        }
        if (areStacksEqual(stack1, stack2) == false)
        {
            // Sort's Data Components by Hash Code
            if (stack1.hasNbt() && stack2.hasNbt())
                return Integer.compare(stack1.getNbt().hashCode(), stack2.getNbt().hashCode());
        }

        return Integer.compare(stack2.getCount(), stack1.getCount());
    }

    private static int getCustomPriority(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            // No priority for empty stacks
            return -1;
        }

        // Get item ID and name to check against custom priority lists
        String itemID = Registries.ITEM.getId(stack.getItem()).toString();
        String itemName = stack.getName().getString();

        if (itemID.equals(itemName))
        {
            itemName = null;
        }

        // Top priority check
        int idTopPriority = topSortingPriorityList.indexOf(itemID);
        int nameTopPriority = itemName != null ? topSortingPriorityList.indexOf(itemName) : -1;

        // Bottom priority check
        int idBottomPriority = bottomSortingPriorityList.indexOf(itemID);
        int nameBottomPriority = itemName != null ? bottomSortingPriorityList.indexOf(itemName) : -1;

        // Sort at the top: Prefer name priority if it exists
        if (nameTopPriority != -1)
        {
            return -topSortingPriorityList.size() + nameTopPriority - 2;
        }
        if (idTopPriority != -1)
        {
            return -topSortingPriorityList.size() + idTopPriority - 2;
        }

        // Sort at the bottom: Prefer name priority if it exists
        if (nameBottomPriority != -1)
        {
            return bottomSortingPriorityList.size() + nameBottomPriority;
        }
        if (idBottomPriority != -1)
        {
            return bottomSortingPriorityList.size() + idBottomPriority;
        }

        // Default: no specific priority found
        return -1;
    }

    public static boolean onPong(StatisticsS2CPacket packet)
    {
        if (selectedSlotUpdateTask != null)
        {
            selectedSlotUpdateTask.run();
            selectedSlotUpdateTask = null;
            return true;
        }
        return false;
    }

    private static boolean isShulkerBox(ItemStack stack)
    {
        return stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock;
    }

    private static List<ItemStack> getShulkerBoxItems(ItemStack stack)
    {
        List<ItemStack> items = new ArrayList<>();

        if (stack.hasNbt()) {
            NbtCompound nbt = stack.getNbt();

            if (nbt.contains("BlockEntityTag", 10)) {
                NbtCompound blockEntityTag = nbt.getCompound("BlockEntityTag");

                if (blockEntityTag.contains("Items", 9)) {
                    NbtList itemsList = blockEntityTag.getList("Items", 10);

                    for (int i = 0; i < itemsList.size(); i++) {
                        NbtCompound itemNbt = itemsList.getCompound(i);
                        ItemStack item = ItemStack.fromNbt(itemNbt);

                        if (!item.isEmpty())
                            items.add(item);
                    }
                }
            }
        }

        return items;
    }

    private static boolean isEmptyShulkerBox(ItemStack stack)
    {
        if (!isShulkerBox(stack)) return false;

        if (stack.hasNbt()) {
            NbtCompound nbt = stack.getNbt();

            if (nbt.contains("BlockEntityTag", 10)) {
                NbtCompound blockEntityTag = nbt.getCompound("BlockEntityTag");

                if (blockEntityTag.contains("Items", 9)) {
                    NbtList itemsList = blockEntityTag.getList("Items", 10);

                    for (int i = 0; i < itemsList.size(); i++) {
                        NbtCompound itemNbt = itemsList.getCompound(i);
                        if (itemNbt.contains("id", 8))
                            return false;
                    }
                }
            }
        }

        return true;
    }

    public static int stackMaxSize(ItemStack stack, boolean assumeShulkerStacking)
    {
        if (stack.isEmpty())
        {
            return 64;
        }

        if (assumeShulkerStacking && SortConfigs.SORT_ASSUME_EMPTY_BOX_STACKS.getBooleanValue())
        {
            if (isEmptyShulkerBox(stack))
            {
                return 64;
            }
        }

        return stack.getMaxCount();
    }

    /**
     * @return are there still items left in the original slot?
     */
    private static boolean addStackTo(HandledScreen<? extends ScreenHandler> gui, Slot slot, Slot target)
    {
        if (slot == null || target == null)
        {
            return false;
        }

        ItemStack stack = slot.getStack();
        ItemStack targetStack = target.getStack();

        if (stack.isEmpty() || !ItemStack.areItemsEqual(stack, targetStack))
        {
            return !stack.isEmpty();
        }

        if (targetStack.isEmpty())
        {
            clickSlot(gui, slot, slot.id, 0, SlotActionType.PICKUP);
            clickSlot(gui, target, target.id, 0, SlotActionType.PICKUP);
            //System.out.printf("Moved stack from slot %d to slot %d\n", slot.id, target.id);
            //ItemScroller.printDebug("Moved stack from slot {} to slot {}", slot.id, target.id);
            return false;
        }

        int stackSize = stack.getCount();
        int targetSize = targetStack.getCount();
        assumeEmptyShulkerStacking = true;
        int maxSize = stackMaxSize(stack, true);
        //System.out.printf("Merging %s into %s, maxSize: %d\n", stack, targetStack, maxSize);
        //ItemScroller.printDebug("Merging {} into {}, maxSize: {}", stack, targetStack, maxSize);

        if (targetSize >= maxSize)
        {
            return true;
        }

        clickSlot(gui, slot, slot.id, 0, SlotActionType.PICKUP);
        clickSlot(gui, target, target.id, 0, SlotActionType.PICKUP);
        clickSlot(gui, slot, slot.id, 0, SlotActionType.PICKUP);
        assumeEmptyShulkerStacking = false;
        int amount = stackSize + targetSize - maxSize;

        return amount > 0;
    }

    private static void tryMergeItems(HandledScreen<?> gui, int left, int right)
    {
        Map<ItemType, Integer> nonFullStacks = new HashMap<>();

        for (int i = left; i <= right; i++)
        {
            Slot slot = gui.getScreenHandler().getSlot(i);

            if (slot.hasStack())
            {
                ItemStack stack = slot.getStack();

                if (stack.getCount() >= stackMaxSize(stack, true)) {
                    // ignore overstacking items.
                    continue;
                }

                ItemType key = new ItemType(stack);
                int slotNum = nonFullStacks.getOrDefault(key, -1);

                if (slotNum == -1)
                {
                    nonFullStacks.put(key, i);
                }
                else
                {
                    if (addStackTo(gui, slot, gui.getScreenHandler().getSlot(slotNum)))
                    {
                        nonFullStacks.put(key, i);
                    }
                }
            }
        }
    }
}