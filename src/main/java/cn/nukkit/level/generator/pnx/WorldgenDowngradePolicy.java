package cn.nukkit.level.generator.pnx;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.block.BlockUnknown;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Rules-based downgrade policy for missing/unsupported block states in migrated PNX worldgen.
 */
public final class WorldgenDowngradePolicy {
    public enum Category {
        TERRAIN_FILL,
        VEGETATION,
        STRUCTURE_BLOCK,
        FUNCTIONAL_BLOCK
    }

    private static final LegacyBlockMapping.LegacyBlockState DEFAULT_TERRAIN = new LegacyBlockMapping.LegacyBlockState(BlockID.STONE, 0);
    private static final LegacyBlockMapping.LegacyBlockState DEFAULT_VEGETATION = new LegacyBlockMapping.LegacyBlockState(BlockID.LEAVES, 0);
    private static final LegacyBlockMapping.LegacyBlockState DEFAULT_STRUCTURE = new LegacyBlockMapping.LegacyBlockState(BlockID.COBBLESTONE, 0);
    private static final LegacyBlockMapping.LegacyBlockState DEFAULT_FUNCTIONAL = new LegacyBlockMapping.LegacyBlockState(BlockID.STONE, 0);

    private final Map<String, LegacyBlockMapping.LegacyBlockState> explicitDowngrades = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> warningCounters = new ConcurrentHashMap<>();

    public void register(String sourceIdentifier, LegacyBlockMapping.LegacyBlockState replacement) {
        explicitDowngrades.put(sourceIdentifier, replacement);
    }

    public LegacyBlockMapping.LegacyBlockState resolve(String sourceIdentifier,
                                                       Category category,
                                                       LegacyBlockMapping.LegacyBlockState preferred) {
        if (preferred != null && isKnownLegacyState(preferred.id(), preferred.meta())) {
            return preferred;
        }

        LegacyBlockMapping.LegacyBlockState explicit = explicitDowngrades.get(sourceIdentifier);
        if (explicit != null && isKnownLegacyState(explicit.id(), explicit.meta())) {
            warn(category, sourceIdentifier, explicit);
            return explicit;
        }

        LegacyBlockMapping.LegacyBlockState fallback = fallbackFor(category);
        warn(category, sourceIdentifier, fallback);
        return fallback;
    }

    public Map<String, Integer> snapshotWarnings() {
        Map<String, Integer> snapshot = new ConcurrentHashMap<>();
        warningCounters.forEach((k, v) -> snapshot.put(k, v.intValue()));
        return Collections.unmodifiableMap(snapshot);
    }

    public int totalWarnings() {
        return warningCounters.values().stream().mapToInt(LongAdder::intValue).sum();
    }

    private void warn(Category category, String sourceIdentifier, LegacyBlockMapping.LegacyBlockState fallback) {
        String key = category + "|" + sourceIdentifier + "->" + fallback.id() + ":" + fallback.meta();
        warningCounters.computeIfAbsent(key, k -> new LongAdder()).increment();
    }

    private static LegacyBlockMapping.LegacyBlockState fallbackFor(Category category) {
        return switch (category) {
            case TERRAIN_FILL -> DEFAULT_TERRAIN;
            case VEGETATION -> DEFAULT_VEGETATION;
            case STRUCTURE_BLOCK -> DEFAULT_STRUCTURE;
            case FUNCTIONAL_BLOCK -> DEFAULT_FUNCTIONAL;
        };
    }

    private static boolean isKnownLegacyState(int id, int meta) {
        return !(Block.get(id, meta) instanceof BlockUnknown);
    }
}
