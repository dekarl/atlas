package org.atlasapi.remotesite.channel4.pmlsd;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.atlasapi.media.entity.Clip;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.remotesite.channel4.pmlsd.C4BrandClipExtractor;
import org.atlasapi.remotesite.channel4.pmlsd.C4PmlsdModule;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.io.Resources;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.TimeMachine;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;

@RunWith( MockitoJUnitRunner.class )
public class C4BrandClipExtractorTest {

    private final AtomFeedBuilder feed = new AtomFeedBuilder(Resources.getResource(getClass(), "green-wing-video.atom"));
    
    private final ContentFactory<Feed, Feed, Entry> contentFactory
        = new SourceSpecificContentFactory<>(Publisher.C4_PMLSD, new C4AtomFeedUriExtractor());;
    
    @Test
    public void testExtract() {
        
        Clock clock = new TimeMachine(new DateTime(DateTimeZones.UTC));
        C4BrandClipExtractor clipExtractor = new C4BrandClipExtractor(contentFactory, Publisher.C4_PMLSD, clock);
        
        Clip clip = clipExtractor.extract((Entry)feed.build().getEntries().get(0));
        
        assertThat(clip.getCanonicalUri(), is("http://www.channel4.com/programmes/green-wing/video/series-1/episode-7/guyball"));
        assertThat(clip.getPublisher(), is(C4PmlsdModule.SOURCE));
        assertThat(clip.getLastUpdated(), is(clock.now()));
        assertThat(clip.getIsLongForm(), is(false));
        assertThat(clip.getMediaType(), is(MediaType.VIDEO));
        assertThat(clip.getSpecialization(), is(Specialization.TV));
        assertThat(clip.getTitle(), is("Guyball"));        
        assertThat(clip.getDescription(), is("'This country could soon have a new world champion'"));
        assertThat(clip.getImage(), is("http://www.channel4.com/assets/programmes/images/green-wing/series-1/episode-7/guyball/green-wing-s1e7-guyball_625x352.jpg"));
        assertThat(clip.getThumbnail(), is("http://www.channel4.com/assets/programmes/images/green-wing/series-1/episode-7/guyball/green-wing-s1e7-guyball_200x113.jpg"));
   
    }

}
