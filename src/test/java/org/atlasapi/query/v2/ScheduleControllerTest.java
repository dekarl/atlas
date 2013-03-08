package org.atlasapi.query.v2;

import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.application.ApplicationConfiguration;
import org.atlasapi.application.query.ApplicationConfigurationFetcher;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Schedule;
import org.atlasapi.media.entity.Schedule.ScheduleChannel;
import org.atlasapi.output.AtlasErrorSummary;
import org.atlasapi.output.AtlasModelWriter;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.NullAdapterLog;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.servlet.StubHttpServletRequest;
import com.metabroadcast.common.servlet.StubHttpServletResponse;
import com.metabroadcast.common.time.DateTimeZones;

@RunWith(MockitoJUnitRunner.class)
public class ScheduleControllerTest {

    private final ScheduleResolver scheduleResolver = mock(ScheduleResolver.class);
    private final ChannelResolver channelResolver = mock(ChannelResolver.class);
    private final ApplicationConfigurationFetcher configFetcher = mock(ApplicationConfigurationFetcher.class);
    private final AdapterLog log = new NullAdapterLog();
    @SuppressWarnings("unchecked")
    private final AtlasModelWriter<Iterable<ScheduleChannel>> outputter = mock(AtlasModelWriter.class);
    private final ScheduleController controller = new ScheduleController(scheduleResolver, channelResolver, configFetcher, log, outputter);
    
    @Test
    @SuppressWarnings("unchecked")
    public void testScheduleRequestFailsWithNoPublishersOrApiKey() throws IOException {
        String to = new DateTime(DateTimeZones.UTC).toString();
        String from = new DateTime(DateTimeZones.UTC).toString();
        HttpServletRequest request = new StubHttpServletRequest();
        HttpServletResponse response = new StubHttpServletResponse();
        
        when(configFetcher.configurationFor(request)).thenReturn(Maybe.<ApplicationConfiguration>nothing());
        
        controller.schedule(from, to, null, null, "cid", null, request, response);
        
        verify(outputter).writeError(argThat(is(request)), argThat(is(response)), any(AtlasErrorSummary.class));
        verify(outputter, never()).writeTo(argThat(is(request)), argThat(is(response)), any(Iterable.class), any(Set.class), any(ApplicationConfiguration.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testScheduleRequestPassWithJustPublishers() throws IOException {
        DateTime from = new DateTime(DateTimeZones.UTC);
        DateTime to = new DateTime(DateTimeZones.UTC);
        HttpServletRequest request = new StubHttpServletRequest();
        HttpServletResponse response = new StubHttpServletResponse();
        Channel channel = new Channel.Builder().build();
        
        when(configFetcher.configurationFor(request)).thenReturn(Maybe.<ApplicationConfiguration>nothing());
        when(channelResolver.fromId(any(Long.class))).thenReturn(Maybe.just(channel));
        when(scheduleResolver.schedule(from, to, ImmutableSet.of(channel), ImmutableSet.of(Publisher.BBC), null))
            .thenReturn(Schedule.fromChannelMap(ImmutableMap.<Channel,List<Item>>of(), new Interval(from, to)));
        
        controller.schedule(from.toString(), to.toString(), null, null, "cbbh", "bbc.co.uk", request, response);
        
        verify(outputter).writeTo(argThat(is(request)), argThat(is(response)), any(Iterable.class), any(Set.class), any(ApplicationConfiguration.class));
    }

}
