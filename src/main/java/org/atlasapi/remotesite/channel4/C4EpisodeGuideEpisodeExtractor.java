package org.atlasapi.remotesite.channel4;

import org.atlasapi.media.entity.Episode;
import org.atlasapi.remotesite.ContentExtractor;
import org.jdom.Element;

import com.metabroadcast.common.time.Clock;
import com.sun.syndication.feed.atom.Entry;

final class C4EpisodeGuideEpisodeExtractor extends BaseC4EpisodeExtractor implements
        ContentExtractor<Entry, Episode> {

    public C4EpisodeGuideEpisodeExtractor(Clock clock) {
        super(clock);
    }

    @Override
    public Episode extract(Entry source) {
        Episode basicEpisode = createBasicEpisode(source, C4AtomApi.foreignElementLookup(source));
        basicEpisode.addAlias(C4AtomApi.hierarchyEpisodeUri(source));
        return basicEpisode;
    }

    protected Element getMedia(Entry source) {
        return C4AtomApi.mediaContent(source);
    }

}
