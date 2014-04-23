package org.atlasapi.remotesite;

import javax.annotation.PostConstruct;

import org.atlasapi.persistence.content.ContentGroupWriter;
import org.atlasapi.persistence.content.ContentResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.SimpleScheduler;

@Configuration
public class CreateYouTubeContentGroupModule {

    @Autowired
    private ContentResolver contentResolver;
    
    @Autowired
    private ContentGroupWriter contentGroupWriter;
    
    @Autowired
    private SimpleScheduler scheduler;
    
    @Bean
    public CreateYouTubeContentGroupTask createYouTubeContentGroupTask() {
        return new CreateYouTubeContentGroupTask(contentResolver, contentGroupWriter);
    }
    
    @PostConstruct
    public void scheduleTasks() {
        scheduler.schedule(createYouTubeContentGroupTask(), RepetitionRules.NEVER);
    }
}
