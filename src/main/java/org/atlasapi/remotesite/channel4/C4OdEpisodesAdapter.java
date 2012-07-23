package org.atlasapi.remotesite.channel4;

import java.util.List;

import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Policy.Platform;
import org.atlasapi.remotesite.SiteSpecificAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.metabroadcast.common.time.Clock;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;

public class C4OdEpisodesAdapter implements SiteSpecificAdapter<List<Episode>> {
    
    private static Logger log = LoggerFactory.getLogger(C4OdEpisodesAdapter.class);

    private final C4AtomApiClient feedClient;
    private final C4OnDemandEpisodeExtractor itemExtractor;
    private final Optional<Platform> platform;

    public C4OdEpisodesAdapter(C4AtomApiClient feedClient, Optional<Platform> platform, Clock clock) {
        this.feedClient = feedClient;
        this.platform = platform;
        this.itemExtractor = new C4OnDemandEpisodeExtractor(platform, clock);
    }

    @Override
    public List<Episode> fetch(String uri) {
        Preconditions.checkArgument(canFetch(uri));
        Optional<Feed> brand4oDFeed = feedClient.brand4oDFeed(uri);
        
        List<Episode> episodes = Lists.newArrayList();
        
        if (brand4oDFeed.isPresent()) {
            for (Object entry : brand4oDFeed.get().getEntries()) {
                Entry oDentry = (Entry)entry;
                try {
                    Episode episode = itemExtractor.extract(oDentry);
                    if (episode != null) {
                        episodes.add(episode);
                    }
                } catch (Exception e) {
                    log.warn("Failed to extract Episode from entry " + oDentry.getId() , e);
                }
            }
        }
        
        return episodes;
    }

    @Override
    public boolean canFetch(String uri) {
        return C4AtomApi.isACanonicalBrandUri(uri);
    }

}
