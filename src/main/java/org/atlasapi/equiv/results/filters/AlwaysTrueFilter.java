package org.atlasapi.equiv.results.filters;

import org.atlasapi.equiv.results.description.ResultDescription;
import org.atlasapi.equiv.results.scores.ScoredCandidate;

public class AlwaysTrueFilter<T> extends AbstractEquivalenceFilter<T> {

    private static final EquivalenceFilter<Object> INSTANCE 
                                        = new AlwaysTrueFilter<Object>();

    @SuppressWarnings("unchecked")
    public static final <T> EquivalenceFilter<T> get() {
        return (EquivalenceFilter<T>) INSTANCE;
    }
    
    private AlwaysTrueFilter() {}
    
    @Override
    protected boolean doFilter(ScoredCandidate<T> input, T subject, ResultDescription desc) {
        return true;
    }

}
