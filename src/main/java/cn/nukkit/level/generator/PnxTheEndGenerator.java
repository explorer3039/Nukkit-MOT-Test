package cn.nukkit.level.generator;

import cn.nukkit.level.Level;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.generator.biome.BiomePicker;
import cn.nukkit.level.generator.biome.TheEndBiomePicker;
import cn.nukkit.level.generator.biome.result.BiomeResult;

import java.util.Map;

public class PnxTheEndGenerator extends PnxAliasGenerator implements BiomedGenerator {
    public PnxTheEndGenerator() {
        this(Map.of());
    }

    public PnxTheEndGenerator(Map<String, Object> options) {
        super(options);
    }

    @Override
    protected Generator createDelegate(Map<String, Object> options) {
        return new End(options);
    }

    @Override
    protected String aliasName() {
        return "the_end";
    }

    @Override
    public int getId() {
        return TYPE_THE_END;
    }

    @Override
    public int getDimension() {
        return Level.DIMENSION_THE_END;
    }

    @Override
    public String getName() {
        return "pnx_the_end";
    }

    @Override
    public BiomePicker<? extends BiomeResult> createBiomePicker(ChunkManager level) {
        return new TheEndBiomePicker();
    }
}
