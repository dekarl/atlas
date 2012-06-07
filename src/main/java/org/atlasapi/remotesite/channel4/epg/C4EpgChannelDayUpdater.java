package org.atlasapi.remotesite.channel4.epg;

import static org.atlasapi.persistence.logging.AdapterLogEntry.errorEntry;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.ERROR;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.WARN;

import java.util.List;
import java.util.Map;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.ScheduleEntry.ItemRefAndBroadcast;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.atlasapi.persistence.system.RemoteSiteClient;
import org.atlasapi.remotesite.channel4.C4BrandUpdater;
import org.atlasapi.remotesite.channel4.epg.model.C4EpgEntry;
import org.atlasapi.remotesite.redux.UpdateProgress;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.time.DateTimeZones;

public class C4EpgChannelDayUpdater {

    private final static String epgUriPattern = "https://pmlsc.channel4.com/pmlsd/tv-listings/daily/%s/%s.atom";

    private final RemoteSiteClient<List<C4EpgEntry>> scheduleClient;
    private final ContentWriter writer;
    private final C4EpgEntryContentExtractor epgEntryContentExtractor;
    private final BroadcastTrimmer trimmer;
    private final AdapterLog log;
    
    public C4EpgChannelDayUpdater(RemoteSiteClient<List<C4EpgEntry>> scheduleClient, ContentWriter writer, ContentResolver resolver, C4BrandUpdater brandUpdater, BroadcastTrimmer trimmer, AdapterLog log) {
        this.scheduleClient = scheduleClient;
        this.writer = writer;
        this.epgEntryContentExtractor = new C4EpgEntryContentExtractor(resolver, brandUpdater);
        this.trimmer = trimmer;
        this.log = log;
    }
    
    public UpdateProgress update(String channelUriKey, Channel channel, LocalDate scheduleDay) {
        String uri = uriFor(channelUriKey, scheduleDay);
        log.record(new AdapterLogEntry(Severity.DEBUG).withDescription("Updating from " + uri).withSource(getClass()));
        

        try {
            List<C4EpgEntry> entries = getSchedule(uri);
            if(entries == null || entries.isEmpty()) {
                log.record(new AdapterLogEntry(ERROR).withSource(getClass()).withDescription("Empty or null schedule at:" + uri));
                return UpdateProgress.FAILURE;
            }
            
            List<ItemRefAndBroadcast> processedItems = process(entries, channel);
            trim(scheduleDay, channel, processedItems);
            return new UpdateProgress(processedItems.size(), 0);
        } catch (Exception e) {
            log.record(new AdapterLogEntry(ERROR).withCause(e).withSource(getClass()).withDescription("Exception updating from " + uri));
            return UpdateProgress.FAILURE;
        }
    }
    
    private String uriFor(String channelKey, LocalDate scheduleDay) {
        return String.format(epgUriPattern, scheduleDay.toString("yyyy/MM/dd"), channelKey);
    }
    
    private List<C4EpgEntry> getSchedule(String uri) {
        try {
            return scheduleClient.get(uri);
        } catch (Exception e) {
            log.record(new AdapterLogEntry(WARN).withCause(e).withSource(getClass()).withDescription("Exception fetching feed at " + uri));
            return null;
        }
    }

    private void trim(LocalDate scheduleDay, Channel channel, List<ItemRefAndBroadcast> processedItems) {
        DateTime scheduleStart = scheduleDay.toDateTime(new LocalTime(6,0,0), DateTimeZones.LONDON);
        Interval scheduleInterval = new Interval(scheduleStart, scheduleStart.plusDays(1));
        trimmer.trimBroadcasts(scheduleInterval, channel, broacastIdsFrom(processedItems));
    }

    private Map<String, String> broacastIdsFrom(List<ItemRefAndBroadcast> processedItems) {
        ImmutableMap.Builder<String, String> broadcastIdItemIdMap = ImmutableMap.builder();
        for (ItemRefAndBroadcast itemRefAndBroadcast : processedItems) {
            broadcastIdItemIdMap.put(itemRefAndBroadcast.getBroadcast().getSourceId(), itemRefAndBroadcast.getItemUri());
        }
        return broadcastIdItemIdMap.build();
    }

    private List<ItemRefAndBroadcast> process(Iterable<C4EpgEntry> entries, Channel channel) {
        ImmutableList.Builder<ItemRefAndBroadcast> episodes = ImmutableList.builder();
        for (C4EpgEntry entry : entries) {
            try {
                ContentHierarchyAndBroadcast extractedContent = epgEntryContentExtractor.extract(new C4EpgChannelEntry(entry, channel));
                if (extractedContent.getBrand().isPresent()) {
                    writer.createOrUpdate(extractedContent.getBrand().get());
                }
                if (extractedContent.getSeries().isPresent()) {
                    writer.createOrUpdate(extractedContent.getSeries().get());
                }
                writer.createOrUpdate(extractedContent.getItem());
                episodes.add(new ItemRefAndBroadcast(extractedContent.getItem(), extractedContent.getBroadcast()));
            } catch (Exception e) {
                log.record(errorEntry().withCause(e).withSource(getClass()).withDescription("Exception processing entry %s" + entry.id()));
            }
        }
        return episodes.build();
    }

}
