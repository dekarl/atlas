package org.atlasapi.remotesite.channel4;

import java.util.List;

import org.atlasapi.media.entity.Episode;
import org.atlasapi.remotesite.SiteSpecificAdapter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.metabroadcast.common.time.Clock;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;

public class C4OdEpisodesAdapter implements SiteSpecificAdapter<List<Episode>> {

    private final C4AtomApiClient feedClient;
    private final C4OnDemandEpisodeExtractor itemExtractor;

    public C4OdEpisodesAdapter(C4AtomApiClient feedClient, Clock clock) {
        this.feedClient = feedClient;
        this.itemExtractor = new C4OnDemandEpisodeExtractor(clock);
    }

    @Override
    public List<Episode> fetch(String uri) {
        Preconditions.checkArgument(canFetch(uri));
        Optional<Feed> brand4oDFeed = feedClient.brand4oDFeed(uri);
        
        List<Episode> episodes = Lists.newArrayList();
        
        if (brand4oDFeed.isPresent()) {
            for (Object entry : brand4oDFeed.get().getEntries()) {
                Episode episode = itemExtractor.extract(((Entry)entry));
                if (episode != null) {
                    episodes.add(episode);
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
