package cn.nukkit.level.generator;

import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.level.generator.pnx.PnxStagePipeline;
import cn.nukkit.math.NukkitRandom;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * PNX-style stage-driven generator base adapted to MOT's legacy generator lifecycle.
 */
public abstract class PopulatedGenerator extends Generator {
    protected ChunkManager level;
    protected NukkitRandom random;
    private final Map<String, Object> options;
    private PnxStagePipeline stagePipeline;

    protected PopulatedGenerator(Map<String, Object> options) {
        this.options = options == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(options));
    }

    protected abstract void stages(PnxStagePipeline.Builder builder);

    protected void initGenerator(ChunkManager level, NukkitRandom random) {
    }

    @Override
    public final void init(ChunkManager level, NukkitRandom random) {
        this.level = level;
        this.random = random;
        this.initGenerator(level, random);

        PnxStagePipeline.Builder builder = new PnxStagePipeline.Builder();
        this.stages(builder);
        this.stagePipeline = builder.build();
    }

    @Override
    public final void generateChunk(int chunkX, int chunkZ) {
        stagePipeline.runTerrain(new ChunkGenerateContext(this, level, chunkX, chunkZ));
    }

    @Override
    public final void populateChunk(int chunkX, int chunkZ) {
        stagePipeline.runPopulation(new ChunkGenerateContext(this, level, chunkX, chunkZ));
    }

    @Override
    public final void populateStructure(int chunkX, int chunkZ) {
        stagePipeline.runStructure(new ChunkGenerateContext(this, level, chunkX, chunkZ));
    }

    @Override
    public final Map<String, Object> getSettings() {
        return options;
    }

    @Override
    public final ChunkManager getChunkManager() {
        return level;
    }

    protected final BaseFullChunk chunk(int chunkX, int chunkZ) {
        return level.getChunk(chunkX, chunkZ);
    }
}
