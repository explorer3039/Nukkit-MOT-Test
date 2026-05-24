package cn.nukkit.level.generator.biome;

import cn.nukkit.level.generator.biome.result.TheEndBiomeResult;

public class TheEndBiomePicker extends BiomePicker<TheEndBiomeResult> {
    private static final TheEndBiomeResult RESULT = new TheEndBiomeResult();

    @Override
    public TheEndBiomeResult pick(int x, int y, int z) {
        return RESULT;
    }
}
