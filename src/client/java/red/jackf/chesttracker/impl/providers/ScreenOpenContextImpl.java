package red.jackf.chesttracker.impl.providers;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import red.jackf.chesttracker.api.ClientBlockSource;
import red.jackf.chesttracker.api.providers.MemoryLocation;
import red.jackf.chesttracker.api.providers.context.ScreenOpenContext;

import java.util.Optional;

public final class ScreenOpenContextImpl implements ScreenOpenContext {
    private final AbstractContainerScreen<?> screen;
    private @Nullable MemoryLocation target = null;
    private @Nullable ClientBlockSource blockSource = null;

    public ScreenOpenContextImpl(AbstractContainerScreen<?> screen) {
        this.screen = screen;
    }

    @ApiStatus.Internal
    public static ScreenOpenContextImpl createFor(AbstractContainerScreen<?> screen) {
        return new ScreenOpenContextImpl(screen);
    }

    @Override
    public AbstractContainerScreen<?> getScreen() {
        return screen;
    }

    @Override
    public void setMemoryLocation(MemoryLocation memoryLocation) {
        this.target = memoryLocation;
    }

    @Override
    public void setBlockSource(@Nullable ClientBlockSource blockSource) {
        this.blockSource = blockSource;
    }

    public @Nullable MemoryLocation getTarget() {
        return target;
    }

    /**
     * The block this screen was opened from, pinned when the screen opened. Used on close so the memory is attributed to
     * the block the player actually opened, even if the interaction tracker has since moved on.
     */
    public Optional<ClientBlockSource> getBlockSource() {
        return Optional.ofNullable(this.blockSource);
    }
}
