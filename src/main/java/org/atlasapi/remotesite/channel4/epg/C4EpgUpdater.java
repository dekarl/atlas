package org.atlasapi.remotesite.channel4.epg;

import java.util.Map;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.atlasapi.remotesite.channel4.C4AtomApi;
import org.atlasapi.remotesite.channel4.C4BrandUpdater;
import org.atlasapi.remotesite.redux.UpdateProgress;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import com.google.common.collect.BiMap;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.DayRange;
import com.metabroadcast.common.time.DayRangeGenerator;

public class C4EpgUpdater extends ScheduledTask {

    private final C4AtomApi c4AtomApi;
    private final DayRangeGenerator rangeGenerator;
    private final AdapterLog log;

    private C4EpgChannelDayUpdater channelDayUpdater;

    public C4EpgUpdater(C4AtomApi atomApi, SimpleHttpClient client, ContentWriter writer, ContentResolver resolver, C4BrandUpdater brandUpdater, BroadcastTrimmer trimmer, AdapterLog log, DayRangeGenerator generator) {
        this.c4AtomApi = atomApi;
        this.log = log;
        this.channelDayUpdater = new C4EpgChannelDayUpdater(new C4EpgClient(client), writer, resolver, brandUpdater, trimmer, log);
        this.rangeGenerator = generator;
    }

    @Override
    protected void runTask() {
        DateTime start = new DateTime(DateTimeZones.UTC);
        log.record(new AdapterLogEntry(Severity.INFO).withSource(getClass()).withDescription("C4 EPG Update initiated"));
        
        DayRange dayRange = rangeGenerator.generate(new LocalDate(DateTimeZones.UTC));
        
        BiMap<String, Channel> channelMap = c4AtomApi.getChannelMap();
		int total = Iterables.size(dayRange) * channelMap.size();
        int processed = 0;
        UpdateProgress progress = UpdateProgress.START;
        
        for (Map.Entry<String, Channel> channelEntry : channelMap.entrySet()) {
            for (LocalDate scheduleDay : dayRange) {
                reportStatus(String.format("Processing %s/%s. %s failures. %s broadcasts processed", processed++, total, progress.getFailures(), progress.getProcessed()));
                progress = progress.reduce(channelDayUpdater.update(channelEntry.getKey(), channelEntry.getValue(), scheduleDay));
            }
        }
        
        reportStatus(String.format("Processed %s/%s. %s failures. %s broadcasts processed", processed++, total, progress.getFailures(), progress.getProcessed()));
        String runTime = new Period(start, new DateTime(DateTimeZones.UTC)).toString(PeriodFormat.getDefault());
        log.record(new AdapterLogEntry(Severity.INFO).withSource(getClass()).withDescription("C4 EPG Update finished in " + runTime));
    }

}
