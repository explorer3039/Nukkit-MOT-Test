package cn.nukkit.level.generator;

import cn.nukkit.level.Level;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.generator.biome.BiomePicker;
import cn.nukkit.level.generator.biome.NetherBiomePicker;
import cn.nukkit.level.generator.biome.result.BiomeResult;

import java.util.Map;

public class PnxNetherGenerator extends PnxAliasGenerator implements BiomedGenerator {
    public PnxNetherGenerator() {
        this(Map.of());
    }

    public PnxNetherGenerator(Map<String, Object> options) {
        super(options);
    }

    @Override
    protected Generator createDelegate(Map<String, Object> options) {
        return new Nether(options);
    }

    @Override
    protected String aliasName() {
        return "nether";
    }

    @Override
    public int getId() {
        return TYPE_NETHER;
    }

    @Override
    public int getDimension() {
        return Level.DIMENSION_NETHER;
    }

    @Override
    public String getName() {
        return "pnx_nether";
    }

    @Override
    public BiomePicker<? extends BiomeResult> createBiomePicker(ChunkManager level) {
        return new NetherBiomePicker(level.getSeed());
    }
}
