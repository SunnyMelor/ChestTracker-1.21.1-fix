package red.jackf.chesttracker.impl.providers;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.jetbrains.annotations.Nullable;
import red.jackf.chesttracker.api.ClientBlockSource;
import red.jackf.chesttracker.api.providers.InteractionTracker;
import red.jackf.chesttracker.impl.util.CachedClientBlockSource;

import java.util.Optional;

public class InteractionTrackerImpl implements InteractionTracker {
    public static final InteractionTrackerImpl INSTANCE = new InteractionTrackerImpl();

    private @Nullable ClientBlockSource lastSource = null;

    /**
     * Position of the last block source that was consumed by a screen that actually opened, so that the source can be
     * cleared once that screen closes without losing a still-pending interaction.
     */
    private @Nullable BlockPos pendingClearPos = null;

    public static void setup() {
        // Event Setup
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (hand == InteractionHand.MAIN_HAND && level instanceof ClientLevel clientLevel) {
                BlockPos pos = hitResult.getBlockPos();
                // Only blocks that can hold a block entity can open a container GUI. Recording workstations like crafting
                // tables here is what lets a later, unrelated GUI be filed under them.
                if (clientLevel.getBlockState(pos).hasBlockEntity())
                    INSTANCE.setLastBlockSource(new CachedClientBlockSource(clientLevel, pos));
                else
                    INSTANCE.clear();
            }
            return InteractionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            INSTANCE.clear();
            return InteractionResult.PASS;
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> INSTANCE.clear());
    }

    @Override
    public Optional<ClientLevel> getPlayerLevel() {
        if (Minecraft.getInstance().level == null) return Optional.empty();
        return Optional.of(Minecraft.getInstance().level);
    }

    @Override
    public Optional<ClientBlockSource> getLastBlockSource() {
        return Optional.ofNullable(lastSource);
    }

    public void clear() {
        this.lastSource = null;
        this.pendingClearPos = null;
    }

    public void setLastBlockSource(ClientBlockSource source) {
        this.lastSource = source;
    }

    /**
     * Marks the current block source as consumed by an opened screen. A stale source is the main reason memories end up
     * attributed to the wrong container: the server can open a GUI without a block interaction (or a mod can interact
     * with a nearby block), and reusing the old source then files those contents under the previously clicked block.
     */
    public void markConsumed() {
        this.pendingClearPos = this.lastSource == null ? null : this.lastSource.pos();
    }

    /**
     * Clears the block source if it is still the one consumed by a screen that has now closed. Called after every screen
     * close so a source can never outlive the GUI it was used for.
     */
    public void clearConsumed() {
        if (this.pendingClearPos != null && this.lastSource != null && this.pendingClearPos.equals(this.lastSource.pos()))
            this.lastSource = null;
        this.pendingClearPos = null;
    }
}
