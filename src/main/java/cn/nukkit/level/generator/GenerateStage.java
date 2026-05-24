package cn.nukkit.level.generator;

import com.google.common.base.Preconditions;

/**
 * Minimal stage abstraction used by the pnx_* generator pipeline.
 */
public abstract class GenerateStage {
    private GenerateStage next;

    public abstract void apply(ChunkGenerateContext context);

    public abstract String name();

    public final GenerateStage getNextStage() {
        return next;
    }

    private void next(GenerateStage stage) {
        if (this.next == null) {
            this.next = stage;
        } else {
            this.next.next(stage);
        }
    }

    @Override
    public String toString() {
        return name();
    }

    public static final class Builder {
        private GenerateStage start;
        private GenerateStage end;

        public Builder start(GenerateStage start) {
            this.start = start;
            this.end = start;
            return this;
        }

        public Builder next(GenerateStage next) {
            Preconditions.checkNotNull(this.end, "you must set start stage before next");
            this.end.next(next);
            this.end = next;
            return this;
        }

        public GenerateStage getStart() {
            Preconditions.checkNotNull(this.start, "you must set start stage");
            return this.start;
        }

        public GenerateStage getEnd() {
            return this.end;
        }
    }
}
