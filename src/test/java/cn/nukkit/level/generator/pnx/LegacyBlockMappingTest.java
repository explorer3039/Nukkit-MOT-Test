package cn.nukkit.level.generator.pnx;

import cn.nukkit.MockServer;
import cn.nukkit.block.BlockDoor;
import cn.nukkit.block.BlockFenceGate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LegacyBlockMappingTest {

    @BeforeAll
    static void initServer() {
        MockServer.init();
    }

    @Test
    void fullIdPackingRoundTripWorks() {
        int fullId = LegacyBlockMapping.toFullId(5, 7);
        LegacyBlockMapping.LegacyBlockState state = LegacyBlockMapping.fromFullId(fullId);
        Assertions.assertEquals(5, state.id());
        Assertions.assertEquals(7, state.meta());
    }

    @Test
    void stairRotationAndMirrorPreserveUpperHalfBit() {
        int base = 0x01 | 0x04;
        int rotated = LegacyBlockMapping.rotateStairsMeta(base, 1);
        int mirrored = LegacyBlockMapping.mirrorStairsMeta(rotated, LegacyBlockMapping.MirrorAxis.X);

        Assertions.assertEquals(0x04, rotated & 0x04);
        Assertions.assertEquals(0x04, mirrored & 0x04);
    }

    @Test
    void doorBottomRotationAndTopMirrorKeepStateBitsConsistent() {
        int lower = 0x01 | BlockDoor.DOOR_OPEN_BIT;
        int rotatedLower = LegacyBlockMapping.rotateDoorBottomMeta(lower, 2);
        Assertions.assertEquals(BlockDoor.DOOR_OPEN_BIT, rotatedLower & BlockDoor.DOOR_OPEN_BIT);

        int top = BlockDoor.DOOR_TOP_BIT | BlockDoor.DOOR_HINGE_BIT;
        int mirroredTop = LegacyBlockMapping.mirrorDoorTopMeta(top);
        Assertions.assertEquals(BlockDoor.DOOR_TOP_BIT, mirroredTop & BlockDoor.DOOR_TOP_BIT);
        Assertions.assertNotEquals(top, mirroredTop);
    }

    @Test
    void fenceGateAndPortalFrameTransformsPreserveFunctionalBits() {
        int gate = 0x02 | BlockFenceGate.OPEN_BIT;
        int gateRotated = LegacyBlockMapping.rotateFenceGateMeta(gate, 3);
        int gateMirrored = LegacyBlockMapping.mirrorFenceGateMeta(gateRotated, LegacyBlockMapping.MirrorAxis.Z);
        Assertions.assertEquals(BlockFenceGate.OPEN_BIT, gateMirrored & BlockFenceGate.OPEN_BIT);

        int frame = 0x01 | 0x04;
        int frameRotated = LegacyBlockMapping.rotateEndPortalFrameMeta(frame, 1);
        int frameMirrored = LegacyBlockMapping.mirrorEndPortalFrameMeta(frameRotated, LegacyBlockMapping.MirrorAxis.X);
        Assertions.assertEquals(0x04, frameMirrored & 0x04);
    }

    @Test
    void redstoneFacingTransformsPreserveExtraStateBits() {
        int repeaterMeta = 0x03 | 0x08;
        int rotated = LegacyBlockMapping.rotateRedstoneFacingMeta(repeaterMeta, 1);
        int mirrored = LegacyBlockMapping.mirrorRedstoneFacingMeta(rotated, LegacyBlockMapping.MirrorAxis.X);
        Assertions.assertEquals(0x08, mirrored & 0x08);
    }
}
