package org.atlasapi.remotesite.channel4;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.remotesite.SiteSpecificAdapter;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.metabroadcast.common.time.Clock;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;

public class C4BrandEpgAdapter implements SiteSpecificAdapter<List<Episode>> {

    private final Log log = LogFactory.getLog(getClass());
    
    private final C4AtomApiClient client;
    private final C4EpgEpisodeExtractor extractor;

    public C4BrandEpgAdapter(C4AtomApiClient client, Clock clock) {
        this.client = client;
        this.extractor = new C4EpgEpisodeExtractor(clock);
    }
    
    @Override
    public List<Episode> fetch(String uri) {
        
        Optional<Feed> epgFeed = client.brandEpgFeed(uri);

        List<Episode> episodes = Lists.newArrayList();
        
        if (epgFeed.isPresent()) {
            for (Object entry : epgFeed.get().getEntries()) {
                Entry atomEntry = (Entry)entry;
                try {
                    episodes.add(extractor.extract(atomEntry));
                } catch (Exception e) {
                    log.warn("Failed to extract episode from " + atomEntry.getId(), e);
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
