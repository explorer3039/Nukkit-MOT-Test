package cn.nukkit.level.generator;

import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.generator.biome.BiomePicker;
import cn.nukkit.level.generator.biome.OverworldBiomePicker;
import cn.nukkit.level.generator.biome.result.BiomeResult;

import java.util.Map;

public class PnxNormalGenerator extends PnxAliasGenerator implements BiomedGenerator {
    public PnxNormalGenerator() {
        this(Map.of());
    }

    public PnxNormalGenerator(Map<String, Object> options) {
        super(options);
    }

    @Override
    protected Generator createDelegate(Map<String, Object> options) {
        return new Normal(options);
    }

    @Override
    protected String aliasName() {
        return "normal";
    }

    @Override
    public int getId() {
        return TYPE_INFINITE;
    }

    @Override
    public String getName() {
        return "pnx_normal";
    }

    @Override
    public BiomePicker<? extends BiomeResult> createBiomePicker(ChunkManager level) {
        return new OverworldBiomePicker(level);
    }
}
