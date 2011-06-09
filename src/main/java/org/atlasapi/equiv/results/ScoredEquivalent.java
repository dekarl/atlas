package org.atlasapi.equiv.results;

import org.atlasapi.media.entity.Content;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.primitives.Doubles;

public final class ScoredEquivalent<T extends Content> implements Comparable<ScoredEquivalent<?>> {

    public static final <T extends Content> ScoredEquivalent<T> equivalentScore(T equivalent, double score) {
        return new ScoredEquivalent<T>(equivalent, score);
    }
    
    public static final <T extends Content> Function<ScoredEquivalent<T>, T> toEquivalent() {
        return new Function<ScoredEquivalent<T>, T>() {
            @Override
            public T apply(ScoredEquivalent<T> input) {
                return input.equivalent();
            }
        };
    }
    
    private final T target;
    private final double score;

    private ScoredEquivalent(T equivalent, double score) {
        target = equivalent;
        this.score = score;
    }

    public T equivalent() {
        return target;
    }

    public double score() {
        return score;
    }
    
    @Override
    public boolean equals(Object that) {
        if(this == that) {
            return true;
        }
        if(that instanceof ScoredEquivalent) {
            ScoredEquivalent<?> other = (ScoredEquivalent<?>) that;
            return target.equals(other.target) && score == other.score;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(target, score);
    }
    
    @Override
    public String toString() {
        return String.format("%s : %+.2f", target.getCanonicalUri(), score);
    }

    @Override
    public int compareTo(ScoredEquivalent<?> o) {
        return Doubles.compare(score, o.score);
    }
}