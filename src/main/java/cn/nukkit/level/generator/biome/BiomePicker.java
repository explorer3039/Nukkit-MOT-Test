package cn.nukkit.level.generator.biome;

import cn.nukkit.level.generator.biome.result.BiomeResult;

/**
 * PNX-compatible biome picker abstraction.
 */
public abstract class BiomePicker<E extends BiomeResult> {
    public abstract E pick(int x, int y, int z);
}
