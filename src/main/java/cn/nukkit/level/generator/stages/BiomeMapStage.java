package cn.nukkit.level.generator.stages;

import cn.nukkit.level.generator.BiomedGenerator;
import cn.nukkit.level.generator.ChunkGenerateContext;
import cn.nukkit.level.generator.GenerateStage;
import cn.nukkit.level.generator.biome.BiomePicker;
import cn.nukkit.level.generator.biome.result.BiomeResult;

public class BiomeMapStage extends GenerateStage {
    public static final String NAME = "biome";
    private BiomePicker<? extends BiomeResult> biomePicker;

    @Override
    public void apply(ChunkGenerateContext context) {
        if (!(context.getGenerator() instanceof BiomedGenerator biomedGenerator)) {
            return;
        }
        if (biomePicker == null) {
            biomePicker = biomedGenerator.createBiomePicker(context.getLevel());
        }
        int chunkX = context.getChunkX() << 4;
        int chunkZ = context.getChunkZ() << 4;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                BiomeResult result = biomePicker.pick(chunkX + x, 64, chunkZ + z);
                context.getChunk().setBiomeId(x, z, result.biomeId());
            }
        }
    }

    @Override
    public String name() {
        return NAME;
    }
}
