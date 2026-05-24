package cn.nukkit.level.generator.stages;

import cn.nukkit.level.generator.ChunkGenerateContext;
import cn.nukkit.level.generator.GenerateStage;

public class LightPopulationStage extends GenerateStage {
    public static final String NAME = "light_population";

    @Override
    public void apply(ChunkGenerateContext context) {
        context.getChunk().recalculateHeightMap();
        context.getChunk().populateSkyLight();
        context.getChunk().populateBlockLight();
        context.getChunk().setLightPopulated();
    }

    @Override
    public String name() {
        return NAME;
    }
}
