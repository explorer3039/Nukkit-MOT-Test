package cn.nukkit.utils;

import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.math.BlockFace;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Shared redstone neighbor-update helpers without requiring the PNX blockstate stack.
 */
public interface RedstoneComponent {

    default void updateAroundRedstone(@Nullable BlockFace... ignoredFaces) {
        if (ignoredFaces == null) {
            ignoredFaces = new BlockFace[0];
        }
        this.updateAroundRedstone(Sets.newHashSet(ignoredFaces));
    }

    default void updateAroundRedstone(@NotNull Set<BlockFace> ignoredFaces) {
        if (this instanceof Position pos) {
            updateAroundRedstone(pos, ignoredFaces);
        }
    }

    static void updateAroundRedstone(@NotNull Position pos, @Nullable BlockFace... ignoredFaces) {
        if (ignoredFaces == null) {
            ignoredFaces = new BlockFace[0];
        }
        updateAroundRedstone(pos, Sets.newHashSet(ignoredFaces));
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
        if (ignoredFaces == null) {
            ignoredFaces = new BlockFace[0];
        }
        this.updateAllAroundRedstone(Sets.newHashSet(ignoredFaces));
    }

    default void updateAllAroundRedstone(@NotNull Set<BlockFace> ignoredFaces) {
        if (this instanceof Position pos) {
            updateAllAroundRedstone(pos, ignoredFaces);
        }
    }

    static void updateAllAroundRedstone(@NotNull Position pos, @Nullable BlockFace... ignoredFaces) {
        if (ignoredFaces == null) {
            ignoredFaces = new BlockFace[0];
        }
        updateAllAroundRedstone(pos, Sets.newHashSet(ignoredFaces));
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
}
