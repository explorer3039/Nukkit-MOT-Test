package cn.nukkit.block;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.item.EntityMinecartAbstract;
import cn.nukkit.inventory.ContainerInventory;
import cn.nukkit.inventory.InventoryHolder;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemBlock;
import cn.nukkit.level.Level;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.utils.RedstoneComponent;

/**
 * Created on 2015/11/22 by CreeperFace.
 * Contributed by: larryTheCoder on 2017/7/8.
 * 
 * Nukkit Project,
 * Minecart and Riding Project,
 * Package cn.nukkit.block in project Nukkit.
 */
public class BlockRailDetector extends BlockRail implements RedstoneComponent {

    public BlockRailDetector() {
        this(0);
        canBePowered = true;
    }

    public BlockRailDetector(int meta) {
        super(meta);
    }

    @Override
    public int getId() {
        return DETECTOR_RAIL;
    }

    @Override
    public String getName() {
        return "Detector Rail";
    }

    @Override
    public boolean isPowerSource() {
        return true;
    }

    @Override
    public int getWeakPower(BlockFace side) {
        return isActive() ? 15 : 0;
    }

    @Override
    public int getStrongPower(BlockFace side) {
        return isActive() ? (side == BlockFace.UP ? 15 : 0) : 0;
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride() {
        EntityMinecartAbstract minecart = findMinecart();
        return minecart instanceof InventoryHolder ? ContainerInventory.calculateRedstone(((InventoryHolder) minecart).getInventory()) : 0;
    }

    @Override
    public int onUpdate(int type) {
        if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            updateState();
            return type;
        }
        return super.onUpdate(type);
    }

    @Override
    public boolean hasEntityCollision() {
        return true;
    }

    @Override
    public void onEntityCollide(Entity entity) {
        updateState();
    }

    protected void updateState() {
        boolean wasPowered = isActive();
        boolean isPowered = findMinecart() != null;

        if (isPowered && !wasPowered) {
            setActive(true);
            level.scheduleUpdate(this, this, 0);
            level.scheduleUpdate(this, this.down(), 0);
        }

        if (!isPowered && wasPowered) {
            setActive(false);
            level.scheduleUpdate(this, this, 0);
            level.scheduleUpdate(this, this.down(), 0);
        }

        if (isPowered != wasPowered) {
            updateAroundRedstone();
            RedstoneComponent.updateAroundRedstone(this.getSide(BlockFace.DOWN));
        }

        if (isPowered) {
            level.scheduleUpdate(this, 20);
            level.updateComparatorOutputLevel(this);
        }
    }

    public EntityMinecartAbstract findMinecart() {
        for (Entity entity : level.getNearbyEntities(new SimpleAxisAlignedBB(
                getFloorX() + 0.2,
                getFloorY(),
                getFloorZ() + 0.2,
                getFloorX() + 0.8,
                getFloorY() + 0.8,
                getFloorZ() + 0.8))) {
            if (entity instanceof EntityMinecartAbstract) {
                return (EntityMinecartAbstract) entity;
            }
        }
        return null;
    }

    @Override
    public Item toItem() {
        return new ItemBlock(this, 0);
    }

    @Override
    public Item[] getDrops(Item item) {
        return new Item[]{
                Item.get(Item.DETECTOR_RAIL, 0, 1)
        };
    }
}
