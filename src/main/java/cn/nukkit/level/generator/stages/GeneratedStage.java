package cn.nukkit.level.generator.stages;

import cn.nukkit.level.generator.ChunkGenerateContext;
import cn.nukkit.level.generator.GenerateStage;

public class GeneratedStage extends GenerateStage {
    public static final String NAME = "generated";

    @Override
    public void apply(ChunkGenerateContext context) {
        context.getChunk().setGenerated();
    }

    @Override
    public String name() {
        return NAME;
    }
}
