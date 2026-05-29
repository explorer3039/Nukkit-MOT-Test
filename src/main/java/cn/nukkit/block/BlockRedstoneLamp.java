package cn.nukkit.block;

import cn.nukkit.Player;
import cn.nukkit.event.redstone.RedstoneUpdateEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemBlock;
import cn.nukkit.item.ItemTool;
import cn.nukkit.level.Level;
import cn.nukkit.math.BlockFace;
import cn.nukkit.utils.BlockColor;
import cn.nukkit.utils.RedstoneComponent;
import org.jetbrains.annotations.NotNull;

/**
 * @author Nukkit Project Team
 */
public class BlockRedstoneLamp extends BlockSolid implements RedstoneComponent {

    @Override
    public String getName() {
        return "Redstone Lamp";
    }

    @Override
    public int getId() {
        return REDSTONE_LAMP;
    }

    @Override
    public double getHardness() {
        return 0.3D;
    }

    @Override
    public double getResistance() {
        return 1.5D;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public boolean place(@NotNull Item item, @NotNull Block block, @NotNull Block target, @NotNull BlockFace face, double fx, double fy, double fz, Player player) {
        if (this.isGettingPower()) {
            this.level.setBlock(this, Block.get(LIT_REDSTONE_LAMP), false, true);
        } else {
            this.level.setBlock(this, this, false, true);
        }
        return true;
    }

    @Override
    public int onUpdate(int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL || type == Level.BLOCK_UPDATE_REDSTONE) {
            if (this.isGettingPower()) {
                RedstoneUpdateEvent ev = new RedstoneUpdateEvent(this);
                getLevel().getServer().getPluginManager().callEvent(ev);
                if (ev.isCancelled()) {
                    return 0;
                }

                this.level.updateComparatorOutputLevelSelective(this, true);
                this.level.setBlock(this, Block.get(LIT_REDSTONE_LAMP), false, false);
                return 1;
            }
        }

        return 0;
    }

    public boolean isGettingPower() {
        for (BlockFace side : BlockFace.values()) {
            Block block = this.getSide(side);
            if (block == null) {
                continue;
            }
            if (block.getId() == Block.REDSTONE_WIRE && block.getDamage() > 0 && block.y >= this.getY()) {
                return true;
            }

            if (this.level.isSidePowered(block, side)) {
                return true;
            }
        }

        return this.level.isBlockPowered(this.getLocation());
    }

    @Override
    public Item[] getDrops(Item item) {
        return new Item[]{
                new ItemBlock(Block.get(REDSTONE_LAMP))
        };
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.AIR_BLOCK_COLOR;
    }
}
