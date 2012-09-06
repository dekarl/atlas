package org.atlasapi.remotesite.channel4;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.atom.Link;

public class C4AtoZAtomContentUpdateTask extends ScheduledTask {
	
    private final Log log = LogFactory.getLog(getClass());
    
    private final Iterable<Optional<Feed>> atozFeeds;
    private final C4BrandUpdater brandUpdater;
	
	private final C4LinkBrandNameExtractor linkExtractor = new C4LinkBrandNameExtractor();

	public C4AtoZAtomContentUpdateTask(SimpleHttpClient client, String apiBaseUrl, C4BrandUpdater brandUpdater) {
	    this(client, apiBaseUrl, Optional.<String>absent(), brandUpdater);
	}
	
    public C4AtoZAtomContentUpdateTask(SimpleHttpClient client, String apiBaseUrl, Optional<String> platform, C4BrandUpdater brandUpdater) {
        this.brandUpdater = brandUpdater;
		this.atozFeeds = feedSource(client, apiBaseUrl, platform);
    }

    private Iterable<Optional<Feed>> feedSource(final SimpleHttpClient client, final String apiBaseUrl, final Optional<String> platform) {
        return new Iterable<Optional<Feed>>() {
            @Override
            public Iterator<Optional<Feed>> iterator() {
                return new C4AtoZFeedIterator(client, apiBaseUrl, platform);
            }
        };
    }

    @Override
    public void runTask() {
        for (Optional<Feed> feed : atozFeeds) {
            if (feed.isPresent()) {
                loadAndSaveFromFeed(feed.get());
            }
        }
    }

	@SuppressWarnings("unchecked")
	private void loadAndSaveFromFeed(Feed feed) {
		for (Entry entry: (List<Entry>) feed.getEntries()) {
		    String brandUri = extractUriFromLinks(entry);
		    if (brandUri != null && brandUpdater.canFetch(brandUri)) {
		        writeBrand(brandUri);
		    }
		}
	}

    @SuppressWarnings("unchecked")
    private String extractUriFromLinks(Entry entry) {
        for (Object link : Iterables.concat(entry.getAlternateLinks(), entry.getOtherLinks())) {
            Optional<String> extracted = linkExtractor.canonicalBrandUriFrom(((Link)link).getHref());
            if (extracted.isPresent()) {
                return extracted.get();
            }
        }
        return null;
    }

    private void writeBrand(String brandUri) {
        try {
             brandUpdater.createOrUpdateBrand(brandUri);
        } catch (Exception e) {
            log.error("Failed to update brand " + brandUri, e);
        }
    }
    
}
