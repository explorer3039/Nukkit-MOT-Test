package cn.nukkit.level.generator.stages;

import cn.nukkit.level.generator.ChunkGenerateContext;
import cn.nukkit.level.generator.GenerateStage;

/**
 * Placeholder hook matching PNX stage naming; detailed feature migration lands in follow-up patches.
 */
public class NormalChunkFeatureStage extends GenerateStage {
    public static final String NAME = "feature";

    @Override
    public void apply(ChunkGenerateContext context) {
        context.getChunk().setPopulated();
    }

    @Override
    public String name() {
        return NAME;
    }
}
