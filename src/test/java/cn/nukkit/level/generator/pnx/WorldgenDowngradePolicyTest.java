package cn.nukkit.level.generator.pnx;

import cn.nukkit.MockServer;
import cn.nukkit.block.BlockID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WorldgenDowngradePolicyTest {

    @BeforeAll
    static void initServer() {
        MockServer.init();
    }

    @Test
    void explicitDowngradeMappingIsUsedWhenPreferredStateIsUnsupported() {
        WorldgenDowngradePolicy policy = new WorldgenDowngradePolicy();
        LegacyBlockMapping.LegacyBlockState mapped = new LegacyBlockMapping.LegacyBlockState(BlockID.STONE_BRICK_STAIRS, 0);
        policy.register("minecraft:cobbled_deepslate_stairs", mapped);

        LegacyBlockMapping.LegacyBlockState resolved = policy.resolve(
                "minecraft:cobbled_deepslate_stairs",
                WorldgenDowngradePolicy.Category.STRUCTURE_BLOCK,
                null
        );

        Assertions.assertEquals(mapped, resolved);
        Assertions.assertEquals(1, policy.totalWarnings());
    }

    @Test
    void categoryFallbackAndWarningAggregationWork() {
        WorldgenDowngradePolicy policy = new WorldgenDowngradePolicy();

        policy.resolve("minecraft:missing_feature_block", WorldgenDowngradePolicy.Category.VEGETATION, null);
        policy.resolve("minecraft:missing_feature_block", WorldgenDowngradePolicy.Category.VEGETATION, null);

        Assertions.assertEquals(2, policy.totalWarnings());
        Assertions.assertEquals(1, policy.snapshotWarnings().size());
        Assertions.assertTrue(policy.snapshotWarnings().values().iterator().next() >= 2);
    }
}
