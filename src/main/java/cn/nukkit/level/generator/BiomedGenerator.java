package cn.nukkit.level.generator;

import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.generator.biome.BiomePicker;
import cn.nukkit.level.generator.biome.result.BiomeResult;

public interface BiomedGenerator {
    BiomePicker<? extends BiomeResult> createBiomePicker(ChunkManager level);
}
