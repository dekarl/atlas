package org.atlasapi.remotesite.youview;

import javax.annotation.PostConstruct;

import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;

import com.metabroadcast.common.properties.Configurer;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.RepetitionRules.Every;
import com.metabroadcast.common.scheduling.SimpleScheduler;

public class YouViewModule {
    private final static Every EVERY_15_MINUTES = RepetitionRules.every(Duration.standardMinutes(15));
    private final static Every EVERY_HOUR = RepetitionRules.every(Duration.standardHours(1));
    
    private @Autowired SimpleScheduler scheduler;
    private @Autowired ChannelResolver channelResolver;
    private @Autowired ContentResolver contentResolver;
    private @Autowired ContentWriter contentWriter;
    
    @PostConstruct
    public void startBackgroundTasks() {
        scheduler.schedule(youViewTodayUpdater().withName("YouView Today Updater"), EVERY_15_MINUTES);
        scheduler.schedule(youViewFornightUpdater().withName("YouView Updater ±7 Days"), EVERY_HOUR);
    }

    private YouViewFortnightUpdater youViewFornightUpdater() {
        return new YouViewFortnightUpdater(channelResolver(), youViewFetcher(), youViewXmlElementHandler());
    }

    private YouViewTodayUpdater youViewTodayUpdater() {
        return new YouViewTodayUpdater(channelResolver(), youViewFetcher(), youViewXmlElementHandler());
    }
    
    private YouViewScheduleFetcher youViewFetcher() {
        String url = Configurer.get("youview.url").get();
        int timeout = Configurer.get("youview.timeout").toInt();
        return new YouViewScheduleFetcher(url, timeout);
    }
    
    private YouViewXmlElementHandler youViewXmlElementHandler() {
        YouViewContentExtractor extractor = new YouViewContentExtractor(channelResolver());
        return new DefaultYouViewXmlElementHandler(extractor, contentResolver, contentWriter);
    }

    private YouViewChannelResolver channelResolver() {
        return new DefaultYouViewChannelResolver(channelResolver);
    }
}
