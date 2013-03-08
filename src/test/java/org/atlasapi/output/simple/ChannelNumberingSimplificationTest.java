package org.atlasapi.output.simple;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;

import junit.framework.TestCase;

import org.atlasapi.application.ApplicationConfiguration;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelGroup;
import org.atlasapi.media.channel.ChannelGroupStore;
import org.atlasapi.media.channel.ChannelNumbering;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.channel.Platform;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.output.Annotation;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.metabroadcast.common.ids.NumberToShortStringCodec;

public class ChannelNumberingSimplificationTest extends TestCase {
    
    private static final ChannelGroupStore channelGroupStore = new DummyChannelGroupStore();
    private static final ChannelSimplifier channelSimplifier = new ChannelSimplifier(Mockito.mock(NumberToShortStringCodec.class), Mockito.mock(ChannelResolver.class), new PublisherSimplifier());
    private static final ChannelGroupSimplifier channelGroupSimplifier = new ChannelGroupSimplifier(Mockito.mock(NumberToShortStringCodec.class), channelGroupStore, new PublisherSimplifier());
    private static final ChannelNumberingChannelGroupModelSimplifier nestedChannelGroupSimplifier = new ChannelNumberingChannelGroupModelSimplifier(channelGroupSimplifier);
    private static final ChannelNumberingsChannelToChannelGroupModelSimplifier numberingSimplifier = new ChannelNumberingsChannelToChannelGroupModelSimplifier(channelGroupStore, nestedChannelGroupSimplifier);
    
    private static final ChannelModelSimplifier simplifier = new ChannelModelSimplifier(channelSimplifier, numberingSimplifier);
    
    private final Channel channel = Channel.builder()
        .withMediaType(MediaType.VIDEO)
        .withSource(Publisher.BBC)
        .build();
    
    private static final long channelId = 1234L;
    private static final long channelGroupId = 5678L;
    private static final ApplicationConfiguration appConfig = Mockito.mock(ApplicationConfiguration.class);
    
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        channel.setId(channelId);
        
        Platform platform = new Platform();
        platform.setId(channelGroupId);
        
        channelGroupStore.createOrUpdate(platform);
    }
    
    @Test
    public void testChannelNumberingAfterEndDate() {
        ChannelGroup channelGroup = channelGroupStore.channelGroupFor(channelGroupId).get();
        
        ChannelNumbering numbering = ChannelNumbering.builder()
            .withChannel(channelId)
            .withChannelGroup(channelGroupId)
            .withChannelNumber("2")
            .withStartDate(new LocalDate().minusYears(10))
            .withEndDate(new LocalDate().minusYears(9))
            .build();
        
        channel.addChannelNumber(numbering);
        channelGroup.addChannelNumbering(numbering);
        
        channelGroup = channelGroupStore.createOrUpdate(channelGroup);
        org.atlasapi.media.entity.simple.Channel simpleChannel = simplifier.simplify(channel, ImmutableSet.of(Annotation.CHANNEL_GROUPS, Annotation.HISTORY), appConfig);
        assertThat(simpleChannel.getChannelGroups().size(), is(1));
        
        simpleChannel = simplifier.simplify(channel, ImmutableSet.of(Annotation.CHANNEL_GROUPS), appConfig);
        assertTrue(simpleChannel.getChannelGroups().isEmpty());
    }

    @Test 
    public void testMultipleCurrentNumberings() {
        ChannelGroup channelGroup = channelGroupStore.channelGroupFor(channelGroupId).get();
        
        ChannelNumbering one = ChannelNumbering.builder()
            .withChannel(channelId)
            .withChannelGroup(channelGroupId)
            .withChannelNumber("1")
            .withStartDate(new LocalDate(2000, 1, 1))
            .build();
        
        ChannelNumbering two = ChannelNumbering.builder()
            .withChannel(channelId)
            .withChannelGroup(channelGroupId)
            .withChannelNumber("2")
            .withStartDate(new LocalDate(2000, 1, 1))
            .build();
        
        channel.addChannelNumber(one);
        channel.addChannelNumber(two);
        channelGroup.addChannelNumbering(one);
        channelGroup.addChannelNumbering(two);
        
        channelGroup = channelGroupStore.createOrUpdate(channelGroup);
        org.atlasapi.media.entity.simple.Channel simpleChannel = simplifier.simplify(channel, ImmutableSet.of(Annotation.CHANNEL_GROUPS), appConfig);
        assertThat(simpleChannel.getChannelGroups().size(), is(2));
        
        assertEquals("1", simpleChannel.getChannelGroups().get(0).getChannelNumber());
        assertEquals("2", simpleChannel.getChannelGroups().get(1).getChannelNumber());
    }
    
    @Test 
    public void testDefaultsToCurrentFuture() {
        
        ChannelGroup channelGroup = channelGroupStore.channelGroupFor(channelGroupId).get();
        
        ChannelNumbering past = ChannelNumbering.builder()
            .withChannel(channelId)
            .withChannelGroup(channelGroupId)
            .withChannelNumber("1")
            .withStartDate(new LocalDate().minusYears(10))
            .withEndDate(new LocalDate().minusYears(9))
            .build();
        
        ChannelNumbering current = ChannelNumbering.builder()
            .withChannel(channelId)
            .withChannelGroup(channelGroupId)
            .withChannelNumber("2")
            .withStartDate(new LocalDate().minusYears(5))
            .withEndDate(new LocalDate().plusYears(2))
            .build();
        
        ChannelNumbering future = ChannelNumbering.builder()
            .withChannel(channelId)
            .withChannelGroup(channelGroupId)
            .withChannelNumber("3")
            .withStartDate(new LocalDate().plusYears(1))
            .build();
        
        channel.addChannelNumber(past);
        channel.addChannelNumber(current);
        channel.addChannelNumber(future);
        channelGroup.addChannelNumbering(past);
        channelGroup.addChannelNumbering(current);
        channelGroup.addChannelNumbering(future);
        
        channelGroup = channelGroupStore.createOrUpdate(channelGroup);

        org.atlasapi.media.entity.simple.Channel simpleChannel = simplifier.simplify(channel, ImmutableSet.of(Annotation.CHANNEL_GROUPS), appConfig);
        assertThat(simpleChannel.getChannelGroups().size(), is(2));
        
        org.atlasapi.media.entity.simple.ChannelNumbering first = simpleChannel.getChannelGroups().get(0);
        org.atlasapi.media.entity.simple.ChannelNumbering second = simpleChannel.getChannelGroups().get(1);
        
        assertThat(first.getChannelNumber(), org.hamcrest.Matchers.isOneOf("2", "3"));
        assertThat(second.getChannelNumber(), org.hamcrest.Matchers.isOneOf("2", "3"));
    }
    
    @Test
    public void testHistoryShowsAll() {
        ChannelGroup channelGroup = channelGroupStore.channelGroupFor(channelGroupId).get();
        
        ChannelNumbering past = ChannelNumbering.builder()
            .withChannel(channelId)
            .withChannelGroup(channelGroupId)
            .withChannelNumber("1")
            .withStartDate(new LocalDate().minusYears(10))
            .withEndDate(new LocalDate().minusYears(9))
            .build();
        
        ChannelNumbering current = ChannelNumbering.builder()
            .withChannel(channelId)
            .withChannelGroup(channelGroupId)
            .withChannelNumber("2")
            .withStartDate(new LocalDate().minusYears(5))
            .build();
        
        ChannelNumbering future = ChannelNumbering.builder()
            .withChannel(channelId)
            .withChannelGroup(channelGroupId)
            .withChannelNumber("3")
            .withStartDate(new LocalDate().plusYears(1))
            .build();
        
        channel.addChannelNumber(past);
        channel.addChannelNumber(current);
        channel.addChannelNumber(future);
        channelGroup.addChannelNumbering(past);
        channelGroup.addChannelNumbering(current);
        channelGroup.addChannelNumbering(future);
        
        channelGroup = channelGroupStore.createOrUpdate(channelGroup);
        
         org.atlasapi.media.entity.simple.Channel simpleChannel = simplifier.simplify(channel, ImmutableSet.of(Annotation.CHANNEL_GROUPS, Annotation.HISTORY), appConfig);
        assertThat(simpleChannel.getChannelGroups().size(), is(1));
        
        org.atlasapi.media.entity.simple.ChannelNumbering first = Iterables.getOnlyElement(simpleChannel.getChannelGroups());
        assertThat(first.getHistory().size(), is(3));
    }
    
    @Test
    public void testHistoryWithMultipleCurrentChannels() {
        ChannelGroup channelGroup = channelGroupStore.channelGroupFor(channelGroupId).get();
        
        ChannelNumbering past = ChannelNumbering.builder()
            .withChannel(channelId)
            .withChannelGroup(channelGroupId)
            .withChannelNumber("1")
            .withStartDate(new LocalDate().minusYears(10))
            .withEndDate(new LocalDate().minusYears(9))
            .build();
        
        ChannelNumbering current = ChannelNumbering.builder()
            .withChannel(channelId)
            .withChannelGroup(channelGroupId)
            .withChannelNumber("2")
            .withStartDate(new LocalDate().minusYears(5))
            .build();
        
        ChannelNumbering current2 = ChannelNumbering.builder()
            .withChannel(channelId)
            .withChannelGroup(channelGroupId)
            .withChannelNumber("3")
            .withStartDate(new LocalDate().minusYears(5))
            .build();
        
        ChannelNumbering future = ChannelNumbering.builder()
            .withChannel(channelId)
            .withChannelGroup(channelGroupId)
            .withChannelNumber("4")
            .withStartDate(new LocalDate().plusYears(1))
            .build();
        
        channel.addChannelNumber(past);
        channel.addChannelNumber(current);
        channel.addChannelNumber(current2);
        channel.addChannelNumber(future);
        channelGroup.addChannelNumbering(past);
        channelGroup.addChannelNumbering(current);
        channelGroup.addChannelNumbering(current2);
        channelGroup.addChannelNumbering(future);
        
        channelGroup = channelGroupStore.createOrUpdate(channelGroup);
        
        org.atlasapi.media.entity.simple.Channel simpleChannel = simplifier.simplify(channel, ImmutableSet.of(Annotation.CHANNEL_GROUPS, Annotation.HISTORY), appConfig);
        assertThat(simpleChannel.getChannelGroups().size(), is(2));
        
        org.atlasapi.media.entity.simple.ChannelNumbering first = Iterables.get(simpleChannel.getChannelGroups(), 0);
        org.atlasapi.media.entity.simple.ChannelNumbering second = Iterables.get(simpleChannel.getChannelGroups(), 1);
        assertThat(first.getHistory().size(), is(4));
        assertThat(second.getHistory().size(), is(4));
     }
    
    private static class DummyChannelGroupStore implements ChannelGroupStore {

        private Map<Long, ChannelGroup> channelGroups = Maps.newHashMap();
        
        @Override
        public Optional<ChannelGroup> fromAlias(String alias) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ChannelGroup> channelGroupFor(Long id) {
            return Optional.fromNullable(channelGroups.get(id));
        }

        @Override
        public Iterable<ChannelGroup> channelGroupsFor(Iterable<Long> ids) {
            return Iterables.transform(channelGroups.keySet(), new Function<Long, ChannelGroup>() {
                @Override
                public ChannelGroup apply(Long input) {
                    return channelGroups.get(input);
                }
            });
        }

        @Override
        public Iterable<ChannelGroup> channelGroups() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterable<ChannelGroup> channelGroupsFor(Channel channel) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelGroup createOrUpdate(ChannelGroup channelGroup) {
            channelGroups.put(channelGroup.getId(), channelGroup);
            return channelGroup;
        }
        
    }
}
