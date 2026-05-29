package cn.nukkit.block;

import cn.nukkit.Player;
import cn.nukkit.event.block.BlockRedstoneEvent;
import cn.nukkit.event.block.DoorToggleEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.item.ItemTool;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.level.Sound;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.utils.Faceable;
import cn.nukkit.utils.RedstoneComponent;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author MagicDroidX
 * Nukkit Project
 */
public abstract class BlockDoor extends BlockTransparentMeta implements RedstoneComponent, Faceable {

    public static final int DOOR_DIRECTION_BIT = 0x03;
    public static final int DOOR_OPEN_BIT = 0x04;
    public static final int DOOR_TOP_BIT = 0x08;
    public static final int DOOR_HINGE_BIT = 0x10;

    @Deprecated
    public static final int DOOR_POWERED_BIT = 0x02;

    private static final int[] faces = {1, 2, 3, 0};
    private static final Set<Location> MANUAL_OVERRIDES = Sets.newConcurrentHashSet();

    protected BlockDoor(int meta) {
        super(meta);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public WaterloggingType getWaterloggingType() {
        return WaterloggingType.WHEN_PLACED_IN_WATER;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public boolean breaksWhenMoved() {
        return true;
    }

    @Override
    public boolean sticksToPiston() {
        return false;
    }

    private int getFullDamage() {
        int up;
        int down;
        if (isTop()) {
            down = this.down().getDamage();
            up = this.getDamage();
        } else {
            down = this.getDamage();
            up = this.up().getDamage();
        }

        return down & DOOR_DIRECTION_BIT | (isTop() ? DOOR_TOP_BIT : 0) | (this.isRightHinged() ? DOOR_HINGE_BIT : 0);
    }

    @Override
    protected AxisAlignedBB recalculateBoundingBox() {

        double f = 0.1875;
        int damage = this.getFullDamage();

        AxisAlignedBB bb = new SimpleAxisAlignedBB(
                this.x,
                this.y,
                this.z,
                this.x + 1,
                this.y + 2,
                this.z + 1
        );

        int j = damage & DOOR_DIRECTION_BIT;
        boolean isOpen = ((damage & DOOR_OPEN_BIT) > 0);
        boolean isRight = this.isRightHinged();

        if (j == 0) {
            if (isOpen) {
                if (!isRight) {
                    bb.setBounds(
                            this.x,
                            this.y,
                            this.z,
                            this.x + 1,
                            this.y + 1,
                            this.z + f
                    );
                } else {
                    bb.setBounds(
                            this.x,
                            this.y,
                            this.z + 1 - f,
                            this.x + 1,
                            this.y + 1,
                            this.z + 1
                    );
                }
            } else {
                bb.setBounds(
                        this.x,
                        this.y,
                        this.z,
                        this.x + f,
                        this.y + 1,
                        this.z + 1
                );
            }
        } else if (j == 1) {
            if (isOpen) {
                if (!isRight) {
                    bb.setBounds(
                            this.x + 1 - f,
                            this.y,
                            this.z,
                            this.x + 1,
                            this.y + 1,
                            this.z + 1
                    );
                } else {
                    bb.setBounds(
                            this.x,
                            this.y,
                            this.z,
                            this.x + f,
                            this.y + 1,
                            this.z + 1
                    );
                }
            } else {
                bb.setBounds(
                        this.x,
                        this.y,
                        this.z,
                        this.x + 1,
                        this.y + 1,
                        this.z + f
                );
            }
        } else if (j == 2) {
            if (isOpen) {
                if (!isRight) {
                    bb.setBounds(
                            this.x,
                            this.y,
                            this.z + 1 - f,
                            this.x + 1,
                            this.y + 1,
                            this.z + 1
                    );
                } else {
                    bb.setBounds(
                            this.x,
                            this.y,
                            this.z,
                            this.x + 1,
                            this.y + 1,
                            this.z + f
                    );
                }
            } else {
                bb.setBounds(
                        this.x + 1 - f,
                        this.y,
                        this.z,
                        this.x + 1,
                        this.y + 1,
                        this.z + 1
                );
            }
        } else if (j == 3) {
            if (isOpen) {
                if (!isRight) {
                    bb.setBounds(
                            this.x,
                            this.y,
                            this.z,
                            this.x + f,
                            this.y + 1,
                            this.z + 1
                    );
                } else {
                    bb.setBounds(
                            this.x + 1 - f,
                            this.y,
                            this.z,
                            this.x + 1,
                            this.y + 1,
                            this.z + 1
                    );
                }
            } else {
                bb.setBounds(
                        this.x,
                        this.y,
                        this.z + 1 - f,
                        this.x + 1,
                        this.y + 1,
                        this.z + 1
                );
            }
        }

        return bb;
    }

    @Override
    public int onUpdate(int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (this.down().getId() == AIR) {
                Block up = this.up();

                if (up instanceof BlockDoor) {
                    this.getLevel().setBlock(up, Block.get(BlockID.AIR), false);
                    this.getLevel().useBreakOn(this, getToolType() == ItemTool.TYPE_PICKAXE ? Item.get(ItemID.DIAMOND_PICKAXE) : Item.get(Item.WOODEN_PICKAXE)); // Drop iron doors
                }

                return Level.BLOCK_UPDATE_NORMAL;
            }
        }

        if (type == Level.BLOCK_UPDATE_REDSTONE) {
            boolean powered = this.isGettingPower();
            if (this.isOpen() != powered && !this.getManualOverride()) {
                this.level.getServer().getPluginManager().callEvent(new BlockRedstoneEvent(this, isOpen() ? 15 : 0, isOpen() ? 0 : 15));
                this.setOpen(null, powered);
            } else if (this.getManualOverride() && this.isOpen() == powered) {
                this.setManualOverride(false);
            }
        }

        return 0;
    }

    public boolean isGettingPower() {
        Location down;
        Location up;
        if (this.isTop()) {
            down = down().getLocation();
            up = getLocation();
        } else {
            down = getLocation();
            up = up().getLocation();
        }

        for (BlockFace side : BlockFace.values()) {
            Block blockDown = down.getSide(side).getLevelBlock();
            Block blockUp = up.getSide(side).getLevelBlock();

            if (this.level.isSidePowered(blockDown.getLocation(), side)
                    || this.level.isSidePowered(blockUp.getLocation(), side)) {
                return true;
            }
        }

        return this.level.isBlockPowered(down) || this.level.isBlockPowered(up);
    }

    @Override
    public boolean place(@NotNull Item item, @NotNull Block block, @NotNull Block target, @NotNull BlockFace face, double fx, double fy, double fz, Player player) {
        if (this.y > block.getLevel().getMaxBlockY() - 1) {
            return false;
        }
        if (face == BlockFace.UP) {
            Block blockUp = this.up();
            Block blockDown = this.down();
            if (!blockUp.canBeReplaced() || blockDown.isTransparent()) {
                return false;
            }

            BlockFace playerDirection = player != null ? player.getDirection() : BlockFace.SOUTH;
            int direction = faces[playerDirection.getHorizontalIndex()];

            Block left = this.getSide(playerDirection.rotateYCCW());
            Block right = this.getSide(playerDirection.rotateY());
            int metaUp = DOOR_TOP_BIT;
            if (left.getId() == this.getId() || (!right.isTransparent() && left.isTransparent())) { //Door hinge
                metaUp |= DOOR_HINGE_BIT;
            }

            this.setDamage(direction);
            this.getLevel().setBlock(block, this, true, true); //Bottom
            this.getLevel().setBlock(blockUp, Block.get(this.getId(), metaUp), true); //Top

            if (!this.isOpen() && this.isGettingPower()) {
                this.setOpen(null, true);
                metaUp |= DOOR_POWERED_BIT;
                this.getLevel().setBlockDataAt(blockUp.getFloorX(), blockUp.getFloorY(), blockUp.getFloorZ(), metaUp);
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean onBreak(Item item) {
        this.setManualOverride(false);
        if (isTop(this.getDamage())) {
            Block down = this.down();
            if (down.getId() == this.getId()) {
                this.getLevel().setBlock(down, Block.get(BlockID.AIR), true);
            }
        } else {
            Block up = this.up();
            if (up.getId() == this.getId()) {
                this.getLevel().setBlock(up, Block.get(BlockID.AIR), true);
            }
        }
        this.getLevel().setBlock(this, Block.get(BlockID.AIR), true);

        return true;
    }

    @Override
    public boolean onActivate(Item item) {
        return this.onActivate(item, null);
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        if (player == null) {
            return false;
        }

        Item itemInHand = player.getInventory().getItemInHand();
        if (player.isSneaking() && !(itemInHand.isTool() || itemInHand.isNull())) {
            return false;
        }
        return toggle(player);
    }

    public boolean toggle(Player player) {
        return this.setOpen(player, !this.isOpen());
    }

    public boolean setOpen(Player player, boolean open) {
        if (open == this.isOpen()) {
            return false;
        }

        DoorToggleEvent event = new DoorToggleEvent(this, player);
        this.getLevel().getServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        Block down;
        Block up;
        if (isTop(this.getDamage())) {
            down = this.down();
            up = this;
        } else {
            down = this;
            up = this.up();
        }

        if (up.getId() != down.getId()) {
            return false;
        }

        this.level.setBlockDataAt(down.getFloorX(), down.getFloorY(), down.getFloorZ(), (down.getDamage() & ~DOOR_OPEN_BIT) | (open ? DOOR_OPEN_BIT : 0));
        if (player != null) {
            this.setManualOverride(this.isGettingPower() || open);
        }
        this.playOpenCloseSound();
        return true;
    }

    public void setManualOverride(boolean value) {
        Location down;
        Location up;
        if (this.isTop()) {
            down = this.down().getLocation();
            up = this.getLocation();
        } else {
            down = this.getLocation();
            up = this.up().getLocation();
        }

        if (value) {
            MANUAL_OVERRIDES.add(down);
            MANUAL_OVERRIDES.add(up);
        } else {
            MANUAL_OVERRIDES.remove(down);
            MANUAL_OVERRIDES.remove(up);
        }
    }

    public boolean getManualOverride() {
        Location down;
        Location up;
        if (this.isTop()) {
            down = this.down().getLocation();
            up = this.getLocation();
        } else {
            down = this.getLocation();
            up = this.up().getLocation();
        }

        return MANUAL_OVERRIDES.contains(down) || MANUAL_OVERRIDES.contains(up);
    }

    public void playOpenCloseSound() {
        if (this.isTop() && down() instanceof BlockDoor) {
            if (((BlockDoor) down()).isOpen()) {
                this.playOpenSound();
            } else {
                this.playCloseSound();
            }
        } else if (up() instanceof BlockDoor) {
            if (this.isOpen()) {
                this.playOpenSound();
            } else {
                this.playCloseSound();
            }
        }
    }

    public void playOpenSound() {
        this.level.addSound(this, Sound.RANDOM_DOOR_OPEN);
    }

    public void playCloseSound() {
        this.level.addSound(this, Sound.RANDOM_DOOR_CLOSE);
    }

    public boolean isOpen() {
        if (isTop(this.getDamage())) {
            return (this.down().getDamage() & DOOR_OPEN_BIT) > 0;
        } else {
            return (this.getDamage() & DOOR_OPEN_BIT) > 0;
        }
    }

    public boolean isTop() {
        return isTop(this.getDamage());
    }

    public boolean isTop(int meta) {
        return (meta & DOOR_TOP_BIT) != 0;
    }

    public boolean isRightHinged() {
        if (isTop()) {
            return (this.getDamage() & DOOR_HINGE_BIT) > 0;
        }
        return (this.up().getDamage() & DOOR_HINGE_BIT) > 0;
    }

    @Override
    public BlockFace getBlockFace() {
        return BlockFace.fromHorizontalIndex(this.getDamage() & DOOR_DIRECTION_BIT);
    }
}
