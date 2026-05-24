package cn.nukkit.level.generator.stages;

import cn.nukkit.level.generator.ChunkGenerateContext;
import cn.nukkit.level.generator.GenerateStage;

import java.util.function.Consumer;

public class DelegatedStage extends GenerateStage {
    private final String name;
    private final Consumer<ChunkGenerateContext> delegate;

    public DelegatedStage(String name, Consumer<ChunkGenerateContext> delegate) {
        this.name = name;
        this.delegate = delegate;
    }

    @Override
    public void apply(ChunkGenerateContext context) {
        delegate.accept(context);
    }

    @Override
    public String name() {
        return name;
    }
}
