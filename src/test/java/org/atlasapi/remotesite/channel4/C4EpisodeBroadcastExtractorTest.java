package org.atlasapi.remotesite.channel4;

import java.util.List;

import junit.framework.TestCase;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.logging.NullAdapterLog;
import org.junit.Test;

import com.google.common.io.Resources;
import com.sun.syndication.feed.atom.Feed;

public class C4EpisodeBroadcastExtractorTest extends TestCase {
    
    private final AtomFeedBuilder gleeAtom = new AtomFeedBuilder(Resources.getResource(getClass(), "glee-epg.atom"));
    private final Feed gleeFeed = gleeAtom.build();
    private final C4EpisodeBroadcastExtractor extractor = new C4EpisodeBroadcastExtractor(new NullAdapterLog());

    @Test
    public void testShouldExtractBroadcast() throws Exception {
        List<Episode> episodes = extractor.extract(gleeFeed);
        
        assertFalse(episodes.isEmpty());
        assertEquals(1, episodes.size());
        
        Episode episode = episodes.get(0);
        assertEquals("http://www.channel4.com/programmes/glee/episode-guide/series-1/episode-5", episode.getCanonicalUri());
        assertEquals(Integer.valueOf(1), episode.getSeriesNumber());
        assertEquals(Integer.valueOf(5), episode.getEpisodeNumber());
        
        assertFalse(episode.getVersions().isEmpty());
        assertEquals(1, episode.getVersions().size());
        
        Version version = episode.getVersions().iterator().next();
        assertFalse(version.getBroadcasts().isEmpty());
        assertEquals(1, version.getBroadcasts().size());
        
        Broadcast broadcast = version.getBroadcasts().iterator().next();
        
        assertTrue(broadcast.getAliasUrls().contains("tag:www.channel4.com,2009:slot/E439861"));
        assertEquals(Integer.valueOf(55*60), broadcast.getBroadcastDuration());
    }
}
