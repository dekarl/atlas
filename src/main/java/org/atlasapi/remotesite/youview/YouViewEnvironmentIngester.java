package org.atlasapi.remotesite.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.atlasapi.persistence.content.schedule.mongo.ScheduleWriter;
import org.atlasapi.remotesite.channel4.epg.ScheduleResolverBroadcastTrimmer;
import org.joda.time.Duration;
import org.springframework.context.annotation.Configuration;

import com.google.common.primitives.Ints;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.RepetitionRules.Every;
import com.metabroadcast.common.scheduling.SimpleScheduler;

@Configuration
public class YouViewEnvironmentIngester {
    
    private final static Every EVERY_15_MINUTES = RepetitionRules.every(Duration.standardMinutes(15));
    private final static Every EVERY_HOUR = RepetitionRules.every(Duration.standardHours(1));
    
    private final SimpleScheduler scheduler;
    private final YouViewChannelProcessor youViewChannelProcessor;
    private final YouViewScheduleFetcher youViewScheduleFetcher;
    private final YouViewElementProcessor youViewElementProcessor;
    private final YouViewChannelResolver youViewChannelResolver;
    private Publisher publisher;
    
    /**
     * 
     * @param youViewUri        YouView environment URI base
     * @param timeout           Timeout for requests to YouView API
     * @param scheduler         Scheduler with which to register scheduled tasks
     * @param channelResolver   
     * @param contentResolver
     * @param contentWriter
     * @param scheduleWriter
     * @param scheduleResolver  
     * @param publisher         The publisher to write content as
     * @param aliasPrefix       Aliases are maintained for schedule events and programmes. They are prefixed
     *                          with this value, eg. prefix:scheduleevent. This value should not contain the 
     *                          colon separator
     */
    public YouViewEnvironmentIngester(String youViewUri, Duration timeout, 
            SimpleScheduler scheduler, ChannelResolver channelResolver, ContentResolver contentResolver, 
            ContentWriter contentWriter, ScheduleWriter scheduleWriter, ScheduleResolver scheduleResolver, 
            Publisher publisher, String aliasPrefix) {
        
        this.publisher = checkNotNull(publisher);
        this.scheduler = checkNotNull(scheduler);
        this.youViewChannelResolver = new DefaultYouViewChannelResolver(channelResolver);
        this.youViewScheduleFetcher = new YouViewScheduleFetcher(youViewUri, Ints.saturatedCast(timeout.getStandardSeconds()));
        this.youViewElementProcessor = new DefaultYouViewElementProcessor(
                                                new YouViewContentExtractor(youViewChannelResolver, publisher, aliasPrefix), 
                                                contentResolver, contentWriter
                                       );
        this.youViewChannelProcessor = new DefaultYouViewChannelProcessor(scheduleWriter, youViewElementProcessor, new ScheduleResolverBroadcastTrimmer(publisher, scheduleResolver, contentResolver, contentWriter), publisher);
    }
    
    public void startBackgroundTasks() {
        scheduler.schedule(youViewTodayUpdater().withName("YouView [" + publisher.name() + "] Today Updater"), EVERY_15_MINUTES);
        scheduler.schedule(youViewFornightUpdater().withName("YouView [" + publisher.name() + "] Updater ±7 Days"), EVERY_HOUR);
    }

    private YouViewFortnightUpdater youViewFornightUpdater() {
        return new YouViewFortnightUpdater(youViewChannelResolver, youViewScheduleFetcher, youViewChannelProcessor);
    }
        
    private YouViewTodayUpdater youViewTodayUpdater() {
        return new YouViewTodayUpdater(youViewChannelResolver, youViewScheduleFetcher, youViewChannelProcessor);
    }
}
