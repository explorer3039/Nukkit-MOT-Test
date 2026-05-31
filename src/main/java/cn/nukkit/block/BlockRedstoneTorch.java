package cn.nukkit.block;

import cn.nukkit.Player;
import cn.nukkit.event.redstone.RedstoneUpdateEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.math.BlockFace;
import cn.nukkit.utils.BlockColor;
import cn.nukkit.utils.Faceable;
import cn.nukkit.utils.RedstoneComponent;
import cn.nukkit.network.protocol.LevelSoundEventPacket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Angelic47
 * Nukkit Project
 */
public class BlockRedstoneTorch extends BlockTorch implements RedstoneComponent, Faceable {
    static final int MAX_TOGGLE_COUNT = 8;
    static final int TOGGLE_WINDOW_TICKS = 60;
    static final int BURNOUT_COOLDOWN_TICKS = 160;
    private static final Map<String, BurnoutState> BURNOUT_STATES = new ConcurrentHashMap<>();

    public BlockRedstoneTorch() {
        this(0);
    }

    public BlockRedstoneTorch(int meta) {
        super(meta);
    }

    @Override
    public String getName() {
        return "Redstone Torch";
    }

    @Override
    public int getId() {
        return REDSTONE_TORCH;
    }

    @Override
    public int getLightLevel() {
        return 7;
    }

    @Override
    public boolean place(@NotNull Item item, @NotNull Block block, @NotNull Block target, @NotNull BlockFace face, double fx, double fy, double fz, Player player) {
        if (!super.place(item, block, target, face, fx, fy, fz, player)) {
            return false;
        }

        if (!checkState()) {
            updateAllAroundRedstone(getBlockFace().getOpposite());
        }

        return true;
    }

    @Override
    public int getWeakPower(BlockFace side) {
        return getBlockFace() != side ? 15 : 0;
    }

    @Override
    public int getStrongPower(BlockFace side) {
        return side == BlockFace.DOWN ? this.getWeakPower(side) : 0;
    }

    @Override
    public boolean onBreak(Item item) {
        super.onBreak(item);

        BlockFace face = getBlockFace().getOpposite();
        updateAllAroundRedstone(face);
        return true;
    }

    @Override
    public int onUpdate(int type) {
        if (super.onUpdate(type) == 0) {
            if (type == Level.BLOCK_UPDATE_NORMAL || type == Level.BLOCK_UPDATE_REDSTONE) {
                this.level.scheduleUpdate(this, tickRate());
            } else if (type == Level.BLOCK_UPDATE_SCHEDULED) {
                RedstoneUpdateEvent ev = new RedstoneUpdateEvent(this);
                getLevel().getServer().getPluginManager().callEvent(ev);

                if (ev.isCancelled()) {
                    return 0;
                }

                if (checkState()) {
                    return 1;
                }
            }
        }

        return 0;
    }

    protected boolean checkState() {
        if (isPoweredFromSide()) {
            BlockFace face = getBlockFace().getOpposite();
            this.level.setBlock(this, Block.get(UNLIT_REDSTONE_TORCH, getDamage()), false, true);
            updateAllAroundRedstone(face);
            if (setBurnedOut(this, true)) {
                this.level.addLevelSoundEvent(this, LevelSoundEventPacket.SOUND_FIZZ);
                this.level.scheduleUpdate(this, BURNOUT_COOLDOWN_TICKS);
            }

            return true;
        }

        return false;
    }

    protected boolean isPoweredFromSide() {
        BlockFace face = getBlockFace().getOpposite();
        return this.level.isSidePowered(this.getSide(face), face);
    }

    static boolean isBurnedOut(Block block) {
        return setBurnedOut(block, false);
    }

    static boolean setBurnedOut(Block block, boolean addToggle) {
        if (block.level == null || block.level.getServer() == null) {
            return false;
        }

        int currentTick = block.level.getServer().getTick();
        String key = getBurnoutKey(block);
        BurnoutState state = BURNOUT_STATES.computeIfAbsent(key, ignored -> new BurnoutState());
        state.prune(currentTick);

        if (addToggle) {
            state.toggleTicks.addLast(currentTick);
        }

        boolean burnedOut = state.toggleTicks.size() >= MAX_TOGGLE_COUNT;
        if (!burnedOut && state.toggleTicks.isEmpty()) {
            BURNOUT_STATES.remove(key, state);
        }
        return burnedOut;
    }

    private static String getBurnoutKey(Block block) {
        return block.level.getId() + ":" + block.getFloorX() + ":" + block.getFloorY() + ":" + block.getFloorZ();
    }

    private static final class BurnoutState {
        private final Deque<Integer> toggleTicks = new ArrayDeque<>();

        private void prune(int currentTick) {
            while (!toggleTicks.isEmpty() && currentTick - toggleTicks.peekFirst() > TOGGLE_WINDOW_TICKS) {
                toggleTicks.removeFirst();
            }
        }
    }

    @Override
    public int tickRate() {
        return 2;
    }

    @Override
    public boolean isPowerSource() {
        return true;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.AIR_BLOCK_COLOR;
    }
}
