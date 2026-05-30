package cn.nukkit.block;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemBlock;
import cn.nukkit.level.Level;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.MathHelper;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.utils.BlockColor;
import cn.nukkit.utils.RedstoneComponent;

/**
 * Created on 2015/11/22 by CreeperFace.
 * Package cn.nukkit.block in project Nukkit .
 */
public class BlockDaylightDetector extends BlockTransparent implements RedstoneComponent {

    @Override
    public int getId() {
        return DAYLIGHT_DETECTOR;
    }

    @Override
    public String getName() {
        return "Daylight Detector";
    }

    @Override
    public double getHardness() {
        return 0.2;
    }

    @Override
    public WaterloggingType getWaterloggingType() {
        return WaterloggingType.WHEN_PLACED_IN_WATER;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.WOOD_BLOCK_COLOR;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        BlockDaylightDetector block = (BlockDaylightDetector) Block.get(DAYLIGHT_DETECTOR_INVERTED);
        this.getLevel().setBlock(this, block, true, true);
        block.updatePower();
        return true;
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, double fx, double fy, double fz, Player player) {
        if (super.place(item, block, target, face, fx, fy, fz, player)) {
            this.updatePower();
            return true;
        }
        return false;
    }

    @Override
    public boolean onBreak(Item item) {
        if (super.onBreak(item)) {
            updateAroundRedstone();
            return true;
        }
        return false;
    }

    @Override
    public int onUpdate(int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL || type == Level.BLOCK_UPDATE_REDSTONE) {
            this.updatePower();
        }
        return type;
    }

    @Override
    public Item toItem() {
        return new ItemBlock(this, 0);
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public double getMaxY() {
        return this.y + 0.625;
    }
    
    @Override
    public boolean isPowerSource() {
        return true;
    }
    
    @Override
    public int getWeakPower(BlockFace face) {
        return this.getDamage();
    }

    public boolean isInverted() {
        return false;
    }

    public void updatePower() {
        int power = 0;

        if (this.level.getDimension() == Level.DIMENSION_OVERWORLD) {
            int skylight = getEffectiveSkyLightSignalAround(this.level, getFloorX(), getFloorY(), getFloorZ());
            power = skylight - this.level.calculateSkylightSubtracted(1.0F);

            float angle = this.level.calculateCelestialAngle(this.level.getTime(), 1.0F) * 6.2831855F;

            if (power > 0) {
                float target = angle < (float) Math.PI ? 0.0F : ((float) Math.PI * 2F);
                angle = angle + (target - angle) * 0.2F;
                power = Math.round((float) power * MathHelper.cos(angle));
            }

            power = MathHelper.clamp(power, 0, 15);
        }

        if (power != this.getDamage()) {
            this.setDamage(power);
            this.level.setBlock(this, this, false, true);
            updateAroundRedstone();
        }
    }

    public int getEffectiveSkyLightSignalAround(Level level, int x, int y, int z) {
        int skyReduction = level.calculateSkylightSubtracted(1.0F);

        int bestSignal = level.getBlockSkyLightAt(x, y + 1, z) - skyReduction;
        if (bestSignal >= 15) {
            return 15;
        }

        final int radius = 11;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int dist = Math.abs(dx) + Math.abs(dz);
                if (dist == 0 || dist > radius) {
                    continue;
                }

                int signal = level.getBlockSkyLightAt(x + dx, y + 1, z + dz) - skyReduction - dist;
                if (signal > bestSignal) {
                    bestSignal = signal;
                    if (bestSignal >= 15) {
                        return 15;
                    }
                }
            }
        }

        return Math.max(0, bestSignal);
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    protected AxisAlignedBB recalculateBoundingBox() {
        return new SimpleAxisAlignedBB(
                this.x,
                this.y,
                this.z,
                this.x + 1,
                this.y + 0.625,
                this.z + 1
        );
    }
}
