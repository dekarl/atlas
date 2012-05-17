package org.atlasapi.remotesite.channel4;

import java.util.Map;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Version;
import org.atlasapi.remotesite.ContentExtractor;
import org.jdom.Element;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.DateTimeZones;
import com.sun.syndication.feed.atom.Entry;

public class C4EpgEpisodeExtractor extends BaseC4EpisodeExtractor implements
        ContentExtractor<Entry, Episode> {
    
    private static final String DC_START_TIME = "dc:relation.TXStartTime";
    private static final String DC_TX_CHANNEL = "dc:relation.TXChannel";

    private static final DateTimeFormatter fmt = DateTimeFormat.forPattern("EEEE dd MMMM yyyy hh:mm aa").withZone(DateTimeZones.LONDON);
    private static Map<String, String> CHANNEL_LOOKUP = ImmutableMap.<String, String>builder()
        .put("C4", "http://www.channel4.com")
        .put("M4", "http://www.channel4.com/more4")
        .put("E4", "http://www.e4.com")
        .put("4M", "http://www.4music.com")
        .build();

    public C4EpgEpisodeExtractor(Clock clock) {
        super(clock);
    }

    @Override // epg.atom entries don't have media groups/content, so no images.
    protected Element getMedia(Entry source) {
        return null;
    }

    @Override
    public Episode extract(Entry source) {

        Map<String, String> lookup = C4AtomApi.foreignElementLookup(source);

        Episode episode = createBasicEpisode(source, lookup);

        Version version = new Version();
        episode.setVersions(Sets.newHashSet(version));

        String txChannel = CHANNEL_LOOKUP.get(lookup.get(DC_TX_CHANNEL));
        String startTime = lookup.get(DC_START_TIME);
        Duration duration = C4AtomApi.durationFrom(lookup);

        if (txChannel != null) {
            DateTime txStart = fmt.parseDateTime(startTime);
            Broadcast broadcast = C4BroadcastBuilder.broadcast()
                    .withChannel(txChannel)
                    .withTransmissionStart(txStart)
                    .withDuration(duration)
                    .withAtomId(source.getId())
                    .build();
            
            broadcast.setLastUpdated(episode.getLastUpdated());
            version.addBroadcast(broadcast);
        }

        return episode;
    }

    @Override
    protected String extractEpisodeUri(Entry source, Map<String, String> lookup) {
        return C4AtomApi.canonicalUri(source);
    }

}
