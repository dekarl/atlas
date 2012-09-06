package org.atlasapi.remotesite.channel4;

import static org.atlasapi.media.entity.Publisher.C4;

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.atlasapi.persistence.system.RemoteSiteClient;
import org.atlasapi.remotesite.HttpClients;
import org.atlasapi.remotesite.channel4.epg.C4EpgUpdater;
import org.atlasapi.remotesite.channel4.epg.ScheduleResolverBroadcastTrimmer;
import org.atlasapi.remotesite.support.atom.AtomClient;
import org.joda.time.Duration;
import org.joda.time.LocalTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.metabroadcast.common.http.RequestLimitingSimpleHttpClient;
import com.google.common.base.Throwables;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpClientBuilder;
import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.scheduling.RepetitionRule;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.SimpleScheduler;
import com.metabroadcast.common.time.DayRangeGenerator;
import com.sun.syndication.feed.atom.Feed;

@Configuration
public class C4Module {

	private final static RepetitionRule BRAND_UPDATE_TIME = RepetitionRules.daily(new LocalTime(2, 0, 0));
	private final static RepetitionRule HIGHLIGHTS_UPDATE_TIME = RepetitionRules.daily(new LocalTime(10, 0, 0));
	private final static RepetitionRule TWO_HOURS = RepetitionRules.every(Duration.standardHours(2));

	private @Autowired SimpleScheduler scheduler;
	private @Value("${c4.apiKey}") String c4ApiKey;
	private @Value("${c4.lakeviewavailability.key}") String lakeviewAvailabilityFeedKey;
	private @Value("${c4.lakeviewavailability.apiroot}") String lakeviewApiRoot;

	private @Autowired @Qualifier("contentResolver") ContentResolver contentResolver;
	private @Autowired @Qualifier("contentWriter") ContentWriter contentWriter;
	private @Autowired AdapterLog log;
	private @Autowired ScheduleResolver scheduleResolver;
	private @Autowired ChannelResolver channelResolver;
	
	private @Value("${c4.keystore.path") String keyStorePath;
	private @Value("${c4.keystore.password}") String keyStorePass;
	
    @PostConstruct
    public void startBackgroundTasks() {
        if ("DISABLED".equals(c4ApiKey)) {
            log.record(new AdapterLogEntry(Severity.INFO).withSource(getClass()).withDescription("API key required for C4 updaters"));
            return;
        }
        scheduler.schedule(c4EpgUpdater(), TWO_HOURS);
        scheduler.schedule(c4AtozUpdater(), BRAND_UPDATE_TIME);
//        scheduler.schedule(c4HighlightsUpdater(), HIGHLIGHTS_UPDATE_TIME);
        log.record(new AdapterLogEntry(Severity.INFO).withDescription("C4 update scheduled tasks installed").withSource(getClass()));
    }

    @Bean C4AtomApi atomApi() {
        return new C4AtomApi(channelResolver);
    }
    
	@Bean public C4EpgUpdater c4EpgUpdater() {
	    ScheduleResolverBroadcastTrimmer trimmer = new ScheduleResolverBroadcastTrimmer(C4, scheduleResolver, contentResolver, lastUpdatedSettingContentWriter());
        return new C4EpgUpdater(atomApi(), httpsClient(), lastUpdatedSettingContentWriter(),
                contentResolver, c4BrandFetcher(), trimmer, log, new DayRangeGenerator().withLookAhead(7).withLookBack(7));
    }
	
    @Bean C4AtoZAtomContentLoader c4AtozUpdater() {
		return new C4AtoZAtomContentLoader(c4AtomFetcher(), c4BrandFetcher(), log);
	}
    
    @Bean C4LakeviewOnDemandFetcher c4LakeviewOnDemandFetcher() {
    	return new C4LakeviewOnDemandFetcher(c4XBoxAtomFetcher(), lakeviewApiRoot, log); 
    }

	protected @Bean RemoteSiteClient<Feed> c4AtomFetcher() {
	    return new ApiKeyAwareClient<Feed>(c4ApiKey, new AtomClient(httpsClient()));
	}
	
	protected @Bean SimpleHttpClient httpsClient() {
	    try {
	        URL jksFile = new File(keyStorePath).toURI().toURL();
            return HttpClients.httpsClient(jksFile, keyStorePass);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
	}

	protected @Bean C4AtomBackedBrandUpdater c4BrandFetcher() {
		return new C4AtomBackedBrandUpdater(c4AtomFetcher(), contentResolver, lastUpdatedSettingContentWriter(), channelResolver, c4LakeviewOnDemandFetcher(), log);
	}
	
	protected @Bean RemoteSiteClient<Feed> c4XBoxAtomFetcher() {
	    return new AtomClient(requestLimitedAuthHeaderSettingHttpClient());
	}
	
	protected @Bean SimpleHttpClient requestLimitedAuthHeaderSettingHttpClient() {
	    return new RequestLimitingSimpleHttpClient(new SimpleHttpClientBuilder()
        .withUserAgent("C4oD_iPad")
        .withSocketTimeout(50, TimeUnit.SECONDS)
        .withConnectionTimeout(10, TimeUnit.SECONDS)
        .withAcceptHeader(MimeType.TEXT_HTML)
        .withRetries(3)
        .withHeader("X-C4-API-Key", lakeviewAvailabilityFeedKey)
        .build(), 4);
	}
	
    @Bean protected LastUpdatedSettingContentWriter lastUpdatedSettingContentWriter() {
        return new LastUpdatedSettingContentWriter(contentResolver, new LastUpdatedCheckingContentWriter(log, contentWriter));
    }
    
}
