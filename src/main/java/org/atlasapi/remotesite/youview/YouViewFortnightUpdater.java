package org.atlasapi.remotesite.youview;

import org.joda.time.Duration;

public class YouViewFortnightUpdater extends YouViewUpdater {

    public YouViewFortnightUpdater(YouViewChannelResolver channelResolver, YouViewScheduleFetcher fetcher, YouViewXmlElementHandler elementHandler) {
        super(channelResolver, fetcher, elementHandler, Duration.standardDays(7), Duration.standardDays(7));
    }

}
