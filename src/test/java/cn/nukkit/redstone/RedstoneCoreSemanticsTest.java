package cn.nukkit.redstone;

import cn.nukkit.MockServer;
import cn.nukkit.block.BlockPiston;
import cn.nukkit.block.BlockRailDetector;
import cn.nukkit.block.BlockRedstoneWire;
import cn.nukkit.level.Level;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.Vector3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RedstoneCoreSemanticsTest {

    @BeforeAll
    static void initServer() {
        MockServer.init();
    }

    @Test
    void redstoneWireOnlyProvidesPowerWhenSignalIsNonZero() {
        BlockRedstoneWire unpowered = new BlockRedstoneWire(0);
        BlockRedstoneWire powered = new BlockRedstoneWire(7);

        Assertions.assertFalse(unpowered.isPowerSource());
        Assertions.assertEquals(0, unpowered.getWeakPower(BlockFace.UP));
        Assertions.assertTrue(powered.isPowerSource());
        Assertions.assertEquals(7, powered.getWeakPower(BlockFace.UP));
        Assertions.assertEquals(7, powered.getStrongPower(BlockFace.UP));
    }

    @Test
    void detectorRailStronglyPowersOnlyUpWhenActive() {
        BlockRailDetector inactive = new BlockRailDetector(0);
        BlockRailDetector active = new BlockRailDetector(8);

        Assertions.assertEquals(0, inactive.getStrongPower(BlockFace.UP));
        Assertions.assertEquals(15, active.getStrongPower(BlockFace.UP));
        Assertions.assertEquals(0, active.getStrongPower(BlockFace.NORTH));
        Assertions.assertTrue(active.hasComparatorInputOverride());
    }

    @Test
    void levelRedstonePowerUsesProvidedBlockInstanceForWeakPower() {
        Level level = Mockito.mock(Level.class, Mockito.CALLS_REAL_METHODS);
        BlockRedstoneWire wire = new BlockRedstoneWire(9);

        Assertions.assertEquals(9, level.getRedstonePower(wire, BlockFace.UP));
        Mockito.verify(level, Mockito.never()).getBlock(Mockito.any(Vector3.class));
    }

    @Test
    void pistonBaseDoesNotContributeAggregatedStrongPower() {
        Level level = Mockito.mock(Level.class, Mockito.CALLS_REAL_METHODS);
        BlockPiston piston = new BlockPiston();
        Mockito.doReturn(piston).when(level).getBlock(Mockito.any(Vector3.class));

        Assertions.assertEquals(0, level.getStrongPower(new Vector3(0, 64, 0)));
    }
}
