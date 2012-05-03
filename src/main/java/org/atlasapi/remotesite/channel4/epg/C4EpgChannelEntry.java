package org.atlasapi.remotesite.channel4.epg;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.remotesite.channel4.epg.model.C4EpgEntry;

public class C4EpgChannelEntry {

    private final C4EpgEntry epgEntry;
    private final Channel entryChannel;

    public C4EpgChannelEntry(C4EpgEntry epgEntry, Channel entryChannel) {
        this.epgEntry = epgEntry;
        this.entryChannel = entryChannel;
    }

    public C4EpgEntry getEpgEntry() {
        return this.epgEntry;
    }

    public Channel getEntryChannel() {
        return this.entryChannel;
    }

    public boolean hasRelatedLink() {
        return epgEntry.hasRelatedLink();
    }

    public String getRelatedLinkUri() {
        return epgEntry.getRelatedLink();
    }
    
}
