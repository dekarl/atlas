package org.atlasapi.remotesite.channel4.epg;

import static org.atlasapi.media.entity.Channel.CHANNEL_FOUR;

import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Channel;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Schedule;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.NullAdapterLog;
import org.atlasapi.persistence.testing.StubContentResolver;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ScheduleResolverBroadcastTrimmerTest extends TestCase {
    private final Mockery context = new Mockery();
    
    private final ScheduleResolver scheduleResolver = context.mock(ScheduleResolver.class);
    private final ContentWriter contentWriter = context.mock(ContentWriter.class);
    private final Channel channel = Channel.CHANNEL_FOUR;
    private final Set<Channel> channels = ImmutableSet.of(channel);
    private final Set<Publisher> publishers = ImmutableSet.of(Publisher.C4);

    private Item item = buildItem();
    
    public void testTrimBroadcasts() {
        final Schedule schedule = Schedule.fromChannelMap(channelMap(), new Interval(100, 200));
        
        ContentResolver resolver = new StubContentResolver().respondTo(item);
        
        context.checking(new Expectations(){{
            oneOf(scheduleResolver).schedule(with(any(DateTime.class)), with(any(DateTime.class)), with(channels), with(publishers)); will(returnValue(schedule));
            one(contentWriter).createOrUpdate(with(trimmedItem()));
        }});
        
        AdapterLog log = new NullAdapterLog();
        
        ScheduleResolverBroadcastTrimmer trimmer = new ScheduleResolverBroadcastTrimmer(Publisher.C4, scheduleResolver, resolver, contentWriter, log);
        
        Interval scheduleInterval = new Interval(100, 200);
        trimmer.trimBroadcasts(scheduleInterval, CHANNEL_FOUR, ImmutableMap.of("c4:1234", item.getCanonicalUri()));
        
    }
    
    public void testTrimsBroadcastsOfWrongItems() {
        Item item1 = new Item("testUri1", "testCurie", Publisher.C4);
        Version version = new Version();
        Broadcast remove = new Broadcast(Channel.CHANNEL_FOUR.uri(), new DateTime(50), new DateTime(103)).withId("c4:2234");
        remove.setIsActivelyPublished(true);
        version.setBroadcasts(ImmutableSet.of(remove));
        item1.addVersion(version);
        
        Item item2 = new Item("testUri2", "testCurie", Publisher.C4);
        version = new Version();
        Broadcast keep = new Broadcast(Channel.CHANNEL_FOUR.uri(), new DateTime(150), new DateTime(153)).withId("c4:1234");
        remove.setIsActivelyPublished(true);
        version.setBroadcasts(ImmutableSet.of(keep));
        item2.addVersion(version);

        Map<Channel, List<Item>> channelMap = Maps.newHashMap();
        channelMap.put(Channel.CHANNEL_FOUR, Lists.newArrayList(item1, item2));
        final Schedule schedule = Schedule.fromChannelMap(channelMap, new Interval(50, 200));

        ContentResolver resolver = new StubContentResolver().respondTo(item1).respondTo(item2);

        context.checking(new Expectations() {{
            oneOf(scheduleResolver).schedule(with(any(DateTime.class)), with(any(DateTime.class)), with(channels), with(publishers));
            will(returnValue(schedule));
            one(contentWriter).createOrUpdate(with(trimmedItem()));
        }});
        

        AdapterLog log = new NullAdapterLog();
        
        ScheduleResolverBroadcastTrimmer trimmer = new ScheduleResolverBroadcastTrimmer(Publisher.C4, scheduleResolver, resolver, contentWriter, log);
        
        Interval scheduleInterval = new Interval(50, 200);
        trimmer.trimBroadcasts(scheduleInterval, CHANNEL_FOUR, ImmutableMap.of("c4:1234", item2.getCanonicalUri()));
        
        assertTrue(Iterables.getOnlyElement(Iterables.getOnlyElement(item2.getVersions()).getBroadcasts()).isActivelyPublished());
        
    }

    private Matcher<Item> trimmedItem() {
        return new TypeSafeMatcher<Item>() {
            @Override
            public void describeTo(Description desc) {
                desc.appendText("trimmed item with broadcast with id c4:1234");
            }

            @Override
            public boolean matchesSafely(Item item) {
                Set<Broadcast> broadcasts = Iterables.getOnlyElement(item.getVersions()).getBroadcasts();
                for (Broadcast broadcast : broadcasts) {
                    if(!check(broadcast)) {
                        return false;
                    }
                }
                return true;
            }

            private boolean check(Broadcast broadcast) {
                return broadcast.getId().equals("c4:0234") && broadcast.isActivelyPublished()
                    || broadcast.getId().equals("c4:1234") && broadcast.isActivelyPublished()
                    || broadcast.getId().equals("c4:2234") && !broadcast.isActivelyPublished();
            }
        };
    }

	private static Item buildItem() {
		Item item = new Item("testUri", "testCurie", Publisher.C4);
        Version version = new Version();
        
        Broadcast ignore = new Broadcast(Channel.CHANNEL_FOUR.uri(), new DateTime(50), new DateTime(103)).withId("c4:0234");
        ignore.setIsActivelyPublished(true);
        Broadcast retain = new Broadcast(Channel.CHANNEL_FOUR.uri(), new DateTime(105), new DateTime(120)).withId("c4:1234");
        retain.setIsActivelyPublished(true);
        Broadcast remove = new Broadcast(Channel.CHANNEL_FOUR.uri(), new DateTime(150), new DateTime(165)).withId("c4:2234");
        remove.setIsActivelyPublished(true);
        
        version.setBroadcasts(ImmutableSet.of(ignore, retain, remove));
        item.addVersion(version);
		return item;
	}
    
    private Map<Channel, List<Item>> channelMap() {
        Map<Channel, List<Item>> channelMap = Maps.newHashMap();
        channelMap.put(Channel.CHANNEL_FOUR, Lists.newArrayList(item));
        return channelMap;
    }
}