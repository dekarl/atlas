package org.atlasapi.remotesite.channel4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Restriction;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.logging.NullAdapterLog;
import org.joda.time.DateTime;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.intl.Country;
import com.metabroadcast.common.time.DateTimeZones;

public class C4FourOdEpisodesExtractorTest extends TestCase {

	private final AtomFeedBuilder fourOdFeed = new AtomFeedBuilder(Resources.getResource(getClass(), "ramsays-kitchen-nightmares-4od.atom"));
	
	public void testExtractingEpisodes() throws Exception {
		
		List<Episode> episodes = new C4EpisodesExtractor(null, new NullAdapterLog()).includeOnDemands().extract(fourOdFeed.build());

		Episode firstEpisode = (Episode) Iterables.get(episodes, 0);
		
		assertThat(firstEpisode.getCanonicalUri(), is("http://www.channel4.com/programmes/ramsays-kitchen-nightmares/episode-guide/series-1/episode-1"));
		// TODO new alias
		assertThat(firstEpisode.getAliasUrls(), is((Set<String>) ImmutableSet.of("http://www.channel4.com/programmes/ramsays-kitchen-nightmares/4od#2922045")));
		
		assertThat(firstEpisode.getCurie(), is("c4:ramsays-kitchen-nightmares-series-1-episode-1"));
		assertThat(firstEpisode.getTitle(), is("Series 1 Episode 1"));
		assertThat(firstEpisode.getPublisher(), is(Publisher.C4));
		assertThat(firstEpisode.getSeriesNumber(), is(1));
		assertThat(firstEpisode.getEpisodeNumber(), is(1));
		assertThat(firstEpisode.getDescription(), startsWith("Gordon Ramsay visits Bonapartes in Silsden, West Yorkshire."));
		assertThat(firstEpisode.getThumbnail(), is("http://www.channel4.com/assets/programmes/images/ramsays-kitchen-nightmares/series-1/ramsays-kitchen-nightmares-s1-20090617160732_200x113.jpg"));
		assertThat(firstEpisode.getImage(), is("http://www.channel4.com/assets/programmes/images/ramsays-kitchen-nightmares/series-1/ramsays-kitchen-nightmares-s1-20090617160732_625x352.jpg"));
		assertThat(firstEpisode.getLastUpdated(), is(new DateTime("2010-11-08T17:57:59.924Z", DateTimeZones.UTC)));
		
		Version firstEpisodeVersion = Iterables.get(firstEpisode.getVersions(), 0);
		assertThat(firstEpisodeVersion.getDuration(), is((48 * 60) + 55));
		
		Restriction r = firstEpisodeVersion.getRestriction();
		assertThat(r.isRestricted(), is(true));
		assertThat(r.getMessage(), is("Strong language throughout"));
		assertThat(r.getMinimumAge(), is(16));
		
		assertThat(firstEpisodeVersion.getBroadcasts(), is(Collections.<Broadcast>emptySet()));
		
		Encoding firstEpsiodeEncoding = Iterables.get(firstEpisodeVersion.getManifestedAs(), 0); 
		
		Location firstEpsiodeLocation = Iterables.get(firstEpsiodeEncoding.getAvailableAt(), 0); 
		
		assertThat(firstEpsiodeLocation.getUri(), is("http://www.channel4.com/programmes/ramsays-kitchen-nightmares/4od#2922045"));
		// TODO new alias
		assertThat(firstEpsiodeLocation.getAliasUrls(), is((Set<String>) ImmutableSet.of("tag:www.channel4.com,2009:/programmes/ramsays-kitchen-nightmares/4od%232922045")));

		assertThat(firstEpsiodeLocation.getTransportType(), is(TransportType.LINK));
		
		Policy firstEpisodePolicy = firstEpsiodeLocation.getPolicy();
		assertThat(firstEpisodePolicy.getAvailabilityStart().withZone(DateTimeZones.UTC), is(new DateTime("2009-07-01T22:00:00.000Z").withZone(DateTimeZones.UTC)));
		assertThat(firstEpisodePolicy.getAvailabilityEnd().withZone(DateTimeZones.UTC), is(new DateTime("2010-12-31T00:00:00.000Z").withZone(DateTimeZones.UTC)));
		assertThat(firstEpisodePolicy.getAvailableCountries(), is((Set<Country>) Sets.newHashSet(Countries.GB, Countries.IE)));
		
		Episode episodeWithABroadcast = (Episode) Iterables.get(episodes, 4);
		Version episodeWithABroadcastVersion = Iterables.get(episodeWithABroadcast.getVersions(), 0);
		Location locationWithBroadcast = Iterables.get(Iterables.get(episodeWithABroadcastVersion.getManifestedAs(), 0).getAvailableAt(), 0);
		
		assertThat(locationWithBroadcast.getPolicy().getAvailabilityStart(), is(new DateTime("2009-06-10T23:05:00.000Z")));
	}
}
