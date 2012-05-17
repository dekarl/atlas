package org.atlasapi.remotesite.channel4;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.query.content.PerPublisherCurieExpander;
import org.atlasapi.remotesite.ContentExtractor;

import com.google.common.collect.Lists;
import com.metabroadcast.common.time.Clock;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;

/**
 * Extracts content from a feed such as /episode-guide/series-x.atom for some x
 * where 0 < x <= |S|, S being the set of Series in a given Brand.
 * 
 * @author Fred van den Driessche (fred@metabroadcast.com)
 */
public class C4SeriesAndEpisodesExtractor implements ContentExtractor<Feed, SeriesAndEpisodes> {

    private static final Pattern SERIES_ID = Pattern.compile("series-(\\d+)");

    private final C4EpisodeGuideEpisodeExtractor episodeExtractor;
    private final Clock clock;

    public C4SeriesAndEpisodesExtractor(Clock clock) {
        this.episodeExtractor = new C4EpisodeGuideEpisodeExtractor(clock);
        this.clock = clock;
    }

    @Override
    public SeriesAndEpisodes extract(Feed source) {

        String seriesUri = C4AtomApi.canonicalSeriesUri(source);
        Series series = createSeriesFromFeed(seriesUri, source);
        
        List<Episode> episodes = Lists.newArrayListWithExpectedSize(source.getEntries().size());
        for (Object entry : source.getEntries()) {
            episodes.add(extractEpisode(series, (Entry) entry));
        }

        return new SeriesAndEpisodes(series, episodes);
    }

    private Series createSeriesFromFeed(String uri, Feed source) {
        Series series = new Series(uri, PerPublisherCurieExpander.CurieAlgorithm.C4.compact(uri), Publisher.C4);
        series.addAlias(C4AtomApi.canonicalizeSeriesFeedId(source));

        series.setLastUpdated(clock.now());
        
        series.withSeriesNumber(seriesNumberFrom(source));
        
        series.setTitle(source.getTitle());
        series.setDescription(source.getSubtitle().getValue());
        C4AtomApi.addImages(series, source.getLogo());
        
        series.setMediaType(MediaType.VIDEO);
        series.setSpecialization(Specialization.TV);
        
        return series;
    }
    
    private Integer seriesNumberFrom(Feed source) {
        Matcher matcher = SERIES_ID.matcher(source.getId());
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        }
        return null;
    }
    
    private Episode extractEpisode(Series series, Entry entry) {
        Episode episode = episodeExtractor.extract(entry);
        episode.setSeries(series);
        return episode;
    }

}
