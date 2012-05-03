package org.atlasapi.remotesite.channel4.epg;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.remotesite.channel4.epg.model.C4EpgEntry;
import org.atlasapi.remotesite.channel4.epg.model.C4EpgMedia;
import org.atlasapi.remotesite.channel4.epg.model.TypedLink;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.time.DateTimeZones;

public class C4EpgEntryBroadcastExtractorTest {

    private final C4EpgEntryBroadcastExtractor extractor = new C4EpgEntryBroadcastExtractor();

    private final Channel channel = new Channel(Publisher.C4, "Channel 4", "key", MediaType.VIDEO, "http://www.channel4.com");
    
    @Test
    public void testExtractsBroadcastsFromLinkedEntry() {
        
        C4EpgEntry entry = linkedEntry();
        C4EpgChannelEntry source = new C4EpgChannelEntry(entry, channel);
        
        Broadcast broadcast = extractor.extract(source);
        
        assertThat(broadcast.getSourceId(), is("c4:26424439"));
        assertThat(broadcast.getAliases(), hasItem("tag:www.channel4.com,2009:slot/C426424439"));
        assertThat(broadcast.getBroadcastOn(), is(channel.getCanonicalUri()));
        assertThat(broadcast.getTransmissionTime(), is(entry.txDate()));
        assertThat((long)broadcast.getBroadcastDuration(), is(entry.duration().getStandardSeconds()));
        assertThat(broadcast.isActivelyPublished(), is(true));
        assertTrue(broadcast.getSubtitled());
        assertFalse(broadcast.getAudioDescribed());
        assertNull(broadcast.getWidescreen());
        assertNull(broadcast.getSigned());
        assertNull(broadcast.getRepeat());
        
    }
    
    @Test
    public void testExtractsBroadcastsFromUnlinkedEntry() {
        
        C4EpgEntry entry = unlinkedEntry();
        C4EpgChannelEntry source = new C4EpgChannelEntry(entry, channel);
        
        Broadcast broadcast = extractor.extract(source);
        
        assertThat(broadcast.getSourceId(), is("c4:26424438"));
        assertThat(broadcast.getAliases(), hasItem("tag:www.channel4.com,2009:slot/C426424438"));
        assertThat(broadcast.getBroadcastOn(), is(channel.getCanonicalUri()));
        assertThat(broadcast.getTransmissionTime(), is(entry.txDate()));
        assertThat((long)broadcast.getBroadcastDuration(), is(entry.duration().getStandardSeconds()));
        assertThat(broadcast.isActivelyPublished(), is(true));
        assertTrue(broadcast.getSubtitled());
        assertFalse(broadcast.getAudioDescribed());
        assertFalse(broadcast.getWidescreen());
        assertFalse(broadcast.getSigned());
        assertTrue(broadcast.getRepeat());
        
    }
    
    private C4EpgEntry unlinkedEntry() {
        return new C4EpgEntry("tag:pmlsc.channel4.com,2009:slot/26424438")
        .withTitle("The Treacle People")
        .withSummary("One Flu Over the Boggart's Nest")
        .withUpdated(new DateTime("2012-05-08T14:27:26.474Z", DateTimeZones.UTC))
        .withTxDate(new DateTime("2012-04-26T05:05:00.000Z", DateTimeZones.UTC))
        .withTxChannel("C4")
        .withSubtitles(true)
        .withAudioDescription(false)
        .withDuration(Duration.standardSeconds(1455))
        .withWideScreen(false)
        .withSigning(false)
        .withRepeat(true)
        .withProgrammeId("40635/014")
        .withSimulcastRights(true);
    }
    
    private C4EpgEntry linkedEntry() {
        return new C4EpgEntry("tag:pmlsc.channel4.com,2009:slot/26424439")
            .withTitle("Hello")
            .withSummary("Groove thinks there can't be a better way")
            .withUpdated(new DateTime("2010-11-03T05:57:50.175Z", DateTimeZones.UTC))
            .withTxDate(new DateTime("2012-04-26T05:15:00.000Z", DateTimeZones.UTC))
            .withTxChannel("C4")
            .withSubtitles(true)
            .withAudioDescription(false)
            .withDuration(Duration.standardSeconds(1455))
            .withWideScreen(null)
            .withSigning(null)
            .withRepeat(null)
            .withProgrammeId("30630/003")
            .withSimulcastRights(true)
            .withLinks(ImmutableList.of(
                new TypedLink("http://www.channel4.com/programmes/the-hoobs/4od#2924127", "alternate"),
                new TypedLink("http://pmlsc.channel4.com/pmlsd/the-hoobs/episode-guide/series-1/episode-3.atom", "related")
            ))
            .withMedia(
                new C4EpgMedia()
                    .withPlayer("http://www.channel4.com/programmes/the-hoobs/4od#2924127")
                    .withThumbnail("http://cache.channel4.com/assets/programmes/images/the-hoobs/series-1/the-hoobs-s1-20090623112301_200x113.jpg")
                    .withRating("nonadult")
                    .withRestriction(ImmutableSet.of(Countries.GB, Countries.IE))
            );
        
    }
    
}
