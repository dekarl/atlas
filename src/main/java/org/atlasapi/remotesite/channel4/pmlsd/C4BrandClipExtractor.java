package org.atlasapi.remotesite.channel4.pmlsd;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.atlasapi.media.entity.Clip;
import org.atlasapi.media.entity.Policy.Platform;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;
import com.metabroadcast.common.time.Clock;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;

public class C4BrandClipExtractor extends C4MediaItemExtractor<Clip> {

    private final ContentFactory<Feed, Feed, Entry> contentFactory;
    private final C4AtomEntryVersionExtractor versionExtractor;
    private final C4AtomFeedUriExtractor uriExtractor = new C4AtomFeedUriExtractor();
    private final Publisher publisher;

    public C4BrandClipExtractor(ContentFactory<Feed, Feed, Entry> contentFactory, Publisher publisher, 
            Clock clock) {
        super(clock);
        this.contentFactory = contentFactory;
        this.publisher = publisher;
        // TODO: Do we have platform-specific clips?
        versionExtractor = new C4AtomEntryVersionExtractor(Optional.<Platform>absent());
    }

    @Override
    protected Clip createItem(Entry entry, Map<String, String> lookup) {
        return contentFactory.createClip(entry).get();
    }
    
    @Override
    protected Clip setAdditionalItemFields(Entry entry, Map<String, String> lookup, Clip clip) {
        String fourOdUri = C4AtomApi.fourOdUri(entry);
        if (fourOdUri != null) {
            clip.addAliasUrl(fourOdUri);
        }
        clip.setIsLongForm(false);
        clip.setClipOf(possibleSeriesAndEpisodeNumberFrom(lookup).orNull());
        clip.addVersion(versionExtractor.extract(data(entry, fourOdUri, lookup, clip.getLastUpdated())));
        return clip;
    }

    private C4VersionData data(Entry entry, String fourOdUri, Map<String, String> lookup, DateTime lastUpdated) {
        String uri = fourOdUri != null ? fourOdUri : uriExtractor.uriForClip(publisher, entry).get();
        checkNotNull(uri, "No version URI extracted for %s", entry.getId());
        return new C4VersionData(entry.getId(), uri, getMedia(entry), lookup, lastUpdated);
    }
    
    private Optional<String> possibleSeriesAndEpisodeNumberFrom(Map<String, String> lookup) {
        Integer episode = Ints.tryParse(Strings.nullToEmpty(lookup.get(C4AtomApi.DC_EPISODE_NUMBER)));
        Integer series = Ints.tryParse(Strings.nullToEmpty(lookup.get(C4AtomApi.DC_SERIES_NUMBER)));
        
        if (episode == null || series == null) {
            return Optional.absent();
        }
        
        return Optional.of(String.format("%d-%d", series, episode));
    }
    
}
