package cn.nukkit.block;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemBlock;
import cn.nukkit.level.Level;
import cn.nukkit.math.MathHelper;

/**
 * Created on 2015/11/22 by CreeperFace.
 * Package cn.nukkit.block in project Nukkit .
 */
public class BlockDaylightDetectorInverted extends BlockDaylightDetector {

    @Override
    public int getId() {
        return DAYLIGHT_DETECTOR_INVERTED;
    }

    @Override
    public String getName() {
        return "Daylight Detector Inverted";
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        BlockDaylightDetector block = (BlockDaylightDetector) Block.get(DAYLIGHT_DETECTOR);
        this.getLevel().setBlock(this, block, true, true);
        block.updatePower();
        return true;
    }

    @Override
    public Item toItem() {
        return new ItemBlock(Block.get(DAYLIGHT_DETECTOR), 0);
    }

    @Override
    public boolean isInverted() {
        return true;
    }

    @Override
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

            power = MathHelper.clamp(power, 0, 15) > 0 ? 0 : 15;
        }

        if (power != this.getDamage()) {
            this.setDamage(power);
            this.level.setBlock(this, this, false, true);
            updateAroundRedstone();
        }
    }
}
