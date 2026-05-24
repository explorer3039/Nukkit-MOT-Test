package cn.nukkit.level.generator.biome;

import cn.nukkit.level.biome.EnumBiome;
import cn.nukkit.level.generator.biome.result.NetherBiomeResult;
import cn.nukkit.level.generator.noise.nukkit.OpenSimplex2S;

public class NetherBiomePicker extends BiomePicker<NetherBiomeResult> {
    private static final double BIOME_AMPLIFICATION = 512;
    private final OpenSimplex2S biomeGen;

    public NetherBiomePicker(long seed) {
        this.biomeGen = new OpenSimplex2S(seed);
    }

    @Override
    public NetherBiomeResult pick(int x, int y, int z) {
        double value = biomeGen.noise2(x / BIOME_AMPLIFICATION, z / BIOME_AMPLIFICATION);
        double secondaryValue = biomeGen.noise3_XZBeforeY(x / (BIOME_AMPLIFICATION * 2d), 0, z / (BIOME_AMPLIFICATION * 2d));
        int biomeId;
        if (value >= 1 / 3f) {
            biomeId = secondaryValue >= 0 ? EnumBiome.WARPED_FOREST.id : EnumBiome.CRIMSON_FOREST.id;
        } else if (value >= -1 / 3f) {
            biomeId = EnumBiome.HELL.id;
        } else {
            biomeId = secondaryValue >= 0 ? EnumBiome.BASALT_DELTAS.id : EnumBiome.SOUL_SAND_VALLEY.id;
        }
        return new NetherBiomeResult(biomeId);
    }
}
