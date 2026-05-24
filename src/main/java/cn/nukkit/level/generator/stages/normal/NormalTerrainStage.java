package cn.nukkit.level.generator.stages.normal;

import cn.nukkit.block.BlockID;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.level.generator.ChunkGenerateContext;
import cn.nukkit.level.generator.GenerateStage;
import cn.nukkit.level.generator.PnxNormalGenerator;
import cn.nukkit.level.biome.Biome;
import cn.nukkit.math.MathHelper;

public class NormalTerrainStage extends GenerateStage {
    public static final String NAME = "normal_terrain";

    private static final float[] BIOME_WEIGHTS = new float[25];

    static {
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                BIOME_WEIGHTS[i + 2 + (j + 2) * 5] = (float) (10.0F / Math.sqrt((float) (i * i + j * j) + 0.2F));
            }
        }
    }

    private final PnxNormalGenerator generator;

    public NormalTerrainStage(PnxNormalGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void apply(ChunkGenerateContext context) {
        final int chunkX = context.getChunkX();
        final int chunkZ = context.getChunkZ();
        final int baseX = chunkX << 4;
        final int baseZ = chunkZ << 4;

        generator.random().setSeed(chunkX * generator.localSeed1() ^ chunkZ * generator.localSeed2() ^ context.getLevel().getSeed());
        BaseFullChunk chunk = context.getChunk();

        float[] depthRegion = generator.depthNoise().generateNoiseOctaves(generator.depthRegion().get(), chunkX << 2, chunkZ << 2, 5, 5, 200f, 200f, 0.5f);
        generator.depthRegion().set(depthRegion);
        float[] mainNoiseRegion = generator.mainPerlinNoise().generateNoiseOctaves(generator.mainNoiseRegion().get(), chunkX << 2, 0, chunkZ << 2, 5, 33, 5, 11.406866f, 4.277575f, 11.406866f);
        generator.mainNoiseRegion().set(mainNoiseRegion);
        float[] minLimitRegion = generator.minLimitPerlinNoise().generateNoiseOctaves(generator.minLimitRegion().get(), chunkX << 2, 0, chunkZ << 2, 5, 33, 5, 684.412f, 684.412f, 684.412f);
        generator.minLimitRegion().set(minLimitRegion);
        float[] maxLimitRegion = generator.maxLimitPerlinNoise().generateNoiseOctaves(generator.maxLimitRegion().get(), chunkX << 2, 0, chunkZ << 2, 5, 33, 5, 684.412f, 684.412f, 684.412f);
        generator.maxLimitRegion().set(maxLimitRegion);
        float[] heightMap = generator.heightMap().get();

        int horizCounter = 0;
        int vertCounter = 0;
        for (int xSeg = 0; xSeg < 5; ++xSeg) {
            for (int zSeg = 0; zSeg < 5; ++zSeg) {
                float heightVariationSum = 0.0F;
                float baseHeightSum = 0.0F;
                float biomeWeightSum = 0.0F;
                Biome biome = generator.pickBiome(baseX + (xSeg << 2), baseZ + (zSeg << 2));

                for (int xSmooth = -2; xSmooth <= 2; ++xSmooth) {
                    for (int zSmooth = -2; zSmooth <= 2; ++zSmooth) {
                        Biome biome1 = generator.pickBiome(baseX + (xSeg << 2) + xSmooth, baseZ + (zSeg << 2) + zSmooth);
                        float baseHeight = biome1.getBaseHeight();
                        float heightVariation = biome1.getHeightVariation();
                        float scaledWeight = BIOME_WEIGHTS[xSmooth + 2 + (zSmooth + 2) * 5] / (baseHeight + 2.0F);
                        if (biome1.getBaseHeight() > biome.getBaseHeight()) {
                            scaledWeight /= 2.0F;
                        }
                        heightVariationSum += heightVariation * scaledWeight;
                        baseHeightSum += baseHeight * scaledWeight;
                        biomeWeightSum += scaledWeight;
                    }
                }

                heightVariationSum = heightVariationSum / biomeWeightSum;
                baseHeightSum = baseHeightSum / biomeWeightSum;
                heightVariationSum = heightVariationSum * 0.9F + 0.1F;
                baseHeightSum = (baseHeightSum * 4.0F - 1.0F) / 8.0F;
                float depthNoise = depthRegion[vertCounter] / 8000.0f;

                if (depthNoise < 0.0f) {
                    depthNoise = -depthNoise * 0.3f;
                }

                depthNoise = depthNoise * 3.0f - 2.0f;
                if (depthNoise < 0.0f) {
                    depthNoise = depthNoise / 2.0f;
                    if (depthNoise < -1.0f) {
                        depthNoise = -1.0f;
                    }
                    depthNoise = depthNoise / 1.4f;
                    depthNoise = depthNoise / 2.0f;
                } else {
                    if (depthNoise > 1.0f) {
                        depthNoise = 1.0f;
                    }
                    depthNoise = depthNoise / 8.0f;
                }

                ++vertCounter;
                float baseHeightClone = baseHeightSum + depthNoise * 0.2f;
                float heightVariationClone = heightVariationSum;
                baseHeightClone = baseHeightClone * 8.5f / 8.0f;
                float baseHeightFactor = 8.5f + baseHeightClone * 4.0f;

                for (int ySeg = 0; ySeg < 33; ++ySeg) {
                    float baseScale = ((float) ySeg - baseHeightFactor) * 12f * 128.0f / 256.0f / heightVariationClone;
                    if (baseScale < 0.0f) {
                        baseScale *= 4.0f;
                    }
                    float minScaled = minLimitRegion[horizCounter] / 512f;
                    float maxScaled = maxLimitRegion[horizCounter] / 512f;
                    float noiseScaled = (mainNoiseRegion[horizCounter] / 10.0f + 1.0f) / 2.0f;
                    float clamp = MathHelper.denormalizeClamp(minScaled, maxScaled, noiseScaled) - baseScale;
                    if (ySeg > 29) {
                        float yScaled = ((float) (ySeg - 29) / 3.0F);
                        clamp = clamp * (1.0f - yScaled) + -10.0f * yScaled;
                    }
                    heightMap[horizCounter] = clamp;
                    ++horizCounter;
                }
            }
        }

        for (int xSeg = 0; xSeg < 4; ++xSeg) {
            int xScale = xSeg * 5;
            int xScaleEnd = (xSeg + 1) * 5;
            for (int zSeg = 0; zSeg < 4; ++zSeg) {
                int zScale1 = (xScale + zSeg) * 33;
                int zScaleEnd1 = (xScale + zSeg + 1) * 33;
                int zScale2 = (xScaleEnd + zSeg) * 33;
                int zScaleEnd2 = (xScaleEnd + zSeg + 1) * 33;

                for (int ySeg = 0; ySeg < 32; ++ySeg) {
                    double height1 = heightMap[zScale1 + ySeg];
                    double height2 = heightMap[zScaleEnd1 + ySeg];
                    double height3 = heightMap[zScale2 + ySeg];
                    double height4 = heightMap[zScaleEnd2 + ySeg];
                    double height5 = (heightMap[zScale1 + ySeg + 1] - height1) * 0.125f;
                    double height6 = (heightMap[zScaleEnd1 + ySeg + 1] - height2) * 0.125f;
                    double height7 = (heightMap[zScale2 + ySeg + 1] - height3) * 0.125f;
                    double height8 = (heightMap[zScaleEnd2 + ySeg + 1] - height4) * 0.125f;

                    for (int yIn = 0; yIn < 8; ++yIn) {
                        double baseIncr = height1;
                        double baseIncr2 = height2;
                        double scaleY = (height3 - height1) * 0.25f;
                        double scaleY2 = (height4 - height2) * 0.25f;

                        for (int zIn = 0; zIn < 4; ++zIn) {
                            double scaleZ = (baseIncr2 - baseIncr) * 0.25f;
                            double scaleZ2 = baseIncr - scaleZ;

                            for (int xIn = 0; xIn < 4; ++xIn) {
                                if ((scaleZ2 += scaleZ) > 0.0f) {
                                    chunk.setBlockId((xSeg << 2) + zIn, (ySeg << 3) + yIn, (zSeg << 2) + xIn, BlockID.STONE);
                                } else if ((ySeg << 3) + yIn <= PnxNormalGenerator.SEA_HEIGHT) {
                                    chunk.setBlockId((xSeg << 2) + zIn, (ySeg << 3) + yIn, (zSeg << 2) + xIn, BlockID.STILL_WATER);
                                }
                            }
                            baseIncr += scaleY;
                            baseIncr2 += scaleY2;
                        }

                        height1 += height5;
                        height2 += height6;
                        height3 += height7;
                        height4 += height8;
                    }
                }
            }
        }
    }

    @Override
    public String name() {
        return NAME;
    }
}
