package cn.nukkit.level.generator.stages.normal;

import cn.nukkit.level.generator.ChunkGenerateContext;
import cn.nukkit.level.generator.GenerateStage;
import cn.nukkit.level.generator.PnxNormalGenerator;

public class NormalPopulatorStage extends GenerateStage {
    public static final String NAME = "normal_populator";

    private final PnxNormalGenerator generator;

    public NormalPopulatorStage(PnxNormalGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void apply(ChunkGenerateContext context) {
        generator.runPopulators(context.getChunkX(), context.getChunkZ());
        context.getChunk().setPopulated();
    }

    @Override
    public String name() {
        return NAME;
    }
}
