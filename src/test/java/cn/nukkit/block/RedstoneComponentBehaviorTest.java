package cn.nukkit.block;

import cn.nukkit.MockServer;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.blockentity.BlockEntityComparator;
import cn.nukkit.blockentity.BlockEntityHopper;
import cn.nukkit.blockentity.BlockEntityMovingBlock;
import cn.nukkit.blockentity.BlockEntityMusic;
import cn.nukkit.blockentity.BlockEntityPistonArm;
import cn.nukkit.blockentity.BlockEntityTarget;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.event.Event;
import cn.nukkit.event.block.BellRingEvent;
import cn.nukkit.event.block.BlockRedstoneEvent;
import cn.nukkit.event.redstone.RedstoneUpdateEvent;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.item.EntityMinecartAbstract;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.format.LevelProvider;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.BlockVector3;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.plugin.PluginManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.reflect.Method;

class RedstoneComponentBehaviorTest {


    @Test
    void redstoneBlockPlaceUpdatesDirectNeighbors() {
        Level level = Mockito.mock(Level.class, Mockito.CALLS_REAL_METHODS);
        Map<String, CountingBlock> blocks = new HashMap<>();
        Mockito.doAnswer(invocation -> {
            Vector3 pos = invocation.getArgument(0);
            String key = pos.getFloorX() + ":" + pos.getFloorY() + ":" + pos.getFloorZ();
            return blocks.computeIfAbsent(key, ignored -> new CountingBlock(level, pos));
        }).when(level).getBlock(Mockito.any(Vector3.class));
        Mockito.doAnswer(invocation -> {
            int x = invocation.getArgument(0);
            int y = invocation.getArgument(1);
            int z = invocation.getArgument(2);
            String key = x + ":" + y + ":" + z;
            return blocks.computeIfAbsent(key, ignored -> new CountingBlock(level, new Vector3(x, y, z)));
        }).when(level).getBlock(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.doReturn(true).when(level).setBlock(Mockito.any(Block.class), Mockito.any(Block.class), Mockito.anyBoolean(), Mockito.anyBoolean());

        BlockRedstone redstoneBlock = new BlockRedstone();
        redstoneBlock.setLevel(level);
        redstoneBlock.x = 0;
        redstoneBlock.y = 0;
        redstoneBlock.z = 0;

        Block block = new BlockStone();
        block.setLevel(level);
        block.x = 0;
        block.y = 0;
        block.z = 0;

        Block target = new BlockStone();
        target.setLevel(level);
        target.x = 0;
        target.y = -1;
        target.z = 0;

        Assertions.assertTrue(redstoneBlock.place(Item.get(Block.AIR), block, target, BlockFace.UP, 0.5, 0.5, 0.5, null));
        Assertions.assertEquals(6, blocks.values().stream().mapToInt(countingBlock -> countingBlock.updates).sum());
    }

    @Test
    void redstoneBlockBreakUpdatesDirectNeighbors() {
        Level level = Mockito.mock(Level.class, Mockito.CALLS_REAL_METHODS);
        Map<String, CountingBlock> blocks = new HashMap<>();
        Mockito.doAnswer(invocation -> {
            Vector3 pos = invocation.getArgument(0);
            String key = pos.getFloorX() + ":" + pos.getFloorY() + ":" + pos.getFloorZ();
            return blocks.computeIfAbsent(key, ignored -> new CountingBlock(level, pos));
        }).when(level).getBlock(Mockito.any(Vector3.class));
        Mockito.doAnswer(invocation -> {
            int x = invocation.getArgument(0);
            int y = invocation.getArgument(1);
            int z = invocation.getArgument(2);
            String key = x + ":" + y + ":" + z;
            return blocks.computeIfAbsent(key, ignored -> new CountingBlock(level, new Vector3(x, y, z)));
        }).when(level).getBlock(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.doReturn(true).when(level).setBlock(Mockito.any(Block.class), Mockito.any(Block.class), Mockito.anyBoolean(), Mockito.anyBoolean());

        BlockRedstone redstoneBlock = new BlockRedstone();
        redstoneBlock.setLevel(level);
        redstoneBlock.x = 0;
        redstoneBlock.y = 0;
        redstoneBlock.z = 0;

        Assertions.assertTrue(redstoneBlock.onBreak(Item.get(Block.AIR)));
        Assertions.assertEquals(6, blocks.values().stream().mapToInt(countingBlock -> countingBlock.updates).sum());
    }

    @Test
    void doorManualOverrideTracksBothHalves() {
        Level level = Mockito.mock(Level.class);
        BlockDoorWood bottom = new BlockDoorWood();
        bottom.setLevel(level);
        bottom.x = 10;
        bottom.y = 64;
        bottom.z = 10;

        BlockDoorWood top = new BlockDoorWood(BlockDoor.DOOR_TOP_BIT);
        top.setLevel(level);
        top.x = 10;
        top.y = 65;
        top.z = 10;

        Mockito.when(level.getBlock(10, 64, 10, 0)).thenReturn(bottom);
        Mockito.when(level.getBlock(10, 65, 10, 0)).thenReturn(top);

        bottom.setManualOverride(true);
        Assertions.assertTrue(bottom.getManualOverride());
        Assertions.assertTrue(top.getManualOverride());

        top.setManualOverride(false);
        Assertions.assertFalse(bottom.getManualOverride());
        Assertions.assertFalse(top.getManualOverride());
    }

    @Test
    void doorRedstoneRiseAndFallToggleStateAndFireTransitions() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        BlockDoorWood bottom = new BlockDoorWood();
        bottom.setLevel(fixture.level);
        bottom.x = 10;
        bottom.y = 64;
        bottom.z = 10;

        BlockDoorWood top = new BlockDoorWood(BlockDoor.DOOR_TOP_BIT);
        top.setLevel(fixture.level);
        top.x = 10;
        top.y = 65;
        top.z = 10;

        fixture.registerBlock(bottom);
        fixture.registerBlock(top);

        fixture.setPowered(bottom, true);
        bottom.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertTrue(bottom.isOpen());
        Assertions.assertTrue(fixture.eventTypes().contains(BlockRedstoneEvent.class));
        Assertions.assertEquals(List.of("0->15"), fixture.redstoneTransitions());

        fixture.events.clear();
        fixture.setPowered(bottom, false);
        bottom.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertFalse(bottom.isOpen());
        Assertions.assertTrue(fixture.eventTypes().contains(BlockRedstoneEvent.class));
        Assertions.assertEquals(List.of("15->0"), fixture.redstoneTransitions());
    }

    @Test
    void fenceGateManualOverrideIsSharedByLocation() {
        Level level = Mockito.mock(Level.class);
        BlockFenceGate gate = new BlockFenceGate();
        gate.setLevel(level);
        gate.x = 4;
        gate.y = 70;
        gate.z = -2;

        BlockFenceGate sameLocation = new BlockFenceGate();
        sameLocation.setLevel(level);
        sameLocation.x = 4;
        sameLocation.y = 70;
        sameLocation.z = -2;

        gate.setManualOverride(true);
        Assertions.assertTrue(gate.getManualOverride());
        Assertions.assertTrue(sameLocation.getManualOverride());

        sameLocation.setManualOverride(false);
        Assertions.assertFalse(gate.getManualOverride());
        Assertions.assertFalse(sameLocation.getManualOverride());
    }

    @Test
    void fenceGateRedstoneRiseAndFallToggleStateAndFireTransitions() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        BlockFenceGate gate = new BlockFenceGate();
        gate.setLevel(fixture.level);
        gate.x = 4;
        gate.y = 70;
        gate.z = -2;

        fixture.registerBlock(gate);

        fixture.setPowered(gate, true);
        gate.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertTrue(gate.isOpen());
        Assertions.assertTrue(fixture.eventTypes().contains(BlockRedstoneEvent.class));
        Assertions.assertEquals(List.of("0->15"), fixture.redstoneTransitions());

        fixture.events.clear();
        fixture.setPowered(gate, false);
        gate.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertFalse(gate.isOpen());
        Assertions.assertTrue(fixture.eventTypes().contains(BlockRedstoneEvent.class));
        Assertions.assertEquals(List.of("15->0"), fixture.redstoneTransitions());
    }

    @Test
    void trapdoorManualOverrideSuppressesUnpoweredRedstoneClose() {
        Level level = Mockito.mock(Level.class);
        BlockTrapdoor trapdoor = new BlockTrapdoor();
        trapdoor.setLevel(level);
        trapdoor.x = 2;
        trapdoor.y = 64;
        trapdoor.z = 3;
        trapdoor.setOpen(null, true);
        trapdoor.setManualOverride(true);

        Mockito.when(level.isBlockPowered(trapdoor)).thenReturn(false);

        trapdoor.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertTrue(trapdoor.isOpen());
        Assertions.assertTrue(trapdoor.getManualOverride());
    }

    @Test
    void trapdoorManualOverrideClearsWhenRedstoneMatchesState() {
        Level level = Mockito.mock(Level.class);
        BlockTrapdoor trapdoor = new BlockTrapdoor();
        trapdoor.setLevel(level);
        trapdoor.x = 2;
        trapdoor.y = 64;
        trapdoor.z = 3;
        trapdoor.setOpen(null, true);
        trapdoor.setManualOverride(true);

        Mockito.when(level.isBlockPowered(trapdoor)).thenReturn(true);

        trapdoor.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertTrue(trapdoor.isOpen());
        Assertions.assertFalse(trapdoor.getManualOverride());
    }

    @Test
    void trapdoorPlaceOpensImmediatelyWhenPowered() {
        Level level = Mockito.mock(Level.class);
        BlockTrapdoor trapdoor = new BlockTrapdoor();
        Block target = new BlockStone();
        Player player = Mockito.mock(Player.class);

        trapdoor.setLevel(level);
        trapdoor.x = 5;
        trapdoor.y = 70;
        trapdoor.z = 5;
        target.setLevel(level);
        target.x = 5;
        target.y = 70;
        target.z = 4;

        Mockito.when(level.setBlock(Mockito.any(Block.class), Mockito.any(Block.class), Mockito.anyBoolean(), Mockito.anyBoolean())).thenReturn(true);
        Mockito.when(level.isBlockPowered(trapdoor)).thenReturn(true);
        Mockito.when(player.getDirection()).thenReturn(BlockFace.SOUTH);

        boolean placed = trapdoor.place(Item.get(Block.AIR), trapdoor, target, BlockFace.NORTH, 0.5, 0.5, 0.5, player);

        Assertions.assertTrue(placed);
        Assertions.assertTrue(trapdoor.isOpen());
    }

    @Test
    void trapdoorRedstoneRiseAndFallToggleStateAndFireTransitions() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        BlockTrapdoor trapdoor = new BlockTrapdoor();
        trapdoor.setLevel(fixture.level);
        trapdoor.x = 2;
        trapdoor.y = 64;
        trapdoor.z = 3;

        fixture.registerBlock(trapdoor);

        fixture.setPowered(trapdoor, true);
        trapdoor.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertTrue(trapdoor.isOpen());
        Assertions.assertEquals(List.of(BlockRedstoneEvent.class), fixture.eventTypes());
        Assertions.assertEquals(List.of("0->15"), fixture.redstoneTransitions());

        fixture.events.clear();
        fixture.setPowered(trapdoor, false);
        trapdoor.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertFalse(trapdoor.isOpen());
        Assertions.assertEquals(List.of(BlockRedstoneEvent.class), fixture.eventTypes());
        Assertions.assertEquals(List.of("15->0"), fixture.redstoneTransitions());
    }




    @Test
    void observerScheduledPulseFiresRedstoneEventsInOrder() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        BlockObserver observer = new BlockObserver();
        observer.setLevel(fixture.level);
        observer.x = 0;
        observer.y = 0;
        observer.z = 0;
        observer.setBlockFace(BlockFace.SOUTH);

        observer.onUpdate(Level.BLOCK_UPDATE_SCHEDULED);
        observer.onUpdate(Level.BLOCK_UPDATE_SCHEDULED);

        Assertions.assertEquals(
                List.of(
                        RedstoneUpdateEvent.class,
                        BlockRedstoneEvent.class,
                        RedstoneUpdateEvent.class,
                        BlockRedstoneEvent.class
                ),
                fixture.eventTypes()
        );
        Assertions.assertEquals(List.of("0->15", "15->0"), fixture.redstoneTransitions());
    }


    @Test
    void observerNeighborChangeSchedulesOnlyForConfiguredInputFace() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        BlockObserver observer = new BlockObserver();
        observer.setLevel(fixture.level);
        observer.x = 0;
        observer.y = 0;
        observer.z = 0;
        observer.setBlockFace(BlockFace.SOUTH);

        observer.onNeighborChange(BlockFace.SOUTH);

        Assertions.assertTrue(fixture.scheduledUpdates.contains("0:0:0:1"));
        Assertions.assertEquals(List.of(RedstoneUpdateEvent.class), fixture.eventTypes());

        fixture.scheduledUpdates.clear();
        fixture.events.clear();
        observer.onNeighborChange(BlockFace.NORTH);

        Assertions.assertTrue(fixture.scheduledUpdates.isEmpty());
        Assertions.assertTrue(fixture.events.isEmpty());
    }

    @Test
    void redstoneTorchBurnoutMatchesVanillaWindowAndCooldown() {
        LevelFixture fixture = new LevelFixture();
        Server server = Mockito.mock(Server.class);
        PluginManager pluginManager = Mockito.mock(PluginManager.class);
        Mockito.when(fixture.level.getServer()).thenReturn(server);
        Mockito.when(server.getPluginManager()).thenReturn(pluginManager);

        BlockRedstoneTorch torch = new BlockRedstoneTorch(1);
        torch.setLevel(fixture.level);
        torch.x = 10;
        torch.y = 0;
        torch.z = 0;

        fixture.poweredSides.put("9:0:0:" + BlockFace.WEST.getIndex(), true);

        for (int i = 0; i < 7; i++) {
            Mockito.when(server.getTick()).thenReturn(i * 2);
            Assertions.assertTrue(torch.checkState());
            Assertions.assertFalse(BlockRedstoneTorch.isBurnedOut(torch));
        }

        Mockito.when(server.getTick()).thenReturn(14);
        Assertions.assertTrue(torch.checkState());
        Assertions.assertTrue(BlockRedstoneTorch.isBurnedOut(torch));
        Assertions.assertTrue(fixture.scheduledUpdates.contains("10:0:0:" + BlockRedstoneTorch.BURNOUT_COOLDOWN_TICKS));

        BlockRedstoneTorchUnlit unlitTorch = new BlockRedstoneTorchUnlit(1);
        unlitTorch.setLevel(fixture.level);
        unlitTorch.x = 10;
        unlitTorch.y = 0;
        unlitTorch.z = 0;

        fixture.poweredSides.clear();
        fixture.scheduledUpdates.clear();

        Mockito.when(server.getTick()).thenReturn(20);
        Assertions.assertFalse(unlitTorch.checkState());
        Assertions.assertTrue(fixture.scheduledUpdates.isEmpty());

        Mockito.when(server.getTick()).thenReturn(175);
        Assertions.assertTrue(unlitTorch.checkState());
    }

    @Test
    void repeaterScheduledUpdateOnlyTouchesOutputBlockAndDirectNeighbors() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        TestRepeater repeater = new TestRepeater();
        repeater.setLevel(fixture.level);
        repeater.x = 0;
        repeater.y = 0;
        repeater.z = 0;
        repeater.setDamage(BlockFace.SOUTH.getHorizontalIndex());

        repeater.onUpdate(Level.BLOCK_UPDATE_SCHEDULED);

        Assertions.assertEquals(7, fixture.totalUpdates());
        Assertions.assertEquals(1, fixture.updatesAt(0, 0, -1));
        Assertions.assertEquals(1, fixture.updatesAt(0, 0, -2));
        Assertions.assertEquals(0, fixture.updatesAt(0, 0, -3));
    }

    @Test
    void comparatorScheduledUpdateOnlyTouchesOutputBlockAndDirectNeighbors() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        BlockEntityComparator blockEntity = Mockito.mock(BlockEntityComparator.class);
        Mockito.when(blockEntity.getOutputSignal()).thenReturn(0);

        TestComparator comparator = new TestComparator();
        comparator.setLevel(fixture.level);
        comparator.x = 0;
        comparator.y = 0;
        comparator.z = 0;
        comparator.setDamage(BlockFace.SOUTH.getHorizontalIndex());

        Mockito.when(fixture.level.getBlockEntity(comparator)).thenReturn(blockEntity);

        comparator.onUpdate(Level.BLOCK_UPDATE_SCHEDULED);

        Mockito.verify(blockEntity).setOutputSignal(12);
        Assertions.assertEquals(7, fixture.totalUpdates());
        Assertions.assertEquals(1, fixture.updatesAt(0, 0, -1));
        Assertions.assertEquals(1, fixture.updatesAt(0, 0, -2));
        Assertions.assertEquals(0, fixture.updatesAt(0, 0, -3));
    }

    @Test
    void repeaterRedstoneUpdateSchedulesConfiguredDelayOnRisingEdge() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        TestConfigurableRepeater repeater = new TestConfigurableRepeater();
        repeater.setLevel(fixture.level);
        repeater.x = 1;
        repeater.y = 0;
        repeater.z = 1;
        repeater.setDamage(BlockFace.SOUTH.getHorizontalIndex() + 4);
        repeater.setShouldBePowered(true);

        repeater.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertTrue(fixture.scheduledUpdates.contains("1:0:1:4"));
    }

    @Test
    void repeaterLockedRedstoneUpdateDoesNotScheduleTick() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        TestLockedRepeater repeater = new TestLockedRepeater();
        repeater.setLevel(fixture.level);
        repeater.x = 2;
        repeater.y = 0;
        repeater.z = 2;
        repeater.setShouldBePowered(true);

        repeater.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertTrue(fixture.scheduledUpdates.isEmpty());
    }

    @Test
    void comparatorActivationTogglesSubtractModeAndRecomputesOutput() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        BlockEntityComparator blockEntity = Mockito.mock(BlockEntityComparator.class);
        Mockito.when(blockEntity.getOutputSignal()).thenReturn(12);

        TestConfigurableComparator comparator = new TestConfigurableComparator();
        comparator.setLevel(fixture.level);
        comparator.x = 3;
        comparator.y = 0;
        comparator.z = 3;
        comparator.setDamage(BlockFace.SOUTH.getHorizontalIndex());
        comparator.inputStrength = 12;
        comparator.sidePower = 3;

        Mockito.when(fixture.level.getBlockEntity(comparator)).thenReturn(blockEntity);

        comparator.onActivate(Item.get(Item.AIR), null);

        Assertions.assertEquals(BlockRedstoneComparator.Mode.SUBTRACT, comparator.getMode());
        Mockito.verify(blockEntity).setOutputSignal(9);
        Assertions.assertEquals(7, fixture.totalUpdates());
        Assertions.assertEquals(1, fixture.updatesAt(3, 0, 2));
    }


    @Test
    void redstoneTorchSkipsInputSideWhenBurningOut() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        BlockRedstoneTorch torch = new BlockRedstoneTorch(1);
        torch.setLevel(fixture.level);
        torch.x = 0;
        torch.y = 0;
        torch.z = 0;

        fixture.poweredSides.put("-1:0:0:" + BlockFace.WEST.getIndex(), true);

        torch.onUpdate(Level.BLOCK_UPDATE_SCHEDULED);

        Assertions.assertEquals(0, fixture.updatesAt(-1, 0, 0));
        Assertions.assertTrue(fixture.updatesAt(1, 0, 0) > 0);
    }

    @Test
    void redstoneTorchScheduledUpdateFiresRedstoneUpdateEventBeforeStateSwap() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        BlockRedstoneTorch torch = new BlockRedstoneTorch(1);
        torch.setLevel(fixture.level);
        torch.x = 0;
        torch.y = 0;
        torch.z = 0;

        fixture.poweredSides.put("-1:0:0:" + BlockFace.WEST.getIndex(), true);

        torch.onUpdate(Level.BLOCK_UPDATE_SCHEDULED);

        Assertions.assertEquals(1, fixture.eventTypes().size());
        Assertions.assertEquals(RedstoneUpdateEvent.class, fixture.eventTypes().get(0));
    }

    @Test
    void unlitRedstoneTorchScheduledUpdateRelightsAndSkipsInputSide() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        BlockRedstoneTorchUnlit torch = new BlockRedstoneTorchUnlit(1);
        torch.setLevel(fixture.level);
        torch.x = 0;
        torch.y = 0;
        torch.z = 0;

        torch.onUpdate(Level.BLOCK_UPDATE_SCHEDULED);

        Assertions.assertEquals(1, fixture.eventTypes().size());
        Assertions.assertEquals(RedstoneUpdateEvent.class, fixture.eventTypes().get(0));
        Assertions.assertEquals(0, fixture.updatesAt(-1, 0, 0));
        Assertions.assertTrue(fixture.updatesAt(1, 0, 0) > 0);
    }

    @Test
    void redstoneTorchPlaceUpdatesNeighborsWhenRemainingLit() {
        LevelFixture fixture = new LevelFixture();

        BlockRedstoneTorch torch = new BlockRedstoneTorch();
        torch.setLevel(fixture.level);
        torch.x = 0;
        torch.y = 0;
        torch.z = 0;

        BlockAir block = new BlockAir();
        block.setLevel(fixture.level);
        block.x = 0;
        block.y = 0;
        block.z = 0;

        BlockStone support = new BlockStone();
        support.setLevel(fixture.level);
        support.x = 0;
        support.y = -1;
        support.z = 0;
        fixture.registerBlock(support);

        Assertions.assertTrue(torch.place(Item.get(Block.AIR), block, support, BlockFace.UP, 0.5, 0.5, 0.5, null));
        Assertions.assertEquals(0, fixture.updatesAt(0, -1, 0));
        Assertions.assertTrue(fixture.totalUpdates() > 0);
    }


    @Test
    void leverActivationUsesDirectNeighborUpdatesOnly() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        BlockLever lever = new BlockLever(1);
        lever.setLevel(fixture.level);
        lever.x = 0;
        lever.y = 0;
        lever.z = 0;

        lever.onActivate(Item.get(Item.AIR), null);

        Assertions.assertEquals(11, fixture.totalUpdates());
        Assertions.assertEquals(1, fixture.updatesAt(-1, 0, 0));
        Assertions.assertEquals(1, fixture.updatesAt(-2, 0, 0));
        Assertions.assertEquals(0, fixture.updatesAt(-3, 0, 0));
    }


    @Test
    void leverActivationFiresSinglePowerRiseTransition() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        BlockLever lever = new BlockLever(1);
        lever.setLevel(fixture.level);
        lever.x = 0;
        lever.y = 0;
        lever.z = 0;

        lever.onActivate(Item.get(Item.AIR), null);

        Assertions.assertEquals(List.of(BlockRedstoneEvent.class), fixture.eventTypes());
        Assertions.assertEquals(List.of("0->15"), fixture.redstoneTransitions());
    }

    @Test
    void buttonPressAndReleaseFireExpectedTransitions() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        BlockButtonStone button = new BlockButtonStone(5);
        button.setLevel(fixture.level);
        button.x = 0;
        button.y = 0;
        button.z = 0;

        button.onActivate(Item.get(Item.AIR), null);
        button.onUpdate(Level.BLOCK_UPDATE_SCHEDULED);

        Assertions.assertEquals(
                List.of(BlockRedstoneEvent.class, BlockRedstoneEvent.class),
                fixture.eventTypes()
        );
        Assertions.assertEquals(List.of("0->15", "15->0"), fixture.redstoneTransitions());
    }

    @Test
    void pressurePlateStateUpdateStaysOnDirectNeighbors() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        TestPressurePlate plate = new TestPressurePlate();
        plate.setLevel(fixture.level);
        plate.x = 0;
        plate.y = 0;
        plate.z = 0;
        plate.nextStrength = 15;

        plate.updateState(0);

        Assertions.assertEquals(12, fixture.totalUpdates());
        Assertions.assertEquals(1, fixture.updatesAt(0, -2, 0));
        Assertions.assertEquals(0, fixture.updatesAt(0, -3, 0));
    }

    @Test
    void pressurePlateRiseAndFallFireExpectedTransitions() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        TestPressurePlate plate = new TestPressurePlate();
        plate.setLevel(fixture.level);
        plate.x = 0;
        plate.y = 0;
        plate.z = 0;
        plate.nextStrength = 15;

        plate.updateState(0);
        plate.nextStrength = 0;
        plate.updateState(15);

        Assertions.assertEquals(
                List.of(BlockRedstoneEvent.class, BlockRedstoneEvent.class),
                fixture.eventTypes()
        );
        Assertions.assertEquals(List.of("0->15", "15->0"), fixture.redstoneTransitions());
    }

    @Test
    void detectorRailStrongPowerMatchesPoweredState() {
        LevelFixture fixture = new LevelFixture();
        BlockRailDetector detector = new BlockRailDetector(0);
        detector.setLevel(fixture.level);
        detector.setActive(false);
        Assertions.assertEquals(0, detector.getStrongPower(BlockFace.UP));

        detector.setActive(true);
        Assertions.assertEquals(15, detector.getStrongPower(BlockFace.UP));
        Assertions.assertEquals(0, detector.getStrongPower(BlockFace.NORTH));
    }


    @Test
    void dispenserOnlyRetriggersAfterPowerDrops() {
        LevelFixture fixture = new LevelFixture();
        TestDispenser dispenser = new TestDispenser();
        dispenser.setLevel(fixture.level);
        dispenser.x = 0;
        dispenser.y = 0;
        dispenser.z = 0;

        fixture.setPowered(dispenser, true);

        dispenser.onUpdate(Level.BLOCK_UPDATE_REDSTONE);
        Assertions.assertTrue(dispenser.isTriggered());
        Assertions.assertEquals(1, fixture.scheduledUpdates.size());

        dispenser.onUpdate(Level.BLOCK_UPDATE_SCHEDULED);
        Assertions.assertEquals(1, dispenser.dispenseCalls);
        Assertions.assertTrue(dispenser.isTriggered());

        dispenser.onUpdate(Level.BLOCK_UPDATE_REDSTONE);
        Assertions.assertEquals(1, fixture.scheduledUpdates.size());

        fixture.setPowered(dispenser, false);
        dispenser.onUpdate(Level.BLOCK_UPDATE_REDSTONE);
        Assertions.assertFalse(dispenser.isTriggered());

        fixture.setPowered(dispenser, true);
        dispenser.onUpdate(Level.BLOCK_UPDATE_REDSTONE);
        Assertions.assertEquals(2, fixture.scheduledUpdates.size());
    }


    @Test
    void hopperRedstoneUpdateWakesBlockEntityWhenUnlocked() {
        LevelFixture fixture = new LevelFixture();
        BlockEntityHopper hopperEntity = Mockito.mock(BlockEntityHopper.class);
        BlockHopper hopper = new BlockHopper(8);
        hopper.setLevel(fixture.level);
        hopper.x = 0;
        hopper.y = 0;
        hopper.z = 0;

        Mockito.when(fixture.level.getBlockEntity(hopper)).thenReturn(hopperEntity);
        fixture.setPowered(hopper, false);

        hopper.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertTrue(hopper.isEnabled());
        Mockito.verify(hopperEntity).scheduleUpdate();
    }

    @Test
    void noteblockOnlyEmitsOnRedstoneRisingEdge() {
        LevelFixture fixture = new LevelFixture();
        TestNoteblock noteblock = new TestNoteblock();
        noteblock.setLevel(fixture.level);
        noteblock.x = 0;
        noteblock.y = 0;
        noteblock.z = 0;

        BlockEntityMusic music = Mockito.mock(BlockEntityMusic.class);
        AtomicBoolean poweredState = new AtomicBoolean(false);
        Mockito.when(music.isPowered()).thenAnswer(invocation -> poweredState.get());
        Mockito.doAnswer(invocation -> {
            poweredState.set(invocation.getArgument(0));
            return null;
        }).when(music).setPowered(Mockito.anyBoolean());
        Mockito.when(fixture.level.getBlockEntity(noteblock)).thenReturn(music);

        fixture.setPowered(noteblock, true);
        noteblock.onUpdate(Level.BLOCK_UPDATE_REDSTONE);
        noteblock.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        fixture.setPowered(noteblock, false);
        noteblock.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        fixture.setPowered(noteblock, true);
        noteblock.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertEquals(2, noteblock.emitCalls);
        Assertions.assertTrue(poweredState.get());
    }




    @Test
    void bellTreatsSameLevelPoweredWireAsRedstoneInput() {
        LevelFixture fixture = new LevelFixture();
        TestBell bell = new TestBell();
        bell.setLevel(fixture.level);
        bell.x = 0;
        bell.y = 0;
        bell.z = 0;

        BlockRedstoneWire wire = new BlockRedstoneWire(7);
        wire.setLevel(fixture.level);
        wire.x = 1;
        wire.y = 0;
        wire.z = 0;
        fixture.registerBlock(wire);

        bell.onUpdate(Level.BLOCK_UPDATE_REDSTONE);
        bell.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        wire.setDamage(0);
        bell.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertTrue(bell.wasPoweredByWire());
        Assertions.assertEquals(1, bell.ringCalls);
        Assertions.assertFalse(bell.isToggled());
    }

    @Test
    void redstoneLampTreatsSameLevelPoweredWireAsInput() {
        LevelFixture fixture = new LevelFixture();
        BlockRedstoneLamp lamp = new BlockRedstoneLamp();
        lamp.setLevel(fixture.level);
        lamp.x = 0;
        lamp.y = 0;
        lamp.z = 0;

        BlockRedstoneWire wire = new BlockRedstoneWire(10);
        wire.setLevel(fixture.level);
        wire.x = 1;
        wire.y = 0;
        wire.z = 0;
        fixture.registerBlock(wire);

        lamp.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertTrue(fixture.eventTypes().contains(RedstoneUpdateEvent.class));
    }

    @Test
    void litRedstoneLampSchedulesPowerDownWhenSameLevelWireTurnsOff() {
        LevelFixture fixture = new LevelFixture();
        BlockRedstoneLampLit lamp = new BlockRedstoneLampLit();
        lamp.setLevel(fixture.level);
        lamp.x = 0;
        lamp.y = 0;
        lamp.z = 0;

        BlockRedstoneWire wire = new BlockRedstoneWire(12);
        wire.setLevel(fixture.level);
        wire.x = 1;
        wire.y = 0;
        wire.z = 0;
        fixture.registerBlock(wire);

        lamp.onUpdate(Level.BLOCK_UPDATE_REDSTONE);
        wire.setDamage(0);
        lamp.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertTrue(fixture.scheduledUpdates.contains("0:0:0:4"));
    }










    @Test
    void tripWireCollisionPowersWireAndSchedulesDecay() {
        LevelFixture fixture = new LevelFixture();
        TestTripWire wire = new TestTripWire();
        wire.setLevel(fixture.level);
        wire.x = 4;
        wire.y = 64;
        wire.z = 4;

        Entity entity = Mockito.mock(Entity.class);
        Mockito.when(entity.doesTriggerPressurePlate()).thenReturn(true);

        wire.onEntityCollide(entity);

        Assertions.assertTrue(wire.isPowered());
        Assertions.assertEquals(1, wire.updateHookCalls);
        Assertions.assertFalse(wire.lastScheduleUpdateFlag);
        Assertions.assertTrue(fixture.scheduledUpdates.contains("4:64:4:10"));
    }

    @Test
    void tripWireHookPoweredChainFiresRedstoneEventAndSchedulesRecheck() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        BlockTripWireHook first = new BlockTripWireHook();
        first.setLevel(fixture.level);
        first.x = 0;
        first.y = 64;
        first.z = 0;
        first.setFace(BlockFace.EAST);

        BlockTripWireHook second = new BlockTripWireHook();
        second.setLevel(fixture.level);
        second.x = 2;
        second.y = 64;
        second.z = 0;
        second.setFace(BlockFace.WEST);

        BlockTripWire wire = new BlockTripWire();
        wire.setLevel(fixture.level);
        wire.x = 1;
        wire.y = 64;
        wire.z = 0;
        wire.setPowered(true);

        fixture.registerBlock(second);
        fixture.registerBlock(wire);

        first.calculateState(false, true, 1, wire);

        Assertions.assertTrue(fixture.eventTypes().contains(BlockRedstoneEvent.class));
        Assertions.assertTrue(fixture.scheduledUpdates.contains("0:64:0:10"));
    }

    @Test
    void redstoneWireLosesOneStrengthFromAdjacentWire() {
        MockServer.init();

        StatefulLevelFixture fixture = new StatefulLevelFixture();
        BlockRedstoneWire wire = new BlockRedstoneWire();
        wire.setLevel(fixture.level);
        wire.x = 0;
        wire.y = 64;
        wire.z = 0;

        BlockRedstoneWire poweredNeighbor = new BlockRedstoneWire();
        poweredNeighbor.setLevel(fixture.level);
        poweredNeighbor.x = 1;
        poweredNeighbor.y = 64;
        poweredNeighbor.z = 0;

        BlockRedstone redstoneBlock = new BlockRedstone();
        redstoneBlock.setLevel(fixture.level);
        redstoneBlock.x = 2;
        redstoneBlock.y = 64;
        redstoneBlock.z = 0;

        fixture.registerBlock(wire);
        fixture.registerBlock(poweredNeighbor);
        fixture.registerBlock(redstoneBlock);

        poweredNeighbor.onUpdate(Level.BLOCK_UPDATE_REDSTONE);
        Assertions.assertEquals(15, poweredNeighbor.getDamage());

        wire.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertEquals(14, wire.getDamage());
    }

    @Test
    void redstoneWireCanClimbPowerUpFullBlockSide() {
        MockServer.init();

        StatefulLevelFixture fixture = new StatefulLevelFixture();
        BlockRedstoneWire wire = new BlockRedstoneWire();
        wire.setLevel(fixture.level);
        wire.x = 0;
        wire.y = 64;
        wire.z = 0;

        BlockStone support = new BlockStone();
        support.setLevel(fixture.level);
        support.x = 1;
        support.y = 64;
        support.z = 0;

        BlockAir airAbove = new BlockAir();
        airAbove.setLevel(fixture.level);
        airAbove.x = 0;
        airAbove.y = 65;
        airAbove.z = 0;

        BlockRedstoneWire elevatedWire = new BlockRedstoneWire();
        elevatedWire.setLevel(fixture.level);
        elevatedWire.x = 1;
        elevatedWire.y = 65;
        elevatedWire.z = 0;

        BlockRedstone redstoneBlock = new BlockRedstone();
        redstoneBlock.setLevel(fixture.level);
        redstoneBlock.x = 2;
        redstoneBlock.y = 65;
        redstoneBlock.z = 0;

        fixture.registerBlock(wire);
        fixture.registerBlock(support);
        fixture.registerBlock(airAbove);
        fixture.registerBlock(elevatedWire);
        fixture.registerBlock(redstoneBlock);

        elevatedWire.onUpdate(Level.BLOCK_UPDATE_REDSTONE);
        Assertions.assertEquals(15, elevatedWire.getDamage());

        wire.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertEquals(14, wire.getDamage());
    }

    @Test
    void redstoneWireCanReceivePowerFromLowerAdjacentWire() {
        MockServer.init();

        StatefulLevelFixture fixture = new StatefulLevelFixture();
        BlockRedstoneWire wire = new BlockRedstoneWire();
        wire.setLevel(fixture.level);
        wire.x = 0;
        wire.y = 64;
        wire.z = 0;

        BlockRedstoneWire lowerWire = new BlockRedstoneWire();
        lowerWire.setLevel(fixture.level);
        lowerWire.x = 1;
        lowerWire.y = 63;
        lowerWire.z = 0;

        BlockAir sideAir = new BlockAir();
        sideAir.setLevel(fixture.level);
        sideAir.x = 1;
        sideAir.y = 64;
        sideAir.z = 0;

        BlockRedstone redstoneBlock = new BlockRedstone();
        redstoneBlock.setLevel(fixture.level);
        redstoneBlock.x = 2;
        redstoneBlock.y = 63;
        redstoneBlock.z = 0;

        fixture.registerBlock(wire);
        fixture.registerBlock(sideAir);
        fixture.registerBlock(lowerWire);
        fixture.registerBlock(redstoneBlock);

        lowerWire.onUpdate(Level.BLOCK_UPDATE_REDSTONE);
        Assertions.assertEquals(15, lowerWire.getDamage());

        wire.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertEquals(14, wire.getDamage());
    }

    @Test
    void redstoneWireDoesNotClimbTopSlabSide() {
        MockServer.init();

        StatefulLevelFixture fixture = new StatefulLevelFixture();
        BlockRedstoneWire wire = new BlockRedstoneWire();
        wire.setLevel(fixture.level);
        wire.x = 0;
        wire.y = 64;
        wire.z = 0;

        BlockSlabStone topSlab = new BlockSlabStone(BlockSlab.SLAB_TOP_BIT);
        topSlab.setLevel(fixture.level);
        topSlab.x = 1;
        topSlab.y = 64;
        topSlab.z = 0;

        BlockAir airAbove = new BlockAir();
        airAbove.setLevel(fixture.level);
        airAbove.x = 0;
        airAbove.y = 65;
        airAbove.z = 0;

        BlockRedstoneWire elevatedWire = new BlockRedstoneWire();
        elevatedWire.setLevel(fixture.level);
        elevatedWire.x = 1;
        elevatedWire.y = 65;
        elevatedWire.z = 0;

        BlockRedstone redstoneBlock = new BlockRedstone();
        redstoneBlock.setLevel(fixture.level);
        redstoneBlock.x = 2;
        redstoneBlock.y = 65;
        redstoneBlock.z = 0;

        fixture.registerBlock(wire);
        fixture.registerBlock(airAbove);
        fixture.registerBlock(topSlab);
        fixture.registerBlock(elevatedWire);
        fixture.registerBlock(redstoneBlock);

        elevatedWire.onUpdate(Level.BLOCK_UPDATE_REDSTONE);
        Assertions.assertEquals(15, elevatedWire.getDamage());

        wire.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertEquals(0, wire.getDamage());
    }


    @Test
    void targetSchedulesPnxEquivalentPulseDurations() {
        LevelFixture fixture = new LevelFixture();
        BlockEntityTarget targetEntity = Mockito.mock(BlockEntityTarget.class);
        Mockito.when(targetEntity.getActivePower()).thenReturn(0);

        BlockTarget target = Mockito.spy(new BlockTarget());
        target.setLevel(fixture.level);
        target.x = 2;
        target.y = 0;
        target.z = 2;
        Mockito.doReturn(targetEntity).when(target).getOrCreateBlockEntity();
        Mockito.when(target.getBlockEntity()).thenReturn(targetEntity);

        Assertions.assertTrue(target.activatePower(7));
        Assertions.assertTrue(fixture.scheduledUpdates.contains("2:0:2:8"));

        fixture.scheduledUpdates.clear();
        Assertions.assertTrue(target.activatePower(7, 20));
        Assertions.assertTrue(fixture.scheduledUpdates.contains("2:0:2:20"));
    }

    @Test
    void targetSamePowerRefreshDoesNotFanOutSecondNeighborUpdate() {
        LevelFixture fixture = new LevelFixture();
        BlockEntityTarget targetEntity = Mockito.mock(BlockEntityTarget.class);
        Mockito.when(targetEntity.getActivePower()).thenReturn(0, 7);

        BlockTarget target = Mockito.spy(new BlockTarget());
        target.setLevel(fixture.level);
        target.x = 2;
        target.y = 0;
        target.z = 2;
        Mockito.doReturn(targetEntity).when(target).getOrCreateBlockEntity();
        Mockito.when(target.getBlockEntity()).thenReturn(targetEntity);

        Assertions.assertTrue(target.activatePower(7));
        int firstFanout = fixture.totalUpdates();

        Assertions.assertTrue(target.activatePower(7));
        Assertions.assertEquals(firstFanout, fixture.totalUpdates());
    }











    @Test
    void pistonBlocksCalculatorDoesNotCrossStickBetweenHoneyAndSlimeOnRetraction() {
        Level level = Mockito.mock(Level.class);
        TestStickyPiston piston = new TestStickyPiston();
        piston.setLevel(level);
        piston.x = 0;
        piston.y = 0;
        piston.z = 0;
        piston.setDamage(BlockFace.WEST.getIndex());

        BlockPistonHeadSticky head = new BlockPistonHeadSticky(BlockFace.WEST.getIndex());
        head.setLevel(level);
        head.x = 1;
        head.y = 0;
        head.z = 0;

        BlockHoneyBlock honey = new BlockHoneyBlock();
        honey.setLevel(level);
        honey.x = 2;
        honey.y = 0;
        honey.z = 0;

        BlockSlime slime = new BlockSlime();
        slime.setLevel(level);
        slime.x = 3;
        slime.y = 0;
        slime.z = 0;

        Mockito.when(level.getMinBlockY()).thenReturn(-64);
        Mockito.when(level.getMaxBlockY()).thenReturn(320);
        Mockito.when(level.getBlock(Mockito.any(Vector3.class))).thenAnswer(invocation -> {
            Vector3 pos = invocation.getArgument(0);
            return pistonTestBlockAt(level, head, honey, slime, pos.getFloorX(), pos.getFloorY(), pos.getFloorZ());
        });
        Mockito.when(level.getBlock(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenAnswer(invocation -> {
            int x = invocation.getArgument(0);
            int y = invocation.getArgument(1);
            int z = invocation.getArgument(2);
            return pistonTestBlockAt(level, head, honey, slime, x, y, z);
        });

        BlockPistonBase.BlocksCalculator calculator = piston.new BlocksCalculator(false);

        Assertions.assertTrue(calculator.canMove());
        Assertions.assertEquals(1, calculator.getBlocksToMove().size());
        Assertions.assertEquals(Block.HONEY_BLOCK, calculator.getBlocksToMove().get(0).getId());
    }


    private static Block pistonTestBlockAt(Level level, Block head, Block honey, Block slime, int x, int y, int z) {
        if (x == 1 && y == 0 && z == 0) {
            return head;
        }
        if (x == 2 && y == 0 && z == 0) {
            return honey;
        }
        if (x == 3 && y == 0 && z == 0) {
            return slime;
        }
        BlockAir air = new BlockAir();
        air.setLevel(level);
        air.x = x;
        air.y = y;
        air.z = z;
        return air;
    }


    @Test
    void pistonArmCompletionRestoresMovedBlockAndFansOutRedstone() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        FullChunk chunk = Mockito.mock(FullChunk.class);
        LevelProvider provider = Mockito.mock(LevelProvider.class);
        Mockito.when(chunk.getProvider()).thenReturn(provider);
        Mockito.when(provider.getLevel()).thenReturn(fixture.level);

        CompoundTag nbt = new CompoundTag()
                .putInt("x", 0)
                .putInt("y", 0)
                .putInt("z", 0)
                .putByte("State", 1)
                .putByte("NewState", 1)
                .putBoolean("Sticky", false)
                .putBoolean("Extending", true)
                .putBoolean("powered", true)
                .putInt("facing", BlockFace.EAST.getIndex());

        BlockEntityPistonArm arm = new BlockEntityPistonArm(chunk, nbt);
        arm.setLevel(fixture.level);
        arm.facing = BlockFace.EAST;
        arm.extending = true;
        arm.progress = 0.5f;
        arm.lastProgress = 0.5f;
        arm.attachedBlocks = new ArrayList<>(List.of(new BlockVector3(1, 0, 0)));

        TestMovedBlock moved = new TestMovedBlock();
        moved.setLevel(fixture.level);

        BlockEntityMovingBlock movingBlock = Mockito.mock(BlockEntityMovingBlock.class);
        Mockito.when(movingBlock.getBlock()).thenReturn(moved);
        Mockito.when(movingBlock.getBlockEntity()).thenReturn(null);
        Mockito.when(movingBlock.getFloorX()).thenReturn(2);
        Mockito.when(movingBlock.getFloorY()).thenReturn(0);
        Mockito.when(movingBlock.getFloorZ()).thenReturn(0);
        Mockito.when(movingBlock.getChunkX()).thenReturn(0);
        Mockito.when(movingBlock.getChunkZ()).thenReturn(0);

        Mockito.doAnswer(invocation -> {
            Vector3 pos = invocation.getArgument(0);
            if (pos.getFloorX() == 2 && pos.getFloorY() == 0 && pos.getFloorZ() == 0) {
                return movingBlock;
            }
            return null;
        }).when(fixture.level).getBlockEntity(Mockito.any(Vector3.class));
        Mockito.doAnswer(invocation -> {
            BlockVector3 pos = invocation.getArgument(0);
            if (pos.x == 2 && pos.y == 0 && pos.z == 0) {
                return movingBlock;
            }
            return null;
        }).when(fixture.level).getBlockEntity(Mockito.any(BlockVector3.class));

        arm.onUpdate();

        Assertions.assertEquals(1, moved.movedUpdates);
        Assertions.assertTrue(fixture.totalUpdates() > 0);
        Assertions.assertTrue(fixture.updatesAt(1, 0, 0) > 0);
        Assertions.assertTrue(fixture.updatesAt(2, 0, 0) > 0);
        Assertions.assertTrue(fixture.scheduledUpdates.contains("0:0:0:1"));
    }



    @Test
    void pistonArmDoesNotDirectlyMovePlayers() {
        MockServer.init();

        FullChunk chunk = Mockito.mock(FullChunk.class);
        Level level = Mockito.mock(Level.class);
        LevelProvider provider = Mockito.mock(LevelProvider.class);
        Mockito.when(chunk.getProvider()).thenReturn(provider);
        Mockito.when(provider.getLevel()).thenReturn(level);
        Mockito.when(level.getServer()).thenReturn(MockServer.get());

        CompoundTag nbt = new CompoundTag()
                .putInt("x", 0)
                .putInt("y", 0)
                .putInt("z", 0)
                .putByte("State", 1)
                .putByte("NewState", 1)
                .putBoolean("Sticky", false)
                .putBoolean("Extending", true)
                .putBoolean("powered", true)
                .putInt("facing", BlockFace.UP.getIndex());

        BlockEntityPistonArm arm = new BlockEntityPistonArm(chunk, nbt);
        arm.setLevel(level);
        arm.progress = 0.5f;
        arm.lastProgress = 0.0f;

        Player player = Mockito.mock(Player.class);

        invokeMoveEntity(arm, player, BlockFace.UP);

        Mockito.verify(player, Mockito.never()).onPushByPiston(Mockito.any(), Mockito.any());
        Mockito.verify(player, Mockito.never()).move(Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble());
    }






    @Test
    void movingBlockRestoresOriginalBlockWhenPistonChunkIsUnavailable() {
        MockServer.init();

        FullChunk chunk = Mockito.mock(FullChunk.class);
        Level level = Mockito.mock(Level.class);
        LevelProvider provider = Mockito.mock(LevelProvider.class);
        Mockito.when(chunk.getProvider()).thenReturn(provider);
        Mockito.when(provider.getLevel()).thenReturn(level);
        Mockito.when(level.getServer()).thenReturn(MockServer.get());
        Mockito.when(level.isChunkLoaded(0, 0)).thenReturn(false);
        Mockito.when(level.setBlock(Mockito.any(BlockEntityMovingBlock.class), Mockito.any(Block.class), Mockito.eq(true), Mockito.eq(true))).thenReturn(true);

        CompoundTag nbt = new CompoundTag()
                .putInt("x", 0)
                .putInt("y", 0)
                .putInt("z", 0)
                .putBoolean("expanding", true)
                .putCompound("movingBlock", new CompoundTag()
                        .putInt("id", Block.STONE)
                        .putInt("meta", 0))
                .putInt("pistonPosX", 0)
                .putInt("pistonPosY", 0)
                .putInt("pistonPosZ", 0);

        BlockEntityMovingBlock movingBlock = Mockito.spy(new BlockEntityMovingBlock(chunk, nbt));
        Mockito.doNothing().when(movingBlock).close();
        movingBlock.setLevel(level);

        Assertions.assertFalse(movingBlock.onUpdate());
        Mockito.verify(level).setBlock(
                Mockito.eq(movingBlock),
                Mockito.argThat(block -> block.getId() == Block.STONE),
                Mockito.eq(true),
                Mockito.eq(true)
        );
    }

    @Test
    void movingBlockRestoresOriginalBlockWhenPistonEntityIsMissing() {
        MockServer.init();

        FullChunk chunk = Mockito.mock(FullChunk.class);
        Level level = Mockito.mock(Level.class);
        LevelProvider provider = Mockito.mock(LevelProvider.class);
        Mockito.when(chunk.getProvider()).thenReturn(provider);
        Mockito.when(provider.getLevel()).thenReturn(level);
        Mockito.when(level.getServer()).thenReturn(MockServer.get());
        Mockito.when(level.isChunkLoaded(0, 0)).thenReturn(true);
        Mockito.when(level.getBlockEntity(Mockito.any(BlockVector3.class))).thenReturn(null);
        Mockito.when(level.setBlock(Mockito.any(BlockEntityMovingBlock.class), Mockito.any(Block.class), Mockito.eq(true), Mockito.eq(true))).thenReturn(true);

        CompoundTag nbt = new CompoundTag()
                .putInt("x", 0)
                .putInt("y", 0)
                .putInt("z", 0)
                .putBoolean("expanding", true)
                .putCompound("movingBlock", new CompoundTag()
                        .putInt("id", Block.STONE)
                        .putInt("meta", 0))
                .putInt("pistonPosX", 0)
                .putInt("pistonPosY", 0)
                .putInt("pistonPosZ", 0);

        BlockEntityMovingBlock movingBlock = Mockito.spy(new BlockEntityMovingBlock(chunk, nbt));
        Mockito.doNothing().when(movingBlock).close();
        movingBlock.setLevel(level);

        Assertions.assertFalse(movingBlock.onUpdate());
        Mockito.verify(level).setBlock(
                Mockito.eq(movingBlock),
                Mockito.argThat(block -> block.getId() == Block.STONE),
                Mockito.eq(true),
                Mockito.eq(true)
        );
    }

    @Test
    void movingBlockRestoresOriginalBlockWhenNoLongerTrackedByPistonArm() {
        MockServer.init();

        FullChunk chunk = Mockito.mock(FullChunk.class);
        Level level = Mockito.mock(Level.class);
        LevelProvider provider = Mockito.mock(LevelProvider.class);
        Mockito.when(chunk.getProvider()).thenReturn(provider);
        Mockito.when(provider.getLevel()).thenReturn(level);
        Mockito.when(level.getServer()).thenReturn(MockServer.get());
        Mockito.when(level.isChunkLoaded(0, 0)).thenReturn(true);
        Mockito.when(level.setBlock(Mockito.any(BlockEntityMovingBlock.class), Mockito.any(Block.class), Mockito.eq(true), Mockito.eq(true))).thenReturn(true);

        CompoundTag armNbt = new CompoundTag()
                .putInt("x", 0)
                .putInt("y", 0)
                .putInt("z", 0)
                .putByte("State", 1)
                .putByte("NewState", 1)
                .putBoolean("Sticky", false)
                .putBoolean("Extending", true)
                .putBoolean("powered", true)
                .putInt("facing", BlockFace.EAST.getIndex());

        BlockEntityPistonArm arm = new BlockEntityPistonArm(chunk, armNbt);
        arm.setLevel(level);
        arm.facing = BlockFace.EAST;
        arm.extending = true;
        arm.attachedBlocks = new ArrayList<>(List.of(new BlockVector3(5, 0, 0)));

        Mockito.when(level.getBlockEntity(Mockito.any(BlockVector3.class))).thenReturn(arm);

        CompoundTag movingNbt = new CompoundTag()
                .putInt("x", 2)
                .putInt("y", 0)
                .putInt("z", 0)
                .putBoolean("expanding", true)
                .putCompound("movingBlock", new CompoundTag()
                        .putInt("id", Block.STONE)
                        .putInt("meta", 0))
                .putInt("pistonPosX", 0)
                .putInt("pistonPosY", 0)
                .putInt("pistonPosZ", 0);

        BlockEntityMovingBlock movingBlock = Mockito.spy(new BlockEntityMovingBlock(chunk, movingNbt));
        Mockito.doNothing().when(movingBlock).close();
        movingBlock.setLevel(level);

        Assertions.assertFalse(movingBlock.onUpdate());
        Mockito.verify(level).setBlock(
                Mockito.eq(movingBlock),
                Mockito.argThat(block -> block.getId() == Block.STONE),
                Mockito.eq(true),
                Mockito.eq(true)
        );
    }


    @Test
    void observerPulseCanDriveDispenserAcrossScheduledTicks() {
        LevelFixture fixture = new LevelFixture();

        BlockObserver observer = new BlockObserver();
        observer.setLevel(fixture.level);
        observer.x = 0;
        observer.y = 0;
        observer.z = 0;
        observer.setBlockFace(BlockFace.WEST);

        TestDispenser dispenser = new TestDispenser();
        dispenser.setLevel(fixture.level);
        dispenser.x = 1;
        dispenser.y = 0;
        dispenser.z = 0;

        fixture.registerBlock(observer);
        fixture.registerBlock(dispenser);
        fixture.setPowered(dispenser, true);

        observer.onUpdate(Level.BLOCK_UPDATE_SCHEDULED);
        dispenser.onUpdate(Level.BLOCK_UPDATE_REDSTONE);
        dispenser.onUpdate(Level.BLOCK_UPDATE_SCHEDULED);
        fixture.setPowered(dispenser, false);
        observer.onUpdate(Level.BLOCK_UPDATE_SCHEDULED);
        dispenser.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertEquals(1, dispenser.dispenseCalls);
        Assertions.assertFalse(dispenser.isTriggered());
    }

    @Test
    void redstoneLampTransitionsFireUpdateEventsOnPowerRiseAndFall() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        BlockRedstoneLamp lamp = new BlockRedstoneLamp();
        lamp.setLevel(fixture.level);
        lamp.x = 4;
        lamp.y = 0;
        lamp.z = 0;

        fixture.setPowered(lamp, true);
        lamp.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        BlockRedstoneLampLit litLamp = new BlockRedstoneLampLit();
        litLamp.setLevel(fixture.level);
        litLamp.x = 4;
        litLamp.y = 0;
        litLamp.z = 0;

        fixture.setPowered(litLamp, false);
        litLamp.onUpdate(Level.BLOCK_UPDATE_SCHEDULED);

        Assertions.assertEquals(
                List.of(RedstoneUpdateEvent.class, RedstoneUpdateEvent.class),
                fixture.eventTypes()
        );
    }

    @Test
    void lightningRodActivationAndScheduledResetFireExpectedRedstoneTransitions() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();
        BlockLightningRod rod = new BlockLightningRod();
        rod.setLevel(fixture.level);
        rod.x = 0;
        rod.y = 0;
        rod.z = 0;
        rod.setBlockFace(BlockFace.EAST);

        Assertions.assertTrue(rod.activatePower(4));
        Assertions.assertTrue(rod.isPowered());
        Assertions.assertTrue(fixture.scheduledUpdates.contains("0:0:0:4"));
        Assertions.assertEquals(List.of("0->15"), fixture.redstoneTransitions());
        Assertions.assertTrue(fixture.updatesAt(-1, 0, 0) > 0);

        rod.onUpdate(Level.BLOCK_UPDATE_SCHEDULED);

        Assertions.assertFalse(rod.isPowered());
        Assertions.assertEquals(List.of("0->15", "15->0"), fixture.redstoneTransitions());
        Assertions.assertTrue(fixture.updatesAt(-2, 0, 0) > 0);
    }

    @Test
    void breakingPoweredLightningRodFansOutRearNeighborUpdates() {
        LevelFixture fixture = new LevelFixture();
        BlockLightningRod rod = new BlockLightningRod();
        rod.setLevel(fixture.level);
        rod.x = 1;
        rod.y = 0;
        rod.z = 1;
        rod.setBlockFace(BlockFace.EAST);
        rod.setPowered(true);

        Assertions.assertTrue(rod.onBreak(Item.get(Block.AIR)));
        Assertions.assertTrue(fixture.updatesAt(0, 0, 1) > 0);
        Assertions.assertTrue(fixture.updatesAt(-1, 0, 1) > 0);
    }

    @Test
    void leverRepeaterLampIntegrationPreservesExpectedScheduledFlow() {
        MockServer.init();

        LevelFixture fixture = new LevelFixture();

        BlockLever lever = new BlockLever(1);
        lever.setLevel(fixture.level);
        lever.x = 0;
        lever.y = 0;
        lever.z = 0;

        TestRepeater repeater = new TestRepeater();
        repeater.setLevel(fixture.level);
        repeater.x = 1;
        repeater.y = 0;
        repeater.z = 0;
        repeater.setDamage(BlockFace.WEST.getHorizontalIndex());

        BlockRedstoneLamp lamp = new BlockRedstoneLamp();
        lamp.setLevel(fixture.level);
        lamp.x = 2;
        lamp.y = 0;
        lamp.z = 0;

        fixture.registerBlock(lever);
        fixture.registerBlock(repeater);
        fixture.registerBlock(lamp);
        fixture.setPowered(repeater, true);
        fixture.setPowered(lamp, true);

        lever.onActivate(Item.get(Item.AIR), null);
        repeater.onUpdate(Level.BLOCK_UPDATE_REDSTONE);
        repeater.onUpdate(Level.BLOCK_UPDATE_SCHEDULED);
        lamp.onUpdate(Level.BLOCK_UPDATE_REDSTONE);

        Assertions.assertTrue(lever.isPowerOn());
        Assertions.assertTrue(repeater.isPowered());
        Assertions.assertTrue(
                fixture.eventTypes().contains(BlockRedstoneEvent.class)
                        || fixture.eventTypes().contains(RedstoneUpdateEvent.class)
        );
    }

    private static final class CountingBlock extends Block {
        private int updates;

        private CountingBlock(Level level, Vector3 pos) {
            this.level = level;
            this.x = pos.getFloorX();
            this.y = pos.getFloorY();
            this.z = pos.getFloorZ();
        }

        @Override
        public String getName() {
            return "Counting";
        }

        @Override
        public int getId() {
            return AIR;
        }

        @Override
        public int onUpdate(int type) {
            this.updates++;
            return type;
        }
    }

    private static final class LevelFixture {
        private final Level level = Mockito.mock(Level.class);
        private final Map<String, CountingBlock> blocks = new HashMap<>();
        private final Map<String, Block> placedBlocks = new HashMap<>();
        private final Map<String, Boolean> poweredSides = new HashMap<>();
        private final Map<String, Boolean> poweredBlocks = new HashMap<>();
        private final List<String> scheduledUpdates = new ArrayList<>();
        private final List<Event> events = new ArrayList<>();

        private LevelFixture() {
            Server server = MockServer.get();
            PluginManager pluginManager = Mockito.mock(PluginManager.class);
            Mockito.when(level.getServer()).thenReturn(server);
            Mockito.when(server.getPluginManager()).thenReturn(pluginManager);
            Mockito.doAnswer(invocation -> {
                events.add(invocation.getArgument(0));
                return null;
            }).when(pluginManager).callEvent(Mockito.any(Event.class));
            Mockito.when(level.setBlock(Mockito.any(Block.class), Mockito.any(Block.class))).thenReturn(true);
            Mockito.when(level.setBlock(Mockito.any(Vector3.class), Mockito.any(Block.class), Mockito.anyBoolean(), Mockito.anyBoolean())).thenReturn(true);
            Mockito.when(level.setBlock(Mockito.any(Block.class), Mockito.any(Block.class), Mockito.anyBoolean(), Mockito.anyBoolean())).thenReturn(true);
            Mockito.doAnswer(invocation -> {
                int x = invocation.getArgument(0);
                int y = invocation.getArgument(1);
                int z = invocation.getArgument(2);
                int data = invocation.getArgument(3);
                Block placed = placedBlocks.get(key(x, y, z));
                if (placed != null) {
                    placed.setDamage(data);
                } else {
                    getOrCreate(x, y, z).setDamage(data);
                }
                return null;
            }).when(level).setBlockDataAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt());
            Mockito.doAnswer(invocation -> {
                Block block = invocation.getArgument(0);
                scheduledUpdates.add(key(block.getFloorX(), block.getFloorY(), block.getFloorZ()) + ":" + invocation.getArgument(1));
                return null;
            }).when(level).scheduleUpdate(Mockito.any(Block.class), Mockito.anyInt());
            Mockito.doAnswer(invocation -> {
                Vector3 pos = invocation.getArgument(1);
                scheduledUpdates.add(key(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ()) + ":" + invocation.getArgument(2));
                return null;
            }).when(level).scheduleUpdate(Mockito.any(Block.class), Mockito.any(Block.class), Mockito.anyInt());
            Mockito.doNothing().when(level).scheduleUpdate(Mockito.any(Block.class), Mockito.any(Block.class), Mockito.anyInt(), Mockito.anyInt());
            Mockito.when(level.isBlockTickPending(Mockito.any(Block.class), Mockito.any(Block.class))).thenReturn(false);
            Mockito.when(level.getBlock(Mockito.any(Vector3.class))).thenAnswer(vectorLookup());
            Mockito.when(level.getBlock(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenAnswer(intLookup());
            Mockito.when(level.getCollidingEntities(Mockito.any())).thenReturn(new Entity[0]);
            Mockito.doNothing().when(level).addChunkPacket(Mockito.anyInt(), Mockito.anyInt(), Mockito.any());
            Mockito.doNothing().when(level).updateAroundObserver(Mockito.any(Vector3.class));
            Mockito.when(level.isSidePowered(Mockito.any(Vector3.class), Mockito.any(BlockFace.class))).thenAnswer(sidePoweredLookup());
            Mockito.when(level.isBlockPowered(Mockito.any(Vector3.class))).thenAnswer(invocation -> {
                Vector3 pos = invocation.getArgument(0);
                return poweredBlocks.getOrDefault(key(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ()), false);
            });
        }

        private Answer<Block> vectorLookup() {
            return invocation -> {
                Vector3 pos = invocation.getArgument(0);
                Block placed = placedBlocks.get(key(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ()));
                if (placed != null) {
                    return placed;
                }
                return getOrCreate(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ());
            };
        }

        private Answer<Block> intLookup() {
            return invocation -> {
                int x = invocation.getArgument(0);
                int y = invocation.getArgument(1);
                int z = invocation.getArgument(2);
                Block placed = placedBlocks.get(key(x, y, z));
                if (placed != null) {
                    return placed;
                }
                return getOrCreate(x, y, z);
            };
        }

        private Answer<Boolean> sidePoweredLookup() {
            return invocation -> {
                Vector3 pos = invocation.getArgument(0);
                BlockFace face = invocation.getArgument(1);
                return poweredSides.getOrDefault(pos.getFloorX() + ":" + pos.getFloorY() + ":" + pos.getFloorZ() + ":" + face.getIndex(), false);
            };
        }

        private CountingBlock getOrCreate(int x, int y, int z) {
            return blocks.computeIfAbsent(key(x, y, z), ignored -> new CountingBlock(level, new Vector3(x, y, z)));
        }

        private int updatesAt(int x, int y, int z) {
            CountingBlock block = blocks.get(key(x, y, z));
            return block == null ? 0 : block.updates;
        }

        private int totalUpdates() {
            return blocks.values().stream().mapToInt(block -> block.updates).sum();
        }

        private void setPowered(Vector3 pos, boolean powered) {
            poweredBlocks.put(key(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ()), powered);
        }

        private void registerBlock(Block block) {
            placedBlocks.put(key(block.getFloorX(), block.getFloorY(), block.getFloorZ()), block);
        }

        private List<Class<? extends Event>> eventTypes() {
            return events.stream().map(Event::getClass).toList();
        }

        private List<String> redstoneTransitions() {
            List<String> transitions = new ArrayList<>();
            for (Event event : events) {
                if (event instanceof BlockRedstoneEvent redstoneEvent) {
                    transitions.add(redstoneEvent.getOldPower() + "->" + redstoneEvent.getNewPower());
                }
            }
            return transitions;
        }

        private String key(int x, int y, int z) {
            return x + ":" + y + ":" + z;
        }
    }

    private static final class StatefulLevelFixture {
        private final Level level = Mockito.mock(Level.class);
        private final Map<String, Block> placedBlocks = new HashMap<>();
        private final List<String> scheduledUpdates = new ArrayList<>();
        private final List<Event> events = new ArrayList<>();

        private StatefulLevelFixture() {
            Server server = MockServer.get();
            PluginManager pluginManager = Mockito.mock(PluginManager.class);
            Mockito.when(level.getServer()).thenReturn(server);
            Mockito.when(server.getPluginManager()).thenReturn(pluginManager);
            Mockito.doAnswer(invocation -> {
                events.add(invocation.getArgument(0));
                return null;
            }).when(pluginManager).callEvent(Mockito.any(Event.class));
            Mockito.doAnswer(invocation -> {
                Block pos = invocation.getArgument(0);
                Block block = invocation.getArgument(1);
                return placeBlock(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ(), block);
            }).when(level).setBlock(Mockito.any(Block.class), Mockito.any(Block.class));
            Mockito.doAnswer(invocation -> {
                Vector3 pos = invocation.getArgument(0);
                Block block = invocation.getArgument(1);
                return placeBlock(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ(), block);
            }).when(level).setBlock(Mockito.any(Vector3.class), Mockito.any(Block.class), Mockito.anyBoolean(), Mockito.anyBoolean());
            Mockito.doAnswer(invocation -> {
                Block pos = invocation.getArgument(0);
                Block block = invocation.getArgument(1);
                return placeBlock(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ(), block);
            }).when(level).setBlock(Mockito.any(Block.class), Mockito.any(Block.class), Mockito.anyBoolean(), Mockito.anyBoolean());
            Mockito.doAnswer(invocation -> {
                int x = invocation.getArgument(0);
                int y = invocation.getArgument(1);
                int z = invocation.getArgument(2);
                int data = invocation.getArgument(3);
                Block placed = placedBlocks.get(key(x, y, z));
                if (placed != null) {
                    placed.setDamage(data);
                }
                return null;
            }).when(level).setBlockDataAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt());
            Mockito.doAnswer(invocation -> {
                Block block = invocation.getArgument(0);
                scheduledUpdates.add(key(block.getFloorX(), block.getFloorY(), block.getFloorZ()) + ":" + invocation.getArgument(1));
                return null;
            }).when(level).scheduleUpdate(Mockito.any(Block.class), Mockito.anyInt());
            Mockito.doAnswer(invocation -> {
                Vector3 pos = invocation.getArgument(1);
                scheduledUpdates.add(key(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ()) + ":" + invocation.getArgument(2));
                return null;
            }).when(level).scheduleUpdate(Mockito.any(Block.class), Mockito.any(Block.class), Mockito.anyInt());
            Mockito.doNothing().when(level).scheduleUpdate(Mockito.any(Block.class), Mockito.any(Block.class), Mockito.anyInt(), Mockito.anyInt());
            Mockito.when(level.isBlockTickPending(Mockito.any(Block.class), Mockito.any(Block.class))).thenReturn(false);
            Mockito.when(level.getBlock(Mockito.any(Vector3.class))).thenAnswer(invocation -> {
                Vector3 pos = invocation.getArgument(0);
                return placedBlocks.getOrDefault(key(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ()), airAt(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ()));
            });
            Mockito.when(level.getBlock(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenAnswer(invocation -> {
                int x = invocation.getArgument(0);
                int y = invocation.getArgument(1);
                int z = invocation.getArgument(2);
                return placedBlocks.getOrDefault(key(x, y, z), airAt(x, y, z));
            });
            Mockito.when(level.getBlockIdAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenAnswer(invocation -> {
                int x = invocation.getArgument(0);
                int y = invocation.getArgument(1);
                int z = invocation.getArgument(2);
                Block placed = placedBlocks.get(key(x, y, z));
                return placed == null ? Block.AIR : placed.getId();
            });
            Mockito.when(level.getBlockDataAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenAnswer(invocation -> {
                int x = invocation.getArgument(0);
                int y = invocation.getArgument(1);
                int z = invocation.getArgument(2);
                Block placed = placedBlocks.get(key(x, y, z));
                return placed == null ? 0 : placed.getDamage();
            });
            Mockito.when(level.getCollidingEntities(Mockito.any())).thenReturn(new Entity[0]);
            Mockito.doNothing().when(level).addChunkPacket(Mockito.anyInt(), Mockito.anyInt(), Mockito.any());
            Mockito.doNothing().when(level).updateAroundObserver(Mockito.any(Vector3.class));
            Mockito.when(level.isSidePowered(Mockito.any(Vector3.class), Mockito.any(BlockFace.class))).thenReturn(false);
            Mockito.when(level.isBlockPowered(Mockito.any(Vector3.class))).thenReturn(false);
        }

        private void registerBlock(Block block) {
            placeBlock(block.getFloorX(), block.getFloorY(), block.getFloorZ(), block);
        }

        private boolean placeBlock(int x, int y, int z, Block block) {
            block.setLevel(level);
            block.x = x;
            block.y = y;
            block.z = z;
            placedBlocks.put(key(x, y, z), block);
            return true;
        }

        private Block airAt(int x, int y, int z) {
            BlockAir air = new BlockAir();
            air.setLevel(level);
            air.x = x;
            air.y = y;
            air.z = z;
            return air;
        }

        private String key(int x, int y, int z) {
            return x + ":" + y + ":" + z;
        }
    }

    private static final class TestRepeater extends BlockRedstoneRepeaterUnpowered {
        @Override
        protected boolean shouldBePowered() {
            return true;
        }
    }

    private static class TestConfigurableRepeater extends BlockRedstoneRepeaterUnpowered {
        private boolean shouldBePowered;

        void setShouldBePowered(boolean shouldBePowered) {
            this.shouldBePowered = shouldBePowered;
        }

        @Override
        protected boolean shouldBePowered() {
            return shouldBePowered;
        }
    }

    private static final class TestLockedRepeater extends TestConfigurableRepeater {
        @Override
        public boolean isLocked() {
            return true;
        }
    }

    private static final class TestComparator extends BlockRedstoneComparatorPowered {
        @Override
        protected int calculateInputStrength() {
            return 12;
        }

        @Override
        protected int getPowerOnSides() {
            return 0;
        }
    }

    private static final class TestConfigurableComparator extends BlockRedstoneComparatorPowered {
        private int inputStrength;
        private int sidePower;

        @Override
        protected int calculateInputStrength() {
            return inputStrength;
        }

        @Override
        protected int getPowerOnSides() {
            return sidePower;
        }
    }

    private static final class TestPressurePlate extends BlockPressurePlateStone {
        private int nextStrength;

        @Override
        protected int computeRedstoneStrength() {
            return nextStrength;
        }
    }

    private static final class TestDispenser extends BlockDispenser {
        private int dispenseCalls;

        @Override
        public void dispense() {
            dispenseCalls++;
        }
    }

    private static final class TestDropper extends BlockDropper {
        private int dispenseCalls;

        @Override
        public void dispense() {
            dispenseCalls++;
        }

        boolean isTriggered() {
            return triggered;
        }
    }

    private static final class TestCrafter extends BlockCrafter {
        private int craftCalls;

        public boolean craft() {
            craftCalls++;
            return true;
        }
    }

    private static final class TestPiston extends BlockPiston {
        private boolean checkStateResult = true;
        private int checkStateCalls;
        private boolean poweredState;

        void setCheckStateResult(boolean checkStateResult) {
            this.checkStateResult = checkStateResult;
        }

        void setPoweredState(boolean poweredState) {
            this.poweredState = poweredState;
        }

        protected boolean checkState(Boolean isPowered) {
            checkStateCalls++;
            return checkStateResult;
        }

        protected boolean isPowered() {
            return poweredState;
        }
    }

    private static final class TestStickyPiston extends BlockPistonSticky {
    }

    private static final class TestTripWire extends BlockTripWire {
        private int updateHookCalls;
        private boolean lastScheduleUpdateFlag;

        @Override
        public void updateHook(boolean scheduleUpdate) {
            updateHookCalls++;
            lastScheduleUpdateFlag = scheduleUpdate;
        }
    }

    private static final class TestNoteblock extends BlockNoteblock {
        private int emitCalls;

        @Override
        public void emitSound() {
            emitCalls++;
        }
    }

    private static final class TestBell extends BlockBell {
        private int ringCalls;
        private boolean poweredByWire;

        @Override
        public boolean ring(Entity causeEntity, BellRingEvent.RingCause cause, BlockFace hitFace) {
            ringCalls++;
            return true;
        }

        @Override
        public boolean isGettingPower() {
            boolean powered = super.isGettingPower();
            poweredByWire |= powered;
            return powered;
        }

        private boolean wasPoweredByWire() {
            return poweredByWire;
        }
    }

    private static final class TestTNT extends BlockTNT {
        private int primeCalls;

        @Override
        public void prime() {
            primeCalls++;
        }
    }

    private static final class TestUnpullableBlock extends Block {
        @Override
        public String getName() {
            return "Unpullable";
        }

        @Override
        public int getId() {
            return OBSIDIAN;
        }

        @Override
        public boolean canBePulled() {
            return false;
        }
    }

    private static final class TestRedstoneTorch extends BlockRedstoneTorch {
        private int redstoneUpdates;

        private TestRedstoneTorch(int meta) {
            super(meta);
        }

        @Override
        public int onUpdate(int type) {
            if (type == Level.BLOCK_UPDATE_REDSTONE) {
                redstoneUpdates++;
            }
            return type;
        }
    }

    private static final class TestUnlitRedstoneTorch extends BlockRedstoneTorchUnlit {
        private int redstoneUpdates;

        private TestUnlitRedstoneTorch(int meta) {
            super(meta);
        }

        @Override
        public int onUpdate(int type) {
            if (type == Level.BLOCK_UPDATE_REDSTONE) {
                redstoneUpdates++;
            }
            return type;
        }
    }

    private static final class TestMovedBlock extends Block {
        private int movedUpdates;

        @Override
        public String getName() {
            return "Moved";
        }

        @Override
        public int getId() {
            return STONE;
        }

        @Override
        public int onUpdate(int type) {
            if (type == Level.BLOCK_UPDATE_MOVED) {
                movedUpdates++;
            }
            return type;
        }
    }

    private static void invokeMoveEntity(BlockEntityPistonArm arm, Entity entity, BlockFace face) {
        try {
            Method moveEntity = BlockEntityPistonArm.class.getDeclaredMethod("moveEntity", Entity.class, BlockFace.class);
            moveEntity.setAccessible(true);
            moveEntity.invoke(arm, entity, face);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}

