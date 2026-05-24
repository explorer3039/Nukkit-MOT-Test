package cn.nukkit.level.generator.biome;

import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.biome.BiomeSelector;
import cn.nukkit.level.generator.biome.result.OverworldBiomeResult;
import cn.nukkit.math.NukkitRandom;

public class OverworldBiomePicker extends BiomePicker<OverworldBiomeResult> {
    private final BiomeSelector selector;

    public OverworldBiomePicker(ChunkManager level) {
        this.selector = new BiomeSelector(new NukkitRandom(level.getSeed()));
    }

    @Override
    public OverworldBiomeResult pick(int x, int y, int z) {
        return new OverworldBiomeResult(selector.pickBiome(x, z).getId());
    }
}
