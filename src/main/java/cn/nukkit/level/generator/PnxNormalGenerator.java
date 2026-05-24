package cn.nukkit.level.generator;

import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.block.BlockStone;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.biome.Biome;
import cn.nukkit.level.biome.BiomeSelector;
import cn.nukkit.level.biome.EnumBiome;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.level.generator.biome.BiomePicker;
import cn.nukkit.level.generator.biome.OverworldBiomePicker;
import cn.nukkit.level.generator.biome.result.BiomeResult;
import cn.nukkit.level.generator.noise.vanilla.f.NoiseGeneratorOctavesF;
import cn.nukkit.level.generator.object.ore.OreType;
import cn.nukkit.level.generator.populator.impl.PopulatorBedrock;
import cn.nukkit.level.generator.populator.impl.PopulatorCaves;
import cn.nukkit.level.generator.populator.impl.PopulatorDeepslate;
import cn.nukkit.level.generator.populator.impl.PopulatorGroundCover;
import cn.nukkit.level.generator.populator.impl.PopulatorOre;
import cn.nukkit.level.generator.populator.impl.PopulatorSpring;
import cn.nukkit.level.generator.populator.overworld.PopulatorDesertPyramid;
import cn.nukkit.level.generator.populator.overworld.PopulatorDesertWell;
import cn.nukkit.level.generator.populator.overworld.PopulatorDungeon;
import cn.nukkit.level.generator.populator.overworld.PopulatorFossil;
import cn.nukkit.level.generator.populator.overworld.PopulatorIgloo;
import cn.nukkit.level.generator.populator.overworld.PopulatorJungleTemple;
import cn.nukkit.level.generator.populator.overworld.PopulatorMineshaft;
import cn.nukkit.level.generator.populator.overworld.PopulatorOceanRuin;
import cn.nukkit.level.generator.populator.overworld.PopulatorPillagerOutpost;
import cn.nukkit.level.generator.populator.overworld.PopulatorShipwreck;
import cn.nukkit.level.generator.populator.overworld.PopulatorStronghold;
import cn.nukkit.level.generator.populator.overworld.PopulatorSwampHut;
import cn.nukkit.level.generator.populator.overworld.PopulatorVillage;
import cn.nukkit.level.generator.populator.type.Populator;
import cn.nukkit.level.generator.pnx.PnxStagePipeline;
import cn.nukkit.level.generator.stages.DelegatedStage;
import cn.nukkit.level.generator.stages.FinishedStage;
import cn.nukkit.level.generator.stages.GeneratedStage;
import cn.nukkit.level.generator.stages.LightPopulationStage;
import cn.nukkit.level.generator.stages.NormalChunkFeatureStage;
import cn.nukkit.level.generator.stages.normal.NormalPopulatorStage;
import cn.nukkit.level.generator.stages.normal.NormalSurfaceDataStage;
import cn.nukkit.level.generator.stages.normal.NormalSurfaceOverwriteStage;
import cn.nukkit.level.generator.stages.normal.NormalTerrainStage;
import cn.nukkit.level.generator.task.ChunkPopulationTask;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.math.Vector3;
import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

public class PnxNormalGenerator extends PopulatedGenerator implements BiomedGenerator {
    public static final int BEDROCK_LAYER = -64;
    public static final int SEA_HEIGHT = 64;

    private static final List<Populator> GENERATION_POPULATORS = ImmutableList.of(
            new PopulatorDeepslate(BEDROCK_LAYER),
            new PopulatorGroundCover()
    );
    private static final List<Populator> POPULATORS = ImmutableList.of(
            new PopulatorOre(BlockID.STONE, new OreType[]{
                    new OreType(Block.get(BlockID.COAL_ORE), 20, 17, 0, 131),
                    new OreType(Block.get(BlockID.COPPER_ORE), 20, 9, 0, 192),
                    new OreType(Block.get(BlockID.IRON_ORE), 20, 9, 0, 63),
                    new OreType(Block.get(BlockID.REDSTONE_ORE), 8, 8, 0, 15),
                    new OreType(Block.get(BlockID.LAPIS_ORE), 1, 7, 0, 33),
                    new OreType(Block.get(BlockID.GOLD_ORE), 2, 9, 0, 33),
                    new OreType(Block.get(BlockID.DIAMOND_ORE), 1, 8, 0, 15),
                    new OreType(Block.get(BlockID.DIRT), 10, 33, 0, 128),
                    new OreType(Block.get(BlockID.GRAVEL), 8, 33, 0, 128),
                    new OreType(Block.get(BlockID.STONE, BlockStone.GRANITE), 10, 33, 0, 80),
                    new OreType(Block.get(BlockID.STONE, BlockStone.DIORITE), 10, 33, 0, 80),
                    new OreType(Block.get(BlockID.STONE, BlockStone.ANDESITE), 10, 33, 0, 80),
                    new OreType(Block.get(BlockID.DEEPSLATE), 20, 33, 0, 8)
            }),
            new PopulatorOre(BlockID.DEEPSLATE, new OreType[]{
                    new OreType(Block.get(BlockID.DEEPSLATE_COAL_ORE), 1, 13, -4, 8, BlockID.DEEPSLATE),
                    new OreType(Block.get(BlockID.DEEPSLATE_COPPER_ORE), 5, 9, -64, 8, BlockID.DEEPSLATE),
                    new OreType(Block.get(BlockID.DEEPSLATE_IRON_ORE), 5, 9, -64, 8, BlockID.DEEPSLATE),
                    new OreType(Block.get(BlockID.DEEPSLATE_REDSTONE_ORE), 8, 8, -64, 8, BlockID.DEEPSLATE),
                    new OreType(Block.get(BlockID.DEEPSLATE_LAPIS_ORE), 6, 6, -64, 8, BlockID.DEEPSLATE),
                    new OreType(Block.get(BlockID.DEEPSLATE_GOLD_ORE), 2, 9, -64, 8, BlockID.DEEPSLATE),
                    new OreType(Block.get(BlockID.DEEPSLATE_DIAMOND_ORE), 4, 5, -64, 8, BlockID.DEEPSLATE)
            }),
            new PopulatorCaves(BEDROCK_LAYER),
            new PopulatorSpring(BlockID.WATER, BlockID.STONE, 15, 8, 255),
            new PopulatorSpring(BlockID.LAVA, BlockID.STONE, 10, 16, 255),
            new PopulatorBedrock(BEDROCK_LAYER)
    );
    private static final List<Populator> STRUCTURE_POPULATORS = ImmutableList.of(
            new PopulatorFossil(),
            new PopulatorShipwreck(),
            new PopulatorSwampHut(),
            new PopulatorDesertPyramid(),
            new PopulatorJungleTemple(),
            new PopulatorIgloo(),
            new PopulatorPillagerOutpost(),
            new PopulatorOceanRuin(),
            new PopulatorVillage(),
            new PopulatorStronghold(),
            new PopulatorMineshaft(),
            new PopulatorDesertWell(),
            new PopulatorDungeon()
    );

    private NoiseGeneratorOctavesF depthNoise;
    private NoiseGeneratorOctavesF minLimitPerlinNoise;
    private NoiseGeneratorOctavesF maxLimitPerlinNoise;
    private NoiseGeneratorOctavesF mainPerlinNoise;
    private BiomeSelector selector;
    private NukkitRandom nukkitRandom;
    private long localSeed1;
    private long localSeed2;

    private final ThreadLocal<float[]> depthRegion = ThreadLocal.withInitial(() -> null);
    private final ThreadLocal<float[]> mainNoiseRegion = ThreadLocal.withInitial(() -> null);
    private final ThreadLocal<float[]> minLimitRegion = ThreadLocal.withInitial(() -> null);
    private final ThreadLocal<float[]> maxLimitRegion = ThreadLocal.withInitial(() -> null);
    private final ThreadLocal<float[]> heightMap = ThreadLocal.withInitial(() -> new float[825]);

    public PnxNormalGenerator() {
        this(Map.of());
    }

    public PnxNormalGenerator(Map<String, Object> options) {
        super(options == null ? Collections.emptyMap() : options);
    }

    @Override
    protected void initGenerator(ChunkManager level, NukkitRandom random) {
        this.nukkitRandom = random;
        this.nukkitRandom.setSeed(level.getSeed());
        SplittableRandom random1 = new SplittableRandom();
        this.localSeed1 = random1.nextLong();
        this.localSeed2 = random1.nextLong();
        this.nukkitRandom.setSeed(level.getSeed());
        this.selector = new BiomeSelector(this.nukkitRandom);

        this.minLimitPerlinNoise = new NoiseGeneratorOctavesF(random, 16);
        this.maxLimitPerlinNoise = new NoiseGeneratorOctavesF(random, 16);
        this.mainPerlinNoise = new NoiseGeneratorOctavesF(random, 8);
        this.depthNoise = new NoiseGeneratorOctavesF(random, 16);
    }

    @Override
    protected void stages(PnxStagePipeline.Builder builder) {
        builder.start(new NormalTerrainStage(this));
        builder.next(new NormalSurfaceDataStage(this));
        builder.next(new NormalSurfaceOverwriteStage(this));
        builder.next(new GeneratedStage());
        builder.next(new NormalPopulatorStage(this));
        builder.next(new NormalChunkFeatureStage());
        builder.next(new LightPopulationStage());
        builder.next(new FinishedStage());
        builder.next(new DelegatedStage("pnx_normal:structure", context -> runStructurePopulators(context.getChunkX(), context.getChunkZ())));
        builder.terrainEnd(GeneratedStage.NAME);
        builder.populationEnd(FinishedStage.NAME);
    }

    @Override
    public int getId() {
        return TYPE_INFINITE;
    }

    @Override
    public String getName() {
        return "pnx_normal";
    }

    @Override
    public Vector3 getSpawn() {
        return new Vector3(0.5, 256, 0.5);
    }

    @Override
    public BiomePicker<? extends BiomeResult> createBiomePicker(ChunkManager level) {
        return new OverworldBiomePicker(level);
    }

    public Biome pickBiome(int x, int z) {
        return selector.pickBiome(x, z);
    }

    public void runGenerationPopulators(int chunkX, int chunkZ) {
        BaseFullChunk chunk = level.getChunk(chunkX, chunkZ);
        for (Populator populator : GENERATION_POPULATORS) {
            populator.populate(this.level, chunkX, chunkZ, this.nukkitRandom, chunk);
        }
    }

    public void runPopulators(int chunkX, int chunkZ) {
        this.nukkitRandom.setSeed(0xdeadbeef ^ (chunkX << 8) ^ chunkZ ^ this.level.getSeed());
        for (Populator populator : POPULATORS) {
            populator.populate(this.level, chunkX, chunkZ, this.nukkitRandom, level.getChunk(chunkX, chunkZ));
        }
        Biome biome = EnumBiome.getBiome(level.getChunk(chunkX, chunkZ).getBiomeId(7, 7));
        biome.populateChunk(this.level, chunkX, chunkZ, this.nukkitRandom);
    }

    public void runStructurePopulators(int chunkX, int chunkZ) {
        final BaseFullChunk chunk = level.getChunk(chunkX, chunkZ);
        for (final Populator populator : STRUCTURE_POPULATORS) {
            Server.getInstance().computeThreadPool.submit(new ChunkPopulationTask(level, chunk, populator));
        }
    }

    public NoiseGeneratorOctavesF depthNoise() {
        return depthNoise;
    }

    public NoiseGeneratorOctavesF minLimitPerlinNoise() {
        return minLimitPerlinNoise;
    }

    public NoiseGeneratorOctavesF maxLimitPerlinNoise() {
        return maxLimitPerlinNoise;
    }

    public NoiseGeneratorOctavesF mainPerlinNoise() {
        return mainPerlinNoise;
    }

    public ThreadLocal<float[]> depthRegion() {
        return depthRegion;
    }

    public ThreadLocal<float[]> mainNoiseRegion() {
        return mainNoiseRegion;
    }

    public ThreadLocal<float[]> minLimitRegion() {
        return minLimitRegion;
    }

    public ThreadLocal<float[]> maxLimitRegion() {
        return maxLimitRegion;
    }

    public ThreadLocal<float[]> heightMap() {
        return heightMap;
    }

    public NukkitRandom random() {
        return nukkitRandom;
    }

    public long localSeed1() {
        return localSeed1;
    }

    public long localSeed2() {
        return localSeed2;
    }
}
