package cn.nukkit.level.generator.pnx;

import cn.nukkit.level.generator.ChunkGenerateContext;
import cn.nukkit.level.generator.GenerateStage;
import com.google.common.base.Preconditions;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PNX-like stage chain runner split into terrain/population/structure phases.
 */
public final class PnxStagePipeline {
    private final GenerateStage start;
    private final GenerateStage end;
    private final String terrainEnd;
    private final String populationEnd;
    private final Set<Long> generatedChunks = ConcurrentHashMap.newKeySet();
    private final Set<Long> populatedChunks = ConcurrentHashMap.newKeySet();

    private PnxStagePipeline(GenerateStage start, GenerateStage end, String terrainEnd, String populationEnd) {
        this.start = start;
        this.end = end;
        this.terrainEnd = terrainEnd;
        this.populationEnd = populationEnd;
    }

    public void runTerrain(ChunkGenerateContext context) {
        long key = chunkKey(context.getChunkX(), context.getChunkZ());
        if (!generatedChunks.add(key)) {
            return;
        }
        runTo(start, terrainEnd, context);
    }

    public void runPopulation(ChunkGenerateContext context) {
        long key = chunkKey(context.getChunkX(), context.getChunkZ());
        runTerrain(context);
        if (!populatedChunks.add(key)) {
            return;
        }
        GenerateStage startPopulate = afterName(start, terrainEnd);
        runTo(startPopulate, populationEnd, context);
    }

    public void runStructure(ChunkGenerateContext context) {
        GenerateStage startStructure = afterName(start, populationEnd);
        runTo(startStructure, end.name(), context);
    }

    private static void runTo(GenerateStage from, String targetName, ChunkGenerateContext context) {
        if (from == null) {
            return;
        }
        GenerateStage now = from;
        while (now != null) {
            now.apply(context);
            if (now.name().equals(targetName)) {
                return;
            }
            now = now.getNextStage();
        }
    }

    private static GenerateStage afterName(GenerateStage stage, String name) {
        GenerateStage now = stage;
        while (now != null) {
            if (now.name().equals(name)) {
                return now.getNextStage();
            }
            now = now.getNextStage();
        }
        return null;
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    public static final class Builder {
        private final GenerateStage.Builder chainBuilder = new GenerateStage.Builder();
        private boolean started;
        private String terrainEnd;
        private String populationEnd;
        private GenerateStage end;

        public Builder start(GenerateStage stage) {
            chainBuilder.start(stage);
            started = true;
            end = stage;
            return this;
        }

        public Builder next(GenerateStage stage) {
            Preconditions.checkState(started, "you must call start(stage) first");
            chainBuilder.next(stage);
            end = stage;
            return this;
        }

        public Builder terrainEnd(String stageName) {
            this.terrainEnd = stageName;
            return this;
        }

        public Builder populationEnd(String stageName) {
            this.populationEnd = stageName;
            return this;
        }

        public PnxStagePipeline build() {
            Preconditions.checkState(started, "you must call start(stage)");
            Preconditions.checkNotNull(terrainEnd, "you must set terrainEnd(stageName)");
            Preconditions.checkNotNull(populationEnd, "you must set populationEnd(stageName)");
            return new PnxStagePipeline(
                    chainBuilder.getStart(),
                    end,
                    terrainEnd,
                    populationEnd
            );
        }
    }
}
