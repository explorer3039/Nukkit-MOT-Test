package cn.nukkit.utils;

import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.math.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Common redstone propagation helpers for redstone-aware blocks.
 */
public interface RedstoneComponent {

    default void updateAroundRedstone(@Nullable BlockFace... ignoredFaces) {
        if (this instanceof Position) {
            updateAroundRedstone((Position) this, ignoredFaces);
        }
    }

    default void updateAroundRedstone(@NotNull Set<BlockFace> ignoredFaces) {
        if (this instanceof Position) {
            updateAroundRedstone((Position) this, ignoredFaces);
        }
    }

    static void updateAroundRedstone(@NotNull Position pos, @Nullable BlockFace... ignoredFaces) {
        updateAroundRedstone(pos, toSet(ignoredFaces));
    }

    static void updateAroundRedstone(@NotNull Position pos, @NotNull Set<BlockFace> ignoredFaces) {
        for (BlockFace face : BlockFace.values()) {
            if (ignoredFaces.contains(face)) {
                continue;
            }
            pos.getLevelBlock().getSide(face).onUpdate(Level.BLOCK_UPDATE_REDSTONE);
        }
    }

    default void updateAllAroundRedstone(@Nullable BlockFace... ignoredFaces) {
        if (this instanceof Position) {
            updateAllAroundRedstone((Position) this, ignoredFaces);
        }
    }

    default void updateAllAroundRedstone(@NotNull Set<BlockFace> ignoredFaces) {
        if (this instanceof Position) {
            updateAllAroundRedstone((Position) this, ignoredFaces);
        }
    }

    static void updateAllAroundRedstone(@NotNull Position pos, @Nullable BlockFace... ignoredFaces) {
        updateAllAroundRedstone(pos, toSet(ignoredFaces));
    }

    static void updateAllAroundRedstone(@NotNull Position pos, @NotNull Set<BlockFace> ignoredFaces) {
        updateAroundRedstone(pos, ignoredFaces);

        for (BlockFace face : BlockFace.values()) {
            if (ignoredFaces.contains(face)) {
                continue;
            }
            updateAroundRedstone(pos.getSide(face), face.getOpposite());
        }
    }

    private static Set<BlockFace> toSet(@Nullable BlockFace[] faces) {
        if (faces == null || faces.length == 0) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(faces));
    }
}
