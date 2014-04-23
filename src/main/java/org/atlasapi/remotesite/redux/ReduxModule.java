package org.atlasapi.remotesite.redux;

import static org.atlasapi.remotesite.redux.HttpBackedReduxClient.reduxClientForHost;
import static org.atlasapi.remotesite.redux.ReduxLatestUpdateTasks.completeReduxLatestTask;
import static org.atlasapi.remotesite.redux.ReduxLatestUpdateTasks.untilFoundReduxLatestTask;

import java.text.ParseException;

import javax.annotation.PostConstruct;

import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.logging.AdapterLog;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.net.HostSpecifier;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.SimpleScheduler;

@Configuration
public class ReduxModule {

    private @Autowired @Qualifier("contentResolver") ContentResolver resolver;
    private @Autowired ContentWriter writer;
    private @Autowired ChannelResolver channelResolver;
    private @Autowired AdapterLog log;
    private @Autowired SimpleScheduler taskScheduler;

    private @Value("${redux.host}") String reduxHost;
    
    
    protected @Bean ReduxClient reduxClient() {
        try {
            return reduxClientForHost(HostSpecifier.from(reduxHost)).build();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
    
    @PostConstruct
    public void scheduleTasks() {
//        taskScheduler.schedule(maximumReduxLatestTask(1000, reduxClient(), writer, reduxProgrammeAdapter(), log).withName("Redux Latest 1000 updater"), RepetitionRules.NEVER);
//        taskScheduler.schedule(ReduxLatestUpdateTasks.firstBatchOnlyReduxLatestTask(reduxClient(), writer, reduxProgrammeAdapter(), log).withName("Redux Latest First Batch updater"), RepetitionRules.NEVER);
        taskScheduler.schedule(untilFoundReduxLatestTask(reduxClient(), writer, reduxProgrammeAdapter(), channelResolver, log, resolver).withName("Redux Until Found Updater"), RepetitionRules.every(Duration.standardHours(1)));
        taskScheduler.schedule(completeReduxLatestTask(reduxClient(), writer, reduxProgrammeAdapter(), channelResolver, log).withName("Redux Complete Latest updater"), RepetitionRules.NEVER);
    }

    @Bean
    public ReduxUpdateController reduxUpdateController() {
        return new ReduxUpdateController(reduxClient(), writer, reduxProgrammeAdapter(), log);
    }

    @Bean
    protected DefaultReduxProgrammeAdapter reduxProgrammeAdapter() {
        return new DefaultReduxProgrammeAdapter(reduxClient(), new FullProgrammeItemExtractor(channelResolver, log));
    }

}
