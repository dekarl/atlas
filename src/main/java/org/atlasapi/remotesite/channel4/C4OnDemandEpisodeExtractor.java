package org.atlasapi.remotesite.channel4;

import java.util.Map;

import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Version;
import org.atlasapi.remotesite.ContentExtractor;
import org.jdom.Element;
import org.joda.time.DateTime;

import com.metabroadcast.common.time.Clock;
import com.sun.syndication.feed.atom.Entry;

final class C4OnDemandEpisodeExtractor extends BaseC4EpisodeExtractor implements
        ContentExtractor<Entry, Episode> {

    private final C4AtomEntryVersionExtractor versionExtractor = new C4AtomEntryVersionExtractor();

    public C4OnDemandEpisodeExtractor(Clock clock) {
        super(clock);
    }

    @Override
    public Episode extract(Entry source) {
        Map<String, String> foreignElements = C4AtomApi.foreignElementLookup(source);
        Episode episode = createBasicEpisode(source, foreignElements);
        
        String fourOdUri = C4AtomApi.fourOdUri(source);
        if (fourOdUri != null) {
            episode.addAlias(fourOdUri);
        }
        
        episode.addVersion(setLastUpdated(versionExtractor.extract(source), episode.getLastUpdated()));
        
        return episode;
    }
    
    private Version setLastUpdated(Version version, DateTime lastUpdated) {
        version.setLastUpdated(lastUpdated);
        for (Encoding encoding : version.getManifestedAs()) {
            encoding.setLastUpdated(lastUpdated);
            for (Location location : encoding.getAvailableAt()) {
                location.setLastUpdated(lastUpdated);
            }
        }
        return version;
    }

    @Override
    protected Element getMedia(Entry source) {
        return C4AtomApi.mediaGroup(source);
    }
    
}
