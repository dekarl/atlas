package org.atlasapi.remotesite.youview;

import org.joda.time.Duration;

public class YouViewTodayUpdater extends YouViewUpdater {

    public YouViewTodayUpdater(YouViewChannelResolver channelResolver, YouViewScheduleFetcher fetcher, YouViewXmlElementHandler elementHandler) {
        super(channelResolver, fetcher, elementHandler, Duration.standardDays(0), Duration.standardDays(1));
    }

}
