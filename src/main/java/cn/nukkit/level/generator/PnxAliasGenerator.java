package cn.nukkit.level.generator;

import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.generator.stages.BiomeMapStage;
import cn.nukkit.level.generator.stages.DelegatedStage;
import cn.nukkit.level.generator.stages.FinishedStage;
import cn.nukkit.level.generator.stages.GeneratedStage;
import cn.nukkit.level.generator.stages.LightPopulationStage;
import cn.nukkit.level.generator.stages.NormalChunkFeatureStage;
import cn.nukkit.level.generator.pnx.PnxStagePipeline;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.math.Vector3;

import java.util.HashMap;
import java.util.Map;

/**
 * Stage-driven wrapper that exposes a PNX-style pipeline while delegating block-by-block behavior to MOT generators.
 */
public abstract class PnxAliasGenerator extends PopulatedGenerator {
    private Generator delegate;

    protected PnxAliasGenerator(Map<String, Object> options) {
        super(options);
    }

    protected abstract Generator createDelegate(Map<String, Object> options);

    protected abstract String aliasName();

    @Override
    protected final void initGenerator(ChunkManager level, NukkitRandom random) {
        this.delegate = createDelegate(new HashMap<>(getSettings()));
        this.delegate.init(level, random);
    }

    @Override
    protected final void stages(PnxStagePipeline.Builder builder) {
        String prefix = "pnx_" + aliasName();
        builder.start(new DelegatedStage(prefix + ":terrain", context -> delegate.generateChunk(context.getChunkX(), context.getChunkZ())));
        builder.next(new BiomeMapStage());
        builder.next(new GeneratedStage());
        builder.next(new DelegatedStage(prefix + ":population", context -> delegate.populateChunk(context.getChunkX(), context.getChunkZ())));
        builder.next(new NormalChunkFeatureStage());
        builder.next(new LightPopulationStage());
        builder.next(new FinishedStage());
        builder.next(new DelegatedStage(prefix + ":structure", context -> delegate.populateStructure(context.getChunkX(), context.getChunkZ())));
        builder.terrainEnd(GeneratedStage.NAME);
        builder.populationEnd(FinishedStage.NAME);
    }

    @Override
    public Vector3 getSpawn() {
        return delegate.getSpawn();
    }

    protected Generator delegate() {
        return delegate;
    }
}
