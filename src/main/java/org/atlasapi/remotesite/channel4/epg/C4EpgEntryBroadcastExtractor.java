package org.atlasapi.remotesite.channel4.epg;

import static org.atlasapi.remotesite.channel4.C4BroadcastBuilder.broadcast;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.remotesite.ContentExtractor;
import org.atlasapi.remotesite.channel4.epg.model.C4EpgEntry;

public class C4EpgEntryBroadcastExtractor implements ContentExtractor<C4EpgChannelEntry, Broadcast> {

    @Override
    public Broadcast extract(C4EpgChannelEntry source) {
        C4EpgEntry entry = source.getEpgEntry();
        Broadcast broadcast = broadcast().withChannel(source.getEntryChannel().getCanonicalUri())
                .withTransmissionStart(entry.txDate()).withDuration(entry.duration())
                .withAtomId(entry.id()).build();

        broadcast.setSubtitled(entry.subtitles());
        broadcast.setAudioDescribed(entry.audioDescription());
        broadcast.setWidescreen(entry.wideScreen());
        broadcast.setSigned(entry.signing());
        broadcast.setRepeat(entry.repeat());
        broadcast.setIsActivelyPublished(true);

        return broadcast;
    }

}
