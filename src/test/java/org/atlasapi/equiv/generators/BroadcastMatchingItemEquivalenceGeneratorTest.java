package org.atlasapi.equiv.generators;

import static org.atlasapi.media.entity.Publisher.BBC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.joda.time.Duration.standardMinutes;

import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.atlasapi.equiv.results.description.DefaultDescription;
import org.atlasapi.equiv.results.scores.Score;
import org.atlasapi.equiv.results.scores.ScoredCandidates;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Schedule;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.time.DateTimeZones;

@RunWith(JMock.class)
public class BroadcastMatchingItemEquivalenceGeneratorTest extends TestCase {

    private static final Channel BBC_ONE = new Channel(Publisher.METABROADCAST, "BBC One", "bbcone", false, MediaType.AUDIO, "http://www.bbc.co.uk/bbcone");
    private static final Channel BBC_ONE_CAMBRIDGE = new Channel(Publisher.METABROADCAST, "BBC One Cambridgeshire", "bbcone-cambridge", false, MediaType.AUDIO, "http://www.bbc.co.uk/services/bbcone/cambridge");

    private final Mockery context = new Mockery();
    private final ScheduleResolver resolver = context.mock(ScheduleResolver.class);
    private BroadcastMatchingItemEquivalenceGenerator generator;
    
    @Before
    public void setUp() {
    	final ChannelResolver channelResolver = context.mock(ChannelResolver.class);
    	context.checking(new Expectations() {
			{
				allowing(channelResolver).fromUri(BBC_ONE.uri());
				will(returnValue(Maybe.just(BBC_ONE)));
				allowing(channelResolver).fromUri(BBC_ONE_CAMBRIDGE.uri());
				will(returnValue(Maybe.just(BBC_ONE_CAMBRIDGE)));
			}
		});
    	generator = new BroadcastMatchingItemEquivalenceGenerator(resolver, channelResolver, ImmutableSet.of(BBC), standardMinutes(1));
    }

    @Test
    public void testGenerateEquivalencesForOneMatchingBroadcast() {
        final Item item1 = episodeWithBroadcasts("subjectItem", Publisher.PA, 
                new Broadcast(BBC_ONE.uri(), utcTime(100000), utcTime(200000)),
                new Broadcast(BBC_ONE_CAMBRIDGE.uri(), utcTime(100000), utcTime(200000)));//ignored
        
        final Item item2 = episodeWithBroadcasts("equivItem", Publisher.BBC, new Broadcast(BBC_ONE.uri(), utcTime(100000), utcTime(200000)));
        
        context.checking(new Expectations(){{
            one(resolver).schedule(utcTime(40000), utcTime(260000), ImmutableSet.of(BBC_ONE), ImmutableSet.of(BBC), null);
                will(returnValue(Schedule.fromChannelMap(ImmutableMap.of(BBC_ONE, (List<Item>)ImmutableList.<Item>of(item2)), interval(40000, 260000))));
        }});
        
        ScoredCandidates<Item> equivalents = generator.generate(item1, new DefaultDescription());
        
        Map<Item, Score> scoreMap = equivalents.candidates();
        
        assertThat(scoreMap.size(), is(1));
        assertThat(scoreMap.get(item2).asDouble(), is(equalTo(1.0)));
    }

    private Interval interval(long startMillis, long endMillis) {
        return new Interval(startMillis, endMillis, DateTimeZones.UTC);
    }
    
    private DateTime utcTime(long millis) {
        return new DateTime(millis, DateTimeZones.UTC);
    }
    
    private Episode episodeWithBroadcasts(String episodeId, Publisher publisher, Broadcast... broadcasts) {
        Episode item = new Episode(episodeId+"Uri", episodeId+"Curie", publisher);
        Version version = new Version();
        version.setCanonicalUri(episodeId+"Version");
        version.setProvider(publisher);
        for (Broadcast broadcast : broadcasts) {
            version.addBroadcast(broadcast);
        }
        item.addVersion(version);
        return item;
    }
    
}
