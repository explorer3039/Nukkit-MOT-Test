package cn.nukkit.level.generator.biome.result;

public abstract class BiomeResult {
    protected int biomeId;

    protected BiomeResult(int biomeId) {
        this.biomeId = biomeId;
    }

    public int biomeId() {
        return biomeId;
    }
}
