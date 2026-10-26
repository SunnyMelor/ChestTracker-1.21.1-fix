package red.jackf.chesttracker.impl.compat.mods;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import red.jackf.chesttracker.api.gui.GetCustomName;
import red.jackf.jackfredlib.api.base.ResultHolder;
import red.jackf.whereisit.api.search.ConnectedBlocksGrabber;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Adds connected block support to expanded storage chests.
 *
 * <p>Expanded Storage is accessed reflectively rather than through a compile-time dependency. The mod is no longer
 * published to the Maven repository this project used to pull it from, so a hard dependency would make the project
 * impossible to build from a clean checkout. Reflection keeps the integration working when the mod is present while
 * letting the project build without it.</p>
 */
public class ExpandedStorageIntegration {
    private static final String ACCESSORS_CLASS = "compasses.expandedstorage.api.ExpandedStorageAccessors";
    private static final String SCREEN_CLASS = "compasses.expandedstorage.impl.client.gui.AbstractScreen";

    private static Method getAttachedChestDirection;
    private static Class<?> abstractScreenClass;

    public static void setup() {
        resolve();

        // should probably be in where is it
        ConnectedBlocksGrabber.EVENT.register((positions, pos, level, state) -> {
            getAttachedChestDirection(state).ifPresent(direction -> positions.add(pos.relative(direction)));
        });

        GetCustomName.EVENT.register(screen -> {
            if (abstractScreenClass != null && abstractScreenClass.isInstance(screen)) {
                return ResultHolder.empty();
            }
            return ResultHolder.pass();
        });
    }

    private static void resolve() {
        try {
            Class<?> accessors = Class.forName(ACCESSORS_CLASS);
            getAttachedChestDirection = accessors.getMethod("getAttachedChestDirection", BlockState.class);
        } catch (ReflectiveOperationException e) {
            getAttachedChestDirection = null;
        }

        try {
            abstractScreenClass = Class.forName(SCREEN_CLASS);
        } catch (ClassNotFoundException e) {
            abstractScreenClass = null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Optional<Direction> getAttachedChestDirection(BlockState state) {
        if (getAttachedChestDirection == null) return Optional.empty();
        try {
            return (Optional<Direction>) getAttachedChestDirection.invoke(null, state);
        } catch (ReflectiveOperationException e) {
            return Optional.empty();
        }
    }
}
