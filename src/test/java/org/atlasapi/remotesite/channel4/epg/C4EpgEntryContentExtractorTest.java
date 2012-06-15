package org.atlasapi.remotesite.channel4.epg;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.atlasapi.remotesite.channel4.C4BrandUpdater;
import org.atlasapi.remotesite.channel4.epg.model.C4EpgEntry;
import org.atlasapi.remotesite.channel4.epg.model.C4EpgMedia;
import org.atlasapi.remotesite.channel4.epg.model.TypedLink;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.TimeMachine;

@RunWith(JMock.class)
public class C4EpgEntryContentExtractorTest {

    private final Mockery context = new Mockery();
    private final ContentResolver resolver = context.mock(ContentResolver.class);
    private final C4BrandUpdater brandUpdater = context.mock(C4BrandUpdater.class);
    
    private final DateTime now = new DateTime(DateTimeZones.UTC);
    private final Clock clock = new TimeMachine(now );
    
    private final C4EpgEntryContentExtractor extractor = new C4EpgEntryContentExtractor(resolver, brandUpdater, clock );
    
    private final Channel channel = new Channel(Publisher.C4, "Channel 4", "key", MediaType.VIDEO, "http://www.channel4.com");
    
    @Test
    public void testCreatesBrandSeriesItemAndBroadcastForRelatedLinkEntryWhenNothingResolved() {
        C4EpgEntry entry = linkedEntry();
        C4EpgChannelEntry source = new C4EpgChannelEntry(entry, channel);
        
        final String idUri = "http://www.channel4.com/programmes/30630/003";
        final String hierarchyUri = "http://www.channel4.com/programmes/the-hoobs/episode-guide/series-1/episode-3";
        final String synthUri = "http://www.channel4.com/programmes/synthesized/the-hoobs/26424439";
        final String seriesUri = "http://www.channel4.com/programmes/the-hoobs/episode-guide/series-1";
        final String brandUri = "http://www.channel4.com/programmes/the-hoobs";
        
        context.checking(new Expectations(){{
            one(resolver).findByCanonicalUris(with(hasItems(idUri, hierarchyUri, synthUri)));
            will(returnValue(ResolvedContent.builder().build()));
            
            one(resolver).findByCanonicalUris(with(hasItems(seriesUri)));
            will(returnValue(ResolvedContent.builder().build()));
            
            one(resolver).findByCanonicalUris(with(hasItems(brandUri)));
            will(returnValue(ResolvedContent.builder().build()));
            
            one(brandUpdater).createOrUpdateBrand(brandUri);
            will(returnValue(null));
        }});
        
        ContentHierarchyAndBroadcast extracted = extractor.extract(source);
        
        Optional<Brand> brand = extracted.getBrand();
        assertThat(brand.isPresent(), is(true));
        assertThat(brand.get().getCanonicalUri(), is(brandUri));
        
        Optional<Series> series = extracted.getSeries();
        assertThat(series.isPresent(), is(true));
        assertThat(series.get().getCanonicalUri(), is(seriesUri));
        assertThat(series.get().getParent().getUri(), is(brandUri));
        
        Episode item = (Episode) extracted.getItem();
        assertThat(item.getCanonicalUri(), is(idUri));
        assertThat(item.getAliases(), hasItem(hierarchyUri));
        assertThat(item.getContainer().getUri(), is(brandUri));
        assertThat(item.getSeriesRef().getUri(), is(seriesUri));
        
        assertThat(getOnlyElement(getOnlyElement(item.getVersions()).getBroadcasts()).getSourceId(), is("c4:26424439"));
    }
    
    @Test
    public void testCreatesItemAndBroadcastForNoRelatedLinkEntryWhenNothingResolved() {
        C4EpgEntry entry = unlinkedEntry();
        C4EpgChannelEntry source = new C4EpgChannelEntry(entry, channel);
        
        final String idUri = "http://www.channel4.com/programmes/40635/014";
        final String synthUri = "http://www.channel4.com/programmes/synthesized/the-treacle-people/26424438";
        
        context.checking(new Expectations(){{
            one(resolver).findByCanonicalUris(with(hasItems(idUri, synthUri)));
            will(returnValue(ResolvedContent.builder().build()));
            
            never(brandUpdater).createOrUpdateBrand(with(any(String.class)));
        }});

        ContentHierarchyAndBroadcast extracted = extractor.extract(source);
        
        assertThat(extracted.getBrand(), is(Optional.<Brand>absent()));
        assertThat(extracted.getSeries(), is(Optional.<Series>absent()));
        
        assertThat(extracted.getItem().getCanonicalUri(), is(idUri));
        assertThat(extracted.getItem().getAliases().isEmpty(), is(true));
        assertThat(extracted.getItem().getContainer(), nullValue());
        assertThat(extracted.getItem(), is(not(instanceOf(Episode.class))));
    }
    
    @Test
    public void testAddsIdAliasForNoRelatedLinkEntryWhenSynthesizedItemResolves() {
        C4EpgEntry entry = unlinkedEntry();
        C4EpgChannelEntry source = new C4EpgChannelEntry(entry, channel);
        
        final String idUri = "http://www.channel4.com/programmes/40635/014";
        final String synthUri = "http://www.channel4.com/programmes/synthesized/the-treacle-people/26424438";
        
        context.checking(new Expectations(){{
            one(resolver).findByCanonicalUris(with(hasItems(idUri, synthUri)));
            will(returnValue(ResolvedContent.builder().put(synthUri, testItem(synthUri)).build()));
            
            never(brandUpdater).createOrUpdateBrand(with(any(String.class)));
        }});

        ContentHierarchyAndBroadcast extracted = extractor.extract(source);
        
        assertThat(extracted.getItem().getCanonicalUri(), is(synthUri));
        assertThat(extracted.getItem().getAliases(), is(hasItem(idUri)));
    }
    
    @Test
    public void testDoesntAddEncodingWhereNoOnDemand() {
        C4EpgEntry entry = entryWithThumbnailNoOnDemand();
        C4EpgChannelEntry source = new C4EpgChannelEntry(entry, channel);
        
        final String idUri = "http://www.channel4.com/programmes/30630/003";
        final String hierarchyUri = "http://www.channel4.com/programmes/the-hoobs/episode-guide/series-1/episode-3";
        final String synthUri = "http://www.channel4.com/programmes/synthesized/the-hoobs/26424439";
        final String seriesUri = "http://www.channel4.com/programmes/the-hoobs/episode-guide/series-1";
        final String brandUri = "http://www.channel4.com/programmes/the-hoobs";
        
        context.checking(new Expectations(){{
            one(resolver).findByCanonicalUris(with(hasItems(idUri, hierarchyUri, synthUri)));
            will(returnValue(ResolvedContent.builder().build()));
            
            one(resolver).findByCanonicalUris(with(hasItems(seriesUri)));
            will(returnValue(ResolvedContent.builder().build()));
            
            one(resolver).findByCanonicalUris(with(hasItems(brandUri)));
            will(returnValue(ResolvedContent.builder().build()));
            
            one(brandUpdater).createOrUpdateBrand(brandUri);
            will(returnValue(null));
        }});

        ContentHierarchyAndBroadcast extracted = extractor.extract(source);
        
        assertThat(Iterables.getOnlyElement(extracted.getItem().getVersions()).getManifestedAs().size(), is(0));
    }
    
    private Item testItem(String uri) {
        Item item = new Item();
        item.setCanonicalUri(uri);
        item.setPublisher(Publisher.C4);
        Version version = new Version();
        version.setProvider(Publisher.C4);
        item.addVersion(version);
        return item;
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
    
    private C4EpgEntry entryWithThumbnailNoOnDemand() {
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
                .withThumbnail("http://cache.channel4.com/assets/programmes/images/the-hoobs/series-1/the-hoobs-s1-20090623112301_200x113.jpg")
        );
    }

}
