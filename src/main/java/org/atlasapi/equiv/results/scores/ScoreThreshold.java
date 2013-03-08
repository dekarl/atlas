package org.atlasapi.equiv.results.scores;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Predicate;

public abstract class ScoreThreshold implements Predicate<Score> {

    public static final ScoreThreshold positive() {
        return POSITIVE;
    }
    public static final ScoreThreshold nonNegative() {
        return NON_NEGATIVE;
    }
    public static final ScoreThreshold negative() {
        return NEGATIVE;
    }
    public static final ScoreThreshold nonPositive() {
        return NON_POSITIVE;
    }
    
    private static final ScoreThreshold POSITIVE = greaterThan(0);
    private static final ScoreThreshold NON_NEGATIVE = greaterThanOrEqual(0);
    private static final ScoreThreshold NEGATIVE = not(NON_NEGATIVE);
    private static final ScoreThreshold NON_POSITIVE = not(POSITIVE);
    
    public static final ScoreThreshold greaterThan(double threshold) {
        return new GreaterThanThreshold(threshold);
    }
    
    public static final ScoreThreshold greaterThanOrEqual(double threshold) {
        return new GreaterThanEqualThreshold(threshold);
    }
    
    public static final ScoreThreshold not(ScoreThreshold threshold) {
        return threshold instanceof InverseThreshold ? ((InverseThreshold)threshold).delegate
                                                     : new InverseThreshold(threshold);
    }
    
    private static final class InverseThreshold extends ScoreThreshold {

        private final ScoreThreshold delegate;

        public InverseThreshold(ScoreThreshold delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public boolean applyThreshold(Score input) {
            return !delegate.applyThreshold(input);
        }
        
    }
    
    private static final class GreaterThanThreshold extends ScoreThreshold {
        private final double threshold;

        public GreaterThanThreshold(double threshold) {
            this.threshold = threshold;
        }
        
        @Override
        public boolean applyThreshold(Score input) {
            return input.asDouble() > threshold;
        }
        
        @Override
        public String toString() {
            return String.format("greater than %s", threshold);
        }
    }
    
    private static final class GreaterThanEqualThreshold extends ScoreThreshold {
        private final double threshold;

        public GreaterThanEqualThreshold(double threshold) {
            this.threshold = threshold;
        }
        
        @Override
        public boolean applyThreshold(Score input) {
            return input.asDouble() >= threshold;
        }

        @Override
        public String toString() {
            return String.format("greater than or equal %s", threshold);
        }
    }
    
    @Override
    public final boolean apply(Score input) {
        return checkNotNull(input).isRealScore() && applyThreshold(input);
    }
    
    protected abstract boolean applyThreshold(Score input);
    
}