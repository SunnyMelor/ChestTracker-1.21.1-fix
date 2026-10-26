package red.jackf.chesttracker.impl.util;

import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <p>Knows how many container slots a vanilla block exposes, so that a closing GUI can be checked against the block
 * that Chest Tracker believes it came from.</p>
 *
 * <p>This exists because the last interacted block and the GUI the server actually opened can disagree - another mod may
 * interact with a nearby container, or a GUI can be opened without any block interaction at all. Without this check the
 * contents of that GUI would be filed under the wrong container, which makes searches point at the wrong block.</p>
 */
public class ContainerSizes {
    /**
     * Vanilla container sizes, in slots.
     */
    public static final int SINGLE_CHEST = 27;
    public static final int DOUBLE_CHEST = 54;
    public static final int BARREL = 27;
    public static final int SHULKER_BOX = 27;
    public static final int HOPPER = 5;
    public static final int DISPENSER = 9;
    public static final int CRAFTER = 9;
    public static final int FURNACE = 3;
    public static final int BREWING_STAND = 5;

    private ContainerSizes() {
    }

    /**
     * Checks whether a GUI with the given number of container slots could belong to the given block.
     *
     * <p>Unknown blocks (most modded containers) are always accepted, so this never blocks mod compatibility. A chest
     * accepts both sizes because the other half of a double chest may be outside the loaded area, which makes the two
     * indistinguishable near chunk borders.</p>
     *
     * @param state       Block state of the container the GUI is being attributed to.
     * @param actualSlots Number of non-player slots in the closing GUI.
     * @return <code>true</code> if the GUI plausibly belongs to this block.
     */
    public static boolean isPlausible(BlockState state, int actualSlots) {
        if (state.getBlock() instanceof ChestBlock)
            return actualSlots == SINGLE_CHEST || actualSlots == DOUBLE_CHEST;
        if (state.getBlock() instanceof BarrelBlock) return actualSlots == BARREL;
        if (state.getBlock() instanceof ShulkerBoxBlock) return actualSlots == SHULKER_BOX;
        if (state.getBlock() instanceof HopperBlock) return actualSlots == HOPPER;
        if (state.getBlock() instanceof DispenserBlock) return actualSlots == DISPENSER;
        if (state.getBlock() instanceof CrafterBlock) return actualSlots == CRAFTER;
        if (state.getBlock() instanceof AbstractFurnaceBlock) return actualSlots == FURNACE;
        if (state.getBlock() instanceof BrewingStandBlock) return actualSlots == BREWING_STAND;

        return true;
    }
}
