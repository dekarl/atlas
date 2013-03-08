package org.atlasapi.remotesite.channel4;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.query.content.PerPublisherCurieExpander;
import org.atlasapi.remotesite.ContentExtractor;
import org.joda.time.DateTime;

import com.google.common.base.Preconditions;
import com.metabroadcast.common.time.DateTimeZones;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.atom.Link;

public class C4SeriesExtractor implements ContentExtractor<Feed, SeriesAndEpisodes> {

	private static final Pattern SERIES_ID = Pattern.compile("series-(\\d+)");
	private final C4EpisodesExtractor episodesExtractor; 
	
	public C4SeriesExtractor(ContentResolver contentResolver, C4LakeviewOnDemandFetcher lakeviewFetcher, AdapterLog log) {
		this.episodesExtractor = new C4EpisodesExtractor(lakeviewFetcher, log);
	}
	
	@Override
	public SeriesAndEpisodes extract(Feed source) {

		Preconditions.checkArgument(C4AtomApi.isASeriesFeed(source), "Not a series feed, feed has id: " + source.getId());
		
		String uri = seriesUriFrom(source);
		
		Series series = new Series(uri, PerPublisherCurieExpander.CurieAlgorithm.C4.compact(uri), Publisher.C4);
		
		series.withSeriesNumber(seriesNumberFrom(source));

		// TODO new alias
		series.addAliasUrl(source.getId());
		series.setTitle(source.getTitle());
		series.setDescription(source.getSubtitle().getValue());

		series.setLastUpdated(new DateTime(source.getUpdated(), DateTimeZones.UTC));
		
		C4AtomApi.addImages(series, source.getLogo());
		
		series.setDescription(series.getDescription());
		
		series.setMediaType(MediaType.VIDEO);
		series.setSpecialization(Specialization.TV);
		
		List<Episode> episodes = episodesExtractor.extract(source);
		for (Episode episode : episodes) {
			episode.setSeries(series);
		}
		return new SeriesAndEpisodes(series, episodes);
	}
	
	private Integer seriesNumberFrom(Feed source) {
		Matcher matcher = SERIES_ID.matcher(source.getId());
		if (matcher.find()) {
			return Integer.valueOf(matcher.group(1));
		}
		return null;
	}

	private String seriesUriFrom(Feed source) {
		if (source.getAlternateLinks().isEmpty()) {
			throw new IllegalArgumentException("Cannot determine series uri");
		}
		return ((Link) source.getAlternateLinks().get(0)).getHref();
	}
}
