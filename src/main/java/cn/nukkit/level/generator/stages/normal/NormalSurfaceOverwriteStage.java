package cn.nukkit.level.generator.stages.normal;

import cn.nukkit.level.generator.ChunkGenerateContext;
import cn.nukkit.level.generator.GenerateStage;
import cn.nukkit.level.generator.PnxNormalGenerator;

public class NormalSurfaceOverwriteStage extends GenerateStage {
    public static final String NAME = "normal_surface_overwrite";

    private final PnxNormalGenerator generator;

    public NormalSurfaceOverwriteStage(PnxNormalGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void apply(ChunkGenerateContext context) {
        generator.runGenerationPopulators(context.getChunkX(), context.getChunkZ());
    }

    @Override
    public String name() {
        return NAME;
    }
}
