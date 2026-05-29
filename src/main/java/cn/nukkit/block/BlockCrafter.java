package cn.nukkit.block;

import cn.nukkit.Player;
import cn.nukkit.block.custom.properties.BlockProperties;
import cn.nukkit.block.properties.BlockPropertiesHelper;
import cn.nukkit.block.properties.VanillaProperties;
import cn.nukkit.block.properties.enums.CrafterOrientation;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntityCrafter;
import cn.nukkit.event.inventory.CraftItemEvent;
import cn.nukkit.inventory.CraftingRecipe;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.inventory.InventoryHolder;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemNamespaceId;
import cn.nukkit.item.ItemTool;
import cn.nukkit.level.Level;
import cn.nukkit.level.Sound;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.Tag;
import cn.nukkit.network.protocol.LevelEventPacket;
import cn.nukkit.network.protocol.LevelSoundEventPacket;
import cn.nukkit.network.protocol.ProtocolInfo;
import cn.nukkit.utils.Faceable;
import cn.nukkit.utils.RedstoneComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import com.google.common.collect.Sets;

/**
 * Minimal crafter implementation with a block entity backed 3x3 inventory.
 * <p>
 * Adapted from PowerNukkitX.
 */
public class BlockCrafter extends BlockSolidMeta implements RedstoneComponent, Faceable, BlockEntityHolder<BlockEntityCrafter>, BlockPropertiesHelper {

    private static final BlockProperties PROPERTIES = new BlockProperties(
            VanillaProperties.CRAFTER_ORIENTATION,
            VanillaProperties.CRAFTING,
            VanillaProperties.TRIGGERED
    );
    private static final Set<String> manualOverrides = Sets.newConcurrentHashSet();

    public BlockCrafter() {
        this(0);
    }

    public BlockCrafter(int meta) {
        super(meta);
    }

    @Override
    public String getName() {
        return "Crafter";
    }

    @Override
    public int getId() {
        return CRAFTER;
    }

    @Override
    public String getIdentifier() {
        return "minecraft:crafter";
    }

    @Override
    public BlockProperties getBlockProperties() {
        return PROPERTIES;
    }

    @NotNull
    @Override
    public Class<? extends BlockEntityCrafter> getBlockEntityClass() {
        return BlockEntityCrafter.class;
    }

    @NotNull
    @Override
    public String getBlockEntityType() {
        return BlockEntity.CRAFTER;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        if (player == null) {
            return false;
        }
        if (player.protocol < ProtocolInfo.v1_20_50) {
            return true;
        }

        player.addWindow(this.getOrCreateBlockEntity().getInventory());
        return true;
    }

    @Override
    public boolean place(@NotNull Item item, @NotNull Block block, @NotNull Block target, @NotNull BlockFace face,
                         double fx, double fy, double fz, @Nullable Player player) {
        if (player != null) {
            if (Math.abs(player.x - this.x) < 2 && Math.abs(player.z - this.z) < 2) {
                double y = player.y + player.getEyeHeight();
                if (y - this.y > 2) {
                    this.setOrientation(BlockFace.UP, player.getHorizontalFacing().getOpposite());
                } else if (this.y - y > 0) {
                    this.setOrientation(BlockFace.DOWN, player.getHorizontalFacing().getOpposite());
                } else {
                    this.setBlockFace(player.getHorizontalFacing().getOpposite());
                }
            } else {
                this.setBlockFace(player.getHorizontalFacing().getOpposite());
            }
        } else {
            this.setBlockFace(BlockFace.NORTH);
        }

        CompoundTag nbt = new CompoundTag();
        if (item.hasCustomName()) {
            nbt.putString("CustomName", item.getCustomName());
        }
        if (item.hasCustomBlockData()) {
            for (Map.Entry<String, Tag> tag : item.getCustomBlockData().getTags().entrySet()) {
                nbt.put(tag.getKey(), tag.getValue());
            }
        }
        return BlockEntityHolder.setBlockAndCreateEntity(this, true, true, nbt) != null;
    }

    @Override
    public Item toItem() {
        return Item.fromString(ItemNamespaceId.CRAFTER_NAMESPACE_ID);
    }

    @Override
    public BlockFace getBlockFace() {
        return this.getPropertyValue(VanillaProperties.CRAFTER_ORIENTATION).getPrimaryFace();
    }

    @Override
    public void setBlockFace(BlockFace face) {
        this.setOrientation(face, BlockFace.EAST);
    }

    private void setOrientation(BlockFace primary, BlockFace secondary) {
        this.setPropertyValue(VanillaProperties.CRAFTER_ORIENTATION, CrafterOrientation.byFaces(primary, secondary));
    }

    public boolean isTriggered() {
        return this.getBooleanValue(VanillaProperties.TRIGGERED);
    }

    public void setTriggered(boolean triggered) {
        this.setBooleanValue(VanillaProperties.TRIGGERED, triggered);
    }

    public boolean isCrafting() {
        return this.getBooleanValue(VanillaProperties.CRAFTING);
    }

    public void setCrafting(boolean crafting) {
        this.setBooleanValue(VanillaProperties.CRAFTING, crafting);
    }

    public void setManualOverride(boolean manualOverride) {
        String key = this.getManualOverrideKey();
        if (manualOverride) {
            manualOverrides.add(key);
        } else {
            manualOverrides.remove(key);
        }
    }

    public boolean getManualOverride() {
        return manualOverrides.contains(this.getManualOverrideKey());
    }

    @Override
    public int onUpdate(int type) {
        if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            if (this.isTriggered()) {
                this.level.addLevelSoundEvent(this, this.craft() ? LevelSoundEventPacket.SOUND_CRAFTER_CRAFT : LevelSoundEventPacket.SOUND_CRAFTER_FAILED);
                this.updateAllAroundRedstone();
                this.setTriggered(false);
                this.setCrafting(true);
                this.level.setBlock(this, this, false, false);
                this.level.scheduleUpdate(this, this, 4);
            } else if (this.isCrafting()) {
                this.setCrafting(false);
                this.level.setBlock(this, this, false, false);
            }
            return type;
        }

        if (type == Level.BLOCK_UPDATE_REDSTONE) {
            boolean powered = this.isGettingRedstonePower();
            if (powered && !this.isTriggered() && !this.getManualOverride()) {
                this.setManualOverride(true);
                this.setTriggered(true);
                this.level.setBlock(this, this, false, false);
                this.level.scheduleUpdate(this, this, 4);
            }
            if (!powered) {
                this.setManualOverride(false);
            }
            return type;
        }

        return 0;
    }

    public boolean craft() {
        BlockEntityCrafter crafter = this.getBlockEntity();
        if (crafter == null) {
            return false;
        }

        Inventory inventory = crafter.getInventory();
        List<Item> inputs = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            Item item = inventory.getItem(i);
            if (!item.isNull()) {
                inputs.add(item.clone());
            }
        }

        if (inputs.isEmpty()) {
            return false;
        }

        CraftingRecipe matched = null;
        for (cn.nukkit.inventory.Recipe recipe : this.level.getServer().getCraftingManager().recipes) {
            if (!(recipe instanceof CraftingRecipe craftingRecipe)) {
                continue;
            }
            if (craftingRecipe.requiresCraftingTable() && inventory.getSize() < 9) {
                continue;
            }

            List<Item> trialInputs = new ArrayList<>();
            for (Item input : inputs) {
                trialInputs.add(input.clone());
            }
            if (craftingRecipe.matchItems(trialInputs, Collections.emptyList())) {
                matched = craftingRecipe;
                break;
            }
        }

        if (matched == null) {
            return false;
        }

        CraftItemEvent event = new CraftItemEvent(null, inputs.toArray(Item.EMPTY_ARRAY), matched);
        this.level.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        List<Item> outputs = new ArrayList<>();
        outputs.add(matched.getResult().clone());
        outputs.addAll(matched.getExtraResults());

        for (Item output : outputs) {
            if (!this.dispenseCraftResult(output.clone())) {
                return false;
            }
        }

        List<Item> remaining = new ArrayList<>();
        for (Item ingredient : matched.getIngredientsAggregate()) {
            remaining.add(ingredient.clone());
        }

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            Item stack = inventory.getItem(slot);
            if (stack.isNull()) {
                continue;
            }

            for (Item need : remaining) {
                if (need.getCount() <= 0) {
                    continue;
                }
                if (!need.equals(stack, need.hasMeta(), need.hasCompoundTag())) {
                    continue;
                }

                int amount = Math.min(stack.getCount(), need.getCount());
                stack.setCount(stack.getCount() - amount);
                need.setCount(need.getCount() - amount);

                if (stack.getCount() <= 0) {
                    inventory.clear(slot);
                } else {
                    inventory.setItem(slot, stack);
                }
                break;
            }
        }

        return true;
    }

    private boolean dispenseCraftResult(Item result) {
        BlockFace facing = this.getBlockFace();

        LevelEventPacket packet = new LevelEventPacket();
        packet.x = (float) (0.5 + facing.getXOffset() * 0.7);
        packet.y = (float) (0.5 + facing.getYOffset() * 0.7);
        packet.z = (float) (0.5 + facing.getZOffset() * 0.7);
        packet.evid = LevelEventPacket.EVENT_PARTICLE_SHOOT;
        packet.data = 7;
        this.level.addChunkPacket(this.getChunkX(), this.getChunkZ(), packet);

        Block side = this.getSide(facing);
        if (this.level.getBlockEntityIfLoaded(side) instanceof InventoryHolder inventoryHolder) {
            Inventory target = inventoryHolder.getInventory();
            if (!target.canAddItem(result)) {
                return false;
            }
            target.addItem(result);
            return true;
        }

        this.level.addSound(this, Sound.RANDOM_CLICK);
        Vector3 dispensePos = this.getDispensePosition();
        if (facing.getAxis() == BlockFace.Axis.Y) {
            dispensePos.y -= 0.125;
        } else {
            dispensePos.y -= 0.15625;
        }

        Random random = ThreadLocalRandom.current();
        Vector3 motion = new Vector3();
        double offset = random.nextDouble() * 0.1 + 0.2;
        motion.x = facing.getXOffset() * offset;
        motion.y = 0.20000000298023224;
        motion.z = facing.getZOffset() * offset;
        motion.x += random.nextGaussian() * 0.007499999832361937 * 6;
        motion.y += random.nextGaussian() * 0.007499999832361937 * 6;
        motion.z += random.nextGaussian() * 0.007499999832361937 * 6;
        this.level.dropItem(dispensePos, result, motion);
        return true;
    }

    private Vector3 getDispensePosition() {
        BlockFace facing = this.getBlockFace();
        return this.add(
                0.5 + 0.7 * facing.getXOffset(),
                0.5 + 0.7 * facing.getYOffset(),
                0.5 + 0.7 * facing.getZOffset()
        );
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride() {
        BlockEntityCrafter crafter = this.getBlockEntity();
        return crafter == null ? 0 : crafter.getInventory().getComparatorOutput();
    }

    @Override
    public double getHardness() {
        return 1.5;
    }

    @Override
    public double getResistance() {
        return 3.5;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public int getToolTier() {
        return ItemTool.TIER_WOODEN;
    }

    @Override
    public boolean canBePushed() {
        BlockEntityCrafter crafter = this.getBlockEntity();
        return crafter == null || crafter.getInventory().getViewers().isEmpty();
    }

    @Override
    public boolean canBePulled() {
        return this.canBePushed();
    }

    private String getManualOverrideKey() {
        return this.level.getId() + ":" + this.getFloorX() + ":" + this.getFloorY() + ":" + this.getFloorZ();
    }
}
