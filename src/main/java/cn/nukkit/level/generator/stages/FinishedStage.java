package cn.nukkit.level.generator.stages;

import cn.nukkit.level.generator.ChunkGenerateContext;
import cn.nukkit.level.generator.GenerateStage;

public class FinishedStage extends GenerateStage {
    public static final String NAME = "finished";

    @Override
    public void apply(ChunkGenerateContext context) {
        context.getChunk().setChanged(false);
    }

    @Override
    public String name() {
        return NAME;
    }
}
