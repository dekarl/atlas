package org.atlasapi.remotesite.youview;


public class YouViewFortnightUpdater extends YouViewUpdater {

    public YouViewFortnightUpdater(YouViewChannelResolver channelResolver, YouViewScheduleFetcher fetcher, YouViewChannelProcessor processor) {
        super(channelResolver, fetcher, processor, 7, 7);
    }

}
