package cn.nukkit.level.generator;

import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.format.generic.BaseFullChunk;

/**
 * Chunk-scoped context passed into generation stages.
 */
public final class ChunkGenerateContext {
    private final Generator generator;
    private final ChunkManager level;
    private final int chunkX;
    private final int chunkZ;

    public ChunkGenerateContext(Generator generator, ChunkManager level, int chunkX, int chunkZ) {
        this.generator = generator;
        this.level = level;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public Generator getGenerator() {
        return generator;
    }

    public ChunkManager getLevel() {
        return level;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public BaseFullChunk getChunk() {
        return level.getChunk(chunkX, chunkZ);
    }
}
