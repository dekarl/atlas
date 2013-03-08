package org.atlasapi.equiv.scorers;

import java.util.List;
import java.util.Set;

import org.atlasapi.equiv.results.description.ResultDescription;
import org.atlasapi.equiv.results.scores.DefaultScoredCandidates;
import org.atlasapi.equiv.results.scores.DefaultScoredCandidates.Builder;
import org.atlasapi.equiv.results.scores.Score;
import org.atlasapi.equiv.results.scores.ScoredCandidates;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.ChildRef;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.ContentResolver;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Ordering;
import com.google.common.collect.PeekingIterator;

public class ContainerHierarchyMatchingScorer implements EquivalenceScorer<Container> {

    private static final int MAX_EPISODE_DIFFERENCE = 1;
    private static final int MAX_SERIES_DIFFERENCE = 1;

    private final ContentResolver contentResolver;

    public ContainerHierarchyMatchingScorer(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }
    
    @Override
    public ScoredCandidates<Container> score(Container content, Set<? extends Container> suggestions, ResultDescription desc) {
        Builder<Container> results = DefaultScoredCandidates.fromSource("Hierarchy");

        // Brands can have full Series hierarchy so compare its Series' hierarchies if present. 
        // If there are no Series treat it as a flat container
        if (content instanceof Brand) {
            Brand brand = (Brand) content;
            if (!brand.getSeriesRefs().isEmpty()) {
                List<Integer> subjectSortedSeriesSizes = sortedSeriesSizes(seriesFor(brand));
                desc.appendText("Subject %s, %s series: %s", brand, subjectSortedSeriesSizes.size(), subjectSortedSeriesSizes).startStage("matches:");
                for (Container suggestion : suggestions) {
                    results.addEquivalent(suggestion, score(subjectSortedSeriesSizes, suggestion, desc));
                }
                desc.finishStage().finishStage();
                return results.build();
            }
        }
        
        desc.appendText("Subject %s, no series:", content).startStage("matches:");
        for (Container suggestion : suggestions) {
            results.addEquivalent(suggestion, score(content, suggestion, desc));
        }
        
        return results.build();
    }

    private Score score(List<Integer> subjectSortedSeriesSizes, Container suggestion, ResultDescription desc) {
        if (!(suggestion instanceof Brand)) {
            desc.appendText("%s: not Brand -> none", suggestion);
            return Score.NULL_SCORE;
        }
        return score(subjectSortedSeriesSizes, (Brand)suggestion, desc);
    }
    
    private Score score(List<Integer> subjectSortedSeriesSizes, Brand suggestion, ResultDescription desc) {
        
        if (Math.abs(subjectSortedSeriesSizes.size() - suggestion.getSeriesRefs().size()) > MAX_SERIES_DIFFERENCE) {
            desc.appendText("%s: series count |%s-%s| > %s -> none", suggestion, subjectSortedSeriesSizes.size(), suggestion.getSeriesRefs().size(), MAX_SERIES_DIFFERENCE);
            return Score.NULL_SCORE;
        }
        
        return scoreSortedSeriesSizes(subjectSortedSeriesSizes, sortedSeriesSizes(seriesFor(suggestion)), suggestion.getCanonicalUri(), desc);
    }

    @VisibleForTesting //TODO: extract into helper
    public Score scoreSortedSeriesSizes(List<Integer> subjectSortedSeriesSizes, List<Integer> suggestionSortedSeriesSizes, String suggestionUri, ResultDescription desc) {
        PeekingIterator<Integer> subjectAllocation = Iterators.peekingIterator(subjectSortedSeriesSizes.iterator());
        PeekingIterator<Integer> suggestionAllocation = Iterators.peekingIterator(suggestionSortedSeriesSizes.iterator());
       
        boolean dropped = false;
        int sub = 0;
        int sug = 0;
        
        while(subjectAllocation.hasNext() && suggestionAllocation.hasNext()) {
            
            sub = subjectAllocation.next();
            sug = suggestionAllocation.next();
            
            if (!acceptable(sub,sug)) {
                if (dropped) {
                    desc.appendText("%s: series episode counts %s -> none", suggestionUri, suggestionSortedSeriesSizes);
                    return Score.NULL_SCORE;
                }
                dropped = true;
                if (suggestionAllocation.hasNext() && acceptable(sub, suggestionAllocation.peek())) {
                    sug = suggestionAllocation.next();
                } else if (subjectAllocation.hasNext() && acceptable(subjectAllocation.peek(), sug)) {
                    sub = subjectAllocation.next();
                } 
            }
            
        }

        if (dropped && (suggestionAllocation.hasNext() || subjectAllocation.hasNext())) {
            desc.appendText("%s: series episode counts %s -> none", suggestionUri, suggestionSortedSeriesSizes);
            return Score.NULL_SCORE;
        }
        
        desc.appendText("%s: series episode counts %s -> 1", suggestionUri, subjectSortedSeriesSizes, suggestionSortedSeriesSizes);
        return Score.ONE;
    }
    
    private boolean acceptable(int sub, int sug) {
        return Math.abs(sub - sug) <= MAX_EPISODE_DIFFERENCE;
    }

    private ImmutableList<Integer> sortedSeriesSizes(List<Series> subjectSeries) {
        return Ordering.natural().immutableSortedCopy(Iterables.transform(subjectSeries, new Function<Series, Integer>() {
            @Override
            public Integer apply(Series input) {
                return input.getChildRefs().size();
            }
        }));
    }

    //Simple case were container heirarchy is flat: compare episode counts.
    private Score score(Container subject, Container suggestion, ResultDescription desc) {
        if (suggestion instanceof Brand && !((Brand)suggestion).getSeriesRefs().isEmpty()) {
            return Score.NULL_SCORE;
        }
        
        int subjectChildren = subject.getChildRefs().size();
        int suggestionChildren = suggestion.getChildRefs().size();
        
        if (acceptable(subjectChildren, suggestionChildren)) {
            desc.appendText("%s scores 1 (|%s-%s| <= %s)", suggestion, subjectChildren, suggestionChildren, MAX_EPISODE_DIFFERENCE);
            return Score.ONE;
        } else {
            desc.appendText("%s scores none (|%s-%s| > %s)", suggestion, subjectChildren, suggestionChildren, MAX_EPISODE_DIFFERENCE);
            return Score.NULL_SCORE;
        }
    }

    public ImmutableList<Series> seriesFor(Brand brand) {
        Iterable<String> uris = ImmutableList.copyOf(Iterables.transform(brand.getSeriesRefs(), ChildRef.TO_URI));
        List<Identified> allResolvedSeries = contentResolver.findByCanonicalUris(uris).getAllResolvedResults();
        return ImmutableList.copyOf(Iterables.filter(allResolvedSeries, Series.class));
    }

    @Override
    public String toString() {
        return "Container Hierarchy Scorer";
    }
}
