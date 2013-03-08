package org.atlasapi.equiv.scorers;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;

import org.atlasapi.equiv.results.description.DefaultDescription;
import org.atlasapi.equiv.results.description.ResultDescription;
import org.atlasapi.equiv.results.scores.Score;
import org.atlasapi.equiv.results.scores.ScoredCandidates;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.ChildRef;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.EntityType;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.time.DateTimeZones;

@RunWith(JMock.class)
public class ContainerHierarchyMatchingScorerTest {

    private final Mockery context = new Mockery();
    private final ContentResolver contentResolver = context.mock(ContentResolver.class);
    
    private final ContainerHierarchyMatchingScorer scorer = new ContainerHierarchyMatchingScorer(contentResolver);
    
    @Test
    @SuppressWarnings("unchecked")
    public void testScoresNullWhenFlatContainerWithChildCountsOutOfRange() {
        
        context.checking(new Expectations(){{
            never(contentResolver).findByCanonicalUris((Iterable<String>) with(anything()));
        }});

        ScoredCandidates<Container> score = scorer.score(brandWithChildren(5), ImmutableSet.<Container>of(brandWithChildren(7)), new DefaultDescription());
        
        assertThat(Iterables.getOnlyElement(score.candidates().values()), is(Score.NULL_SCORE));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testScores1WhenFlatContainerWithChildCountsInRange() {
        
        context.checking(new Expectations(){{
            never(contentResolver).findByCanonicalUris((Iterable<String>) with(anything()));
        }});

        ScoredCandidates<Container> score = scorer.score(brandWithChildren(7), ImmutableSet.<Container>of(brandWithChildren(7)), new DefaultDescription());
        
        assertThat(Iterables.getOnlyElement(score.candidates().values()), is(Score.ONE));
    }
    
    @Test
    public void testScoresNullWhenSeriesContainerWithSeriesCountsOutOfRange() {
        
        final Brand subject = brandWithSeries(5);

        context.checking(new Expectations(){{
            one(contentResolver).findByCanonicalUris(with(ImmutableList.copyOf(Iterables.transform(subject.getSeriesRefs(),ChildRef.TO_URI))));
                will(returnValue(ResolvedContent.builder().putAll(series(5)).build()));
        }});

        ScoredCandidates<Container> score = scorer.score(subject, ImmutableSet.<Container>of(brandWithSeries(7)), new DefaultDescription());
        
        assertThat(Iterables.getOnlyElement(score.candidates().values()), is(Score.NULL_SCORE));
    }
    
    @Test
    public void testScoresOneWhenSeriesContainerWithSeriesAndEpisodeCountsInRange() {
        
        final Brand subject = brandWithSeries(5);
        final Brand suggestion = brandWithSeries(6);

        context.checking(new Expectations(){{
            one(contentResolver).findByCanonicalUris(with(ImmutableList.copyOf(Iterables.transform(subject.getSeriesRefs(),ChildRef.TO_URI))));
                will(returnValue(ResolvedContent.builder().putAll(series(5)).build()));
            one(contentResolver).findByCanonicalUris(with(ImmutableList.copyOf(Iterables.transform(suggestion.getSeriesRefs(),ChildRef.TO_URI))));
                will(returnValue(ResolvedContent.builder().putAll(series(6)).build()));
        }});

        ScoredCandidates<Container> score = scorer.score(subject, ImmutableSet.<Container>of(suggestion), new DefaultDescription());
        
        assertThat(Iterables.getOnlyElement(score.candidates().values()), is(Score.ONE));
    }
    
    @Test
    public void testScoreSeriesEpisodeCounts() {
        
        ResultDescription desc = new DefaultDescription();
        assertThat(scorer.scoreSortedSeriesSizes(list(1,2,3,4), list(1,2,3,4), "suggestion", desc), is(Score.ONE));
        assertThat(scorer.scoreSortedSeriesSizes(list(1,2,3,4,5), list(1,2,3,4), "suggestion", desc), is(Score.ONE));
        assertThat(scorer.scoreSortedSeriesSizes(list(1,2,3,4), list(1,3,4), "suggestion", desc), is(Score.ONE));
        assertThat(scorer.scoreSortedSeriesSizes(list(1,2,3,6), list(1,3,5), "suggestion", desc), is(Score.ONE));
        
        assertThat(scorer.scoreSortedSeriesSizes(list(6,6), list(24,24), "suggestion", desc), is(Score.NULL_SCORE));
        assertThat(scorer.scoreSortedSeriesSizes(list(6,6), list(6,24,24), "suggestion", desc), is(Score.NULL_SCORE));
        assertThat(scorer.scoreSortedSeriesSizes(list(2,3,4), list(3,4,6,7), "suggestion", desc), is(Score.NULL_SCORE));
        
    }

    private List<Integer> list(Integer...is) {
        return ImmutableList.copyOf(is);
    }

    private Map<String, ? extends Identified> series(int i) {
        Builder<String, Series> builder = ImmutableMap.builder();
        for (int j = 0; j < i; j++) {
            Series series = new Series("uri"+j, "curie", Publisher.BBC);
            builder.put(series.getCanonicalUri(), series);
        };
        return builder.build();
    }
    
    private Brand brandWithSeries(int series) {
        Brand brand = new Brand();
        brand.setSeriesRefs(Iterables.limit(Iterables.cycle(new ChildRef("uri", "sk", new DateTime(DateTimeZones.UTC), EntityType.EPISODE)), series));
        return brand;
    }

    private Brand brandWithChildren(int children) {
        Brand brand = new Brand();
        setChildren(children, brand);
        return brand;
    }

    public void setChildren(int children, Container brand) {
        brand.setChildRefs(Iterables.limit(Iterables.cycle(new ChildRef("uri", "sk", new DateTime(DateTimeZones.UTC), EntityType.EPISODE)), children));
    }

}
