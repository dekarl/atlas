package org.atlasapi.remotesite.itv.whatson;
import java.util.List;

import org.slf4j.Logger;
import org.atlasapi.persistence.system.RemoteSiteClient;
import org.joda.time.LocalDate;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.DayRange;
import com.metabroadcast.common.time.DayRangeGenerator;
import com.metabroadcast.common.time.SystemClock;

public class ItvWhatsOnUpdater extends ScheduledTask {
    private final String feedUrl;
    private final RemoteSiteClient<List<ItvWhatsOnEntry>> itvWhatsOnClient;
    private final ItvWhatsOnEntryProcessor processor;
    private final DayRange dayRange;
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private ItvWhatsOnUpdater(String feedUrl, 
            RemoteSiteClient<List<ItvWhatsOnEntry>> itvWhatsOnClient, 
            ItvWhatsOnEntryProcessor processor, 
            DayRange dayRange) {
        this.feedUrl = feedUrl;
        this.itvWhatsOnClient = itvWhatsOnClient;
        this.processor = processor;
        this.dayRange = dayRange;
    }
    
    public UpdateProgress ingestFeedEntries(List<ItvWhatsOnEntry> entries) {
        UpdateProgress progress = UpdateProgress.START;
        for (ItvWhatsOnEntry entry : entries) {
            try {
                processor.createOrUpdateAtlasEntityFrom(entry);
                progress = progress.reduce(UpdateProgress.SUCCESS);
            } catch (Exception e) {
               log.error("Error processing item '" + entry.getProgrammeTitle() +"'", e);
               progress = progress.reduce(UpdateProgress.FAILURE);
            }
        }
        return progress;
    }

    @Override
    protected void runTask() {

        UpdateProgress feedLevelProgress = UpdateProgress.START;
        UpdateProgress itemLevelProgress = UpdateProgress.START;
        
        for (LocalDate date : dayRange) {
            String feedUri = feedUrl + date.toString("YYYY/MM/dd");
            reportStatus(String.format("Ingesting %s. Feed %s. Item %s", feedUri, feedLevelProgress, itemLevelProgress));
            try {
                List<ItvWhatsOnEntry> entries = itvWhatsOnClient.get(feedUri);
                itemLevelProgress = itemLevelProgress.reduce(ingestFeedEntries(entries));
                feedLevelProgress = feedLevelProgress.reduce(UpdateProgress.SUCCESS);
            } catch (Exception e) {
                feedLevelProgress = feedLevelProgress.reduce(UpdateProgress.FAILURE);
                log.error("Exception fetching feed at " + feedUri, e);
            }
        }
        String message = String.format("Finished. Feed %s. Item %s", feedLevelProgress, itemLevelProgress);
        reportStatus(message);
        if (feedLevelProgress.hasFailures() || itemLevelProgress.hasFailures()) {
            throw new RuntimeException(message);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String feedUrl;
        private RemoteSiteClient<List<ItvWhatsOnEntry>> itvWhatsOnClient;
        private ItvWhatsOnEntryProcessor processor;
        private Integer lookAhead;
        private Integer lookBack;
        private LocalDate day;
        private Clock clock = new SystemClock();
        
        public Builder withFeedUrl(String feedUrl) {
            this.feedUrl = feedUrl;
            return this;
        }
        
        public Builder withWhatsOnClient(RemoteSiteClient<List<ItvWhatsOnEntry>> itvWhatsOnClient) {
            this.itvWhatsOnClient = itvWhatsOnClient;
            return this;
        }
        
        public Builder withProcessor(ItvWhatsOnEntryProcessor processor) {
            this.processor = processor;
            return this;
        }
        
        public Builder withLookAhead(int lookAhead) {
            this.lookAhead = lookAhead;
            return this;
        }
        
        public Builder withLookBack(int lookBack) {
            this.lookBack = lookBack;
            return this;
        }
        
        public Builder withDay(LocalDate day) {
            this.day = day;
            return this;
        }
        
        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }
        
        public ItvWhatsOnUpdater build() {
            Preconditions.checkNotNull(feedUrl);
            Preconditions.checkNotNull(itvWhatsOnClient);
            Preconditions.checkNotNull(processor);
            
            DayRange dayRange;
                        
            if (day == null) {
                Preconditions.checkNotNull(lookBack);
                Preconditions.checkNotNull(lookAhead);
                DayRangeGenerator dateRangeGenerator = new DayRangeGenerator()
                .withLookBack(lookBack)
                .withLookAhead(lookAhead);
                dayRange = dateRangeGenerator.generate(clock.now().toLocalDate());
            } else {
                Preconditions.checkArgument(lookBack == null && lookAhead == null, 
                        "Cannot specify day when lookBack or lookAhead values have been set");
                DayRangeGenerator dateRangeGenerator = new DayRangeGenerator()
                .withLookBack(0)
                .withLookAhead(0);
                dayRange = dateRangeGenerator.generate(day);
            }
            
            return new ItvWhatsOnUpdater(feedUrl, 
                    itvWhatsOnClient, 
                    processor, 
                    dayRange);
        }
    }
}
