package org.atlasapi.remotesite.youview;

import javax.annotation.PostConstruct;

import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.atlasapi.persistence.content.schedule.mongo.ScheduleWriter;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.metabroadcast.common.scheduling.SimpleScheduler;

@Configuration
public class YouViewModule {

    private static final String YOUVIEW_PRODUCTION_ALIAS_PREFIX = "youview";
    private static final String YOUVIEW_STAGE_ALIAS_PREFIX = "youview_stage";
    private @Autowired SimpleScheduler scheduler;
    private @Autowired ChannelResolver channelResolver;
    private @Autowired ContentResolver contentResolver;
    private @Autowired ContentWriter contentWriter;
    private @Autowired ScheduleWriter scheduleWriter;
    private @Autowired ScheduleResolver scheduleResolver;
    
    private @Value("${youview.prod.url}") String youViewProductionUri;
    private @Value("${youview.stage.url}") String youViewStageUri;
    private @Value("${youview.timeout.seconds}") int timeout;
    
    @Bean
    public YouViewEnvironmentIngester youViewProductionIngester() {
        return new YouViewEnvironmentIngester(youViewProductionUri, 
                Duration.standardSeconds(timeout), scheduler, 
                channelResolver, contentResolver, contentWriter, 
                scheduleWriter, scheduleResolver, Publisher.YOUVIEW, 
                YOUVIEW_PRODUCTION_ALIAS_PREFIX);
    }
    
    @Bean
    public YouViewEnvironmentIngester youViewStageIngester() {
        return new YouViewEnvironmentIngester(youViewStageUri, 
                Duration.standardSeconds(timeout), scheduler, 
                channelResolver, contentResolver, contentWriter, 
                scheduleWriter, scheduleResolver, Publisher.YOUVIEW_STAGE,
                YOUVIEW_STAGE_ALIAS_PREFIX);
    }
    
    @PostConstruct
    public void scheduleTasks() {
        youViewProductionIngester().startBackgroundTasks();
        youViewStageIngester().startBackgroundTasks();
    }
    
    
}
