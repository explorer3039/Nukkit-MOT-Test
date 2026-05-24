package cn.nukkit.level.generator.biome.result;

public class OverworldBiomeResult extends BiomeResult {
    private final int original;

    public OverworldBiomeResult(int biomeId) {
        super(biomeId);
        this.original = biomeId;
    }

    public OverworldBiomeResult correct(int ignoredHeightDelta) {
        return this;
    }

    public void reset() {
        this.biomeId = original;
    }
}
