package cn.nukkit.level.generator.stages.normal;

import cn.nukkit.level.generator.ChunkGenerateContext;
import cn.nukkit.level.generator.GenerateStage;
import cn.nukkit.level.generator.PnxNormalGenerator;

public class NormalSurfaceDataStage extends GenerateStage {
    public static final String NAME = "normal_surface";

    private final PnxNormalGenerator generator;

    public NormalSurfaceDataStage(PnxNormalGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void apply(ChunkGenerateContext context) {
        int baseX = context.getChunkX() << 4;
        int baseZ = context.getChunkZ() << 4;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                context.getChunk().setBiome(x, z, generator.pickBiome(baseX | x, baseZ | z));
            }
        }
    }

    @Override
    public String name() {
        return NAME;
    }
}
