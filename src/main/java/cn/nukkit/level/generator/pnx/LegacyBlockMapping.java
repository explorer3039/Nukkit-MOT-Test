package cn.nukkit.level.generator.pnx;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockDoor;
import cn.nukkit.block.BlockFenceGate;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.BlockFace;

/**
 * Legacy id/meta helpers used by migrated PNX-style worldgen paths.
 */
public final class LegacyBlockMapping {
    private LegacyBlockMapping() {
    }

    public enum MirrorAxis {
        X, Z
    }

    public record LegacyBlockState(int id, int meta) {
        public int fullId() {
            return toFullId(id, meta);
        }
    }

    public static int toFullId(int id, int meta) {
        return (id << Block.DATA_BITS) | (meta & Block.DATA_MASK);
    }

    public static LegacyBlockState fromFullId(int fullId) {
        return new LegacyBlockState(fullId >> Block.DATA_BITS, fullId & Block.DATA_MASK);
    }

    public static void setBlock(FullChunk chunk, int x, int y, int z, LegacyBlockState state) {
        chunk.setBlock(x, y, z, state.id(), state.meta());
    }

    public static LegacyBlockState resolveAndSetBlock(FullChunk chunk,
                                                      int x,
                                                      int y,
                                                      int z,
                                                      String sourceIdentifier,
                                                      WorldgenDowngradePolicy.Category category,
                                                      LegacyBlockState preferred,
                                                      WorldgenDowngradePolicy policy) {
        LegacyBlockState resolved = policy.resolve(sourceIdentifier, category, preferred);
        setBlock(chunk, x, y, z, resolved);
        return resolved;
    }

    public static LegacyBlockState getBlock(FullChunk chunk, int x, int y, int z) {
        return new LegacyBlockState(chunk.getBlockId(x, y, z), chunk.getBlockData(x, y, z));
    }

    public static int rotateStairsMeta(int meta, int clockwiseQuarterTurns) {
        return rotateHorizontalMeta(meta, 0x03, clockwiseQuarterTurns);
    }

    public static int mirrorStairsMeta(int meta, MirrorAxis axis) {
        return mirrorHorizontalMeta(meta, 0x03, axis);
    }

    public static int rotateDoorBottomMeta(int meta, int clockwiseQuarterTurns) {
        if ((meta & BlockDoor.DOOR_TOP_BIT) != 0) {
            return meta;
        }
        return rotateHorizontalMeta(meta, BlockDoor.DOOR_DIRECTION_BIT, clockwiseQuarterTurns);
    }

    public static int mirrorDoorBottomMeta(int meta, MirrorAxis axis) {
        if ((meta & BlockDoor.DOOR_TOP_BIT) != 0) {
            return meta;
        }
        return mirrorHorizontalMeta(meta, BlockDoor.DOOR_DIRECTION_BIT, axis);
    }

    public static int mirrorDoorTopMeta(int meta) {
        if ((meta & BlockDoor.DOOR_TOP_BIT) == 0) {
            return meta;
        }
        return meta ^ BlockDoor.DOOR_HINGE_BIT;
    }

    public static int rotateFenceGateMeta(int meta, int clockwiseQuarterTurns) {
        return rotateHorizontalMeta(meta, BlockFenceGate.DIRECTIO_BIT, clockwiseQuarterTurns);
    }

    public static int mirrorFenceGateMeta(int meta, MirrorAxis axis) {
        return mirrorHorizontalMeta(meta, BlockFenceGate.DIRECTIO_BIT, axis);
    }

    public static int rotateEndPortalFrameMeta(int meta, int clockwiseQuarterTurns) {
        return rotateHorizontalMeta(meta, 0x03, clockwiseQuarterTurns);
    }

    public static int mirrorEndPortalFrameMeta(int meta, MirrorAxis axis) {
        return mirrorHorizontalMeta(meta, 0x03, axis);
    }

    public static int rotateRedstoneFacingMeta(int meta, int clockwiseQuarterTurns) {
        return rotateHorizontalMeta(meta, 0x03, clockwiseQuarterTurns);
    }

    public static int mirrorRedstoneFacingMeta(int meta, MirrorAxis axis) {
        return mirrorHorizontalMeta(meta, 0x03, axis);
    }

    private static int rotateHorizontalMeta(int meta, int facingMask, int clockwiseQuarterTurns) {
        int turns = Math.floorMod(clockwiseQuarterTurns, 4);
        if (turns == 0) {
            return meta;
        }
        BlockFace face = BlockFace.fromHorizontalIndex(meta & facingMask);
        for (int i = 0; i < turns; i++) {
            face = face.rotateY();
        }
        return (meta & ~facingMask) | (face.getHorizontalIndex() & facingMask);
    }

    private static int mirrorHorizontalMeta(int meta, int facingMask, MirrorAxis axis) {
        BlockFace face = BlockFace.fromHorizontalIndex(meta & facingMask);
        BlockFace mirrored = switch (axis) {
            case X -> switch (face) {
                case EAST -> BlockFace.WEST;
                case WEST -> BlockFace.EAST;
                default -> face;
            };
            case Z -> switch (face) {
                case NORTH -> BlockFace.SOUTH;
                case SOUTH -> BlockFace.NORTH;
                default -> face;
            };
        };
        return (meta & ~facingMask) | (mirrored.getHorizontalIndex() & facingMask);
    }
}
