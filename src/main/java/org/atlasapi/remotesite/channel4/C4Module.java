package org.atlasapi.remotesite.channel4;

import static org.atlasapi.media.entity.Publisher.C4;

import java.io.File;
import java.net.URL;

import javax.annotation.PostConstruct;

import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Policy.Platform;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.atlasapi.remotesite.HttpClients;
import org.atlasapi.remotesite.channel4.epg.C4EpgChannelDayUpdater;
import org.atlasapi.remotesite.channel4.epg.C4EpgClient;
import org.atlasapi.remotesite.channel4.epg.C4EpgUpdater;
import org.atlasapi.remotesite.channel4.epg.ScheduleResolverBroadcastTrimmer;
import org.joda.time.Duration;
import org.joda.time.LocalTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.scheduling.RepetitionRule;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.SimpleScheduler;
import com.metabroadcast.common.time.DayRangeGenerator;

@Configuration
public class C4Module {
    

    private static final String ATOZ_BASE = "https://pmlsc.channel4.com/pmlsd/";
    private static final String P06_PLATFORM = "p06";

	private final static RepetitionRule BRAND_UPDATE_TIME = RepetitionRules.daily(new LocalTime(2, 0, 0));
	private final static RepetitionRule XBOX_UPDATE_TIME = RepetitionRules.daily(new LocalTime(1, 0, 0));
	private final static RepetitionRule TWO_HOURS = RepetitionRules.every(Duration.standardHours(2));

	private @Autowired SimpleScheduler scheduler;

	private @Autowired @Qualifier("contentResolver") ContentResolver contentResolver;
	private @Autowired @Qualifier("contentWriter") ContentWriter contentWriter;
	private @Autowired AdapterLog log;
	private @Autowired ScheduleResolver scheduleResolver;
	private @Autowired ChannelResolver channelResolver;
	
	private @Value("${c4.keystore.path}") String keyStorePath;
	private @Value("${c4.keystore.password}") String keyStorePass;
	
    @PostConstruct
    public void startBackgroundTasks() {
        scheduler.schedule(c4EpgUpdater(), TWO_HOURS);
        scheduler.schedule(pcC4AtozUpdater().withName("C4 4OD PC Updater"), BRAND_UPDATE_TIME);
        scheduler.schedule(xboxC4AtozUpdater().withName("C4 4OD XBox Updater"), XBOX_UPDATE_TIME);
        log.record(new AdapterLogEntry(Severity.INFO).withDescription("C4 update scheduled tasks installed").withSource(getClass()));
    }

    @Bean C4AtomApi atomApi() {
        return new C4AtomApi(channelResolver);
    }
    
	@Bean public C4EpgUpdater c4EpgUpdater() {
	    return new C4EpgUpdater(atomApi(), c4EpgChannelDayUpdater(), log, new DayRangeGenerator().withLookAhead(7).withLookBack(7));
    }
	
	@Bean public C4EpgChannelDayUpdater c4EpgChannelDayUpdater() {
	    ScheduleResolverBroadcastTrimmer trimmer = new ScheduleResolverBroadcastTrimmer(C4, scheduleResolver, contentResolver, lastUpdatedSettingContentWriter());
	    return new C4EpgChannelDayUpdater(new C4EpgClient(httpsClient()), lastUpdatedSettingContentWriter(),
                contentResolver, c4BrandFetcher(Optional.<Platform>absent(),Optional.<String>absent()), trimmer, log);
	}
	
	@Bean protected C4AtoZAtomContentUpdateTask pcC4AtozUpdater() {
		return new C4AtoZAtomContentUpdateTask(httpsClient(), ATOZ_BASE, c4BrandFetcher(Optional.<Platform>absent(),Optional.<String>absent()));
	}
	
	@Bean protected C4AtoZAtomContentUpdateTask xboxC4AtozUpdater() {
	    return new C4AtoZAtomContentUpdateTask(httpsClient(), ATOZ_BASE, Optional.of(Platform.XBOX), c4BrandFetcher(Optional.of(Platform.XBOX),Optional.of(P06_PLATFORM)));
	}
	
    @Bean protected SimpleHttpClient httpsClient() {
	    try {
	        URL jksFile = new File(keyStorePath).toURI().toURL();
            return HttpClients.httpsClient(jksFile, keyStorePass);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
	}
	
	protected C4AtomBackedBrandUpdater c4BrandFetcher(Optional<Platform> platform, Optional<String> platformParam) {
	    C4AtomApiClient client = new C4AtomApiClient(httpsClient(), ATOZ_BASE, platformParam);
	    C4BrandExtractor extractor = new C4BrandExtractor(client, platform, channelResolver);
		return new C4AtomBackedBrandUpdater(client, platform, contentResolver, lastUpdatedSettingContentWriter(), extractor);
	}
	
	@Bean protected C4BrandUpdateController c4BrandUpdateController() {
	    return new C4BrandUpdateController(c4BrandFetcher(Optional.<Platform>absent(),Optional.<String>absent()), 
	            ImmutableMap.of(Platform.XBOX, c4BrandFetcher(Optional.of(Platform.XBOX),Optional.of(P06_PLATFORM))));
	}
	
	@Bean protected C4EpgChannelDayUpdateController c4EpgChannelDayUpdateController() {
	    return new C4EpgChannelDayUpdateController(atomApi(), c4EpgChannelDayUpdater());
	}
	
    @Bean protected LastUpdatedSettingContentWriter lastUpdatedSettingContentWriter() {
        return new LastUpdatedSettingContentWriter(contentResolver, new LastUpdatedCheckingContentWriter(log, contentWriter));
    }
    
}
