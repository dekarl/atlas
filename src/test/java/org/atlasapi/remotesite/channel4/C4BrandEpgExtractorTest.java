package org.atlasapi.remotesite.channel4;

import java.util.List;

import junit.framework.TestCase;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.logging.NullAdapterLog;
import org.atlasapi.remotesite.channel4.epg.C4EpgEntryItemExtractor;
import org.junit.Test;

import com.google.common.io.Resources;
import com.metabroadcast.common.time.SystemClock;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;

public class C4BrandEpgExtractorTest extends TestCase {
    
    private final AtomFeedBuilder gleeAtom = new AtomFeedBuilder(Resources.getResource(getClass(), "glee-epg.atom"));
    private final Feed gleeFeed = gleeAtom.build();
    
    private final AtomFeedBuilder xmenAtom = new AtomFeedBuilder(Resources.getResource(getClass(), "x-men-the-last-stand-epg.atom"));
    private final Feed xmenFeed = xmenAtom.build();
    
    private final C4EpgEpisodeExtractor extractor = new C4EpgEpisodeExtractor(new SystemClock());

    @Test
    public void testShouldExtractBroadcast() throws Exception {

        Episode episode = extractor.extract((Entry)gleeFeed.getEntries().get(0));
        
        assertEquals("http://www.channel4.com/programmes/glee/episode-guide/series-1/episode-5", episode.getCanonicalUri());
        assertEquals(Integer.valueOf(1), episode.getSeriesNumber());
        assertEquals(Integer.valueOf(5), episode.getEpisodeNumber());
        
        assertFalse(episode.getVersions().isEmpty());
        assertEquals(1, episode.getVersions().size());
        
        Version version = episode.getVersions().iterator().next();
        assertFalse(version.getBroadcasts().isEmpty());
        assertEquals(1, version.getBroadcasts().size());
        
        Broadcast broadcast = version.getBroadcasts().iterator().next();
        
        assertTrue(broadcast.getAliases().contains("tag:www.channel4.com,2009:slot/E439861"));
        assertEquals(Integer.valueOf(55*60), broadcast.getBroadcastDuration());
    }
    
    @Test
    public void testShouldConstructHeirarchicalUri() {
        
        Episode episode = extractor.extract((Entry)xmenFeed.getEntries().get(0));
        
        assertEquals("http://www.channel4.com/programmes/x-men-the-last-stand/episode-guide/series-1/episode-1", episode.getCanonicalUri());
    }
}
