package org.atlasapi.remotesite.youview;

import static org.atlasapi.remotesite.youview.DefaultYouViewChannelResolver.YOUVIEW_URI_MATCHER;

import java.util.List;
import java.util.regex.Matcher;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.remotesite.redux.UpdateProgress;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.metabroadcast.common.scheduling.ScheduledTask;


public class YouViewUpdater extends ScheduledTask {

    private static final String ATOM_PREFIX = "atom";
    private static final String ENTRY_KEY = "entry";
    private final YouViewScheduleFetcher fetcher;
    private final YouViewXmlElementHandler elementHandler;
    private final Duration plus;
    private final Duration minus;
    private final Logger log = LoggerFactory.getLogger(YouViewUpdater.class);
    private final YouViewChannelResolver channelResolver;
    
    public YouViewUpdater(YouViewChannelResolver channelResolver, YouViewScheduleFetcher fetcher, YouViewXmlElementHandler elementHandler, Duration minus, Duration plus) {
        this.channelResolver = channelResolver;
        this.fetcher = fetcher;
        this.elementHandler = elementHandler;
        this.minus = minus;
        this.plus = plus;
    }
    
    @Override
    protected void runTask() {
        try {
            DateTime midnightToday = new DateTime(DateMidnight.now());
            DateTime startTime = midnightToday.minus(minus);
            DateTime endTime = midnightToday.plus(plus);
            
            YouViewDataProcessor<UpdateProgress> processor = processor();
            
            List<Channel> youViewChannels = channelResolver.getAllChannels();
            
            while (startTime.isBefore(endTime)) {
                for (Channel channel : youViewChannels) {
                    Document xml = fetcher.getSchedule(startTime, startTime.plusDays(1), getYouViewId(channel));
                    Element root = xml.getRootElement();
                    Elements entries = root.getChildElements(ENTRY_KEY, root.getNamespaceURI(ATOM_PREFIX));

                    for (int i = 0; i < entries.size(); i++) {
                        processor.process(entries.get(i));
                    }
                }
                startTime = startTime.plusDays(1);
            }

            reportStatus(processor.getResult().toString());
        } catch (Exception e) {
            log.error("Exception when processing YouView schedule", e);
            Throwables.propagate(e);
        }

    }
    
    private int getYouViewId(Channel channel) {
        for (String alias : channel.getAliasUrls()) {
            Matcher m = YOUVIEW_URI_MATCHER.matcher(alias);
            if(m.matches()) {
                return Integer.decode(m.group(1));
            }
        }
        throw new RuntimeException("Channel " + channel.getCanonicalUri() + " does not have a YouView alias");
    }

    private YouViewDataProcessor<UpdateProgress> processor() {
        return new YouViewDataProcessor<UpdateProgress>() {
            
            UpdateProgress progress = UpdateProgress.START;
            
            @Override
            public boolean process(Element element) {
                try {
                    elementHandler.handle(element);
                    progress = progress.reduce(UpdateProgress.SUCCESS);
                } catch (Exception e) {
                    log.warn(element.getLocalName() , e);
                    progress = progress.reduce(UpdateProgress.FAILURE);
                }
                reportStatus(progress.toString());
                return shouldContinue();
            }

            @Override
            public UpdateProgress getResult() {
                return progress;
            }
        };
    }

}
