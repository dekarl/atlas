package org.atlasapi.remotesite.bbc.schedule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.logging.NullAdapterLog;
import org.atlasapi.persistence.system.Fetcher;
import org.atlasapi.persistence.system.RemoteSiteClient;
import org.atlasapi.remotesite.bbc.schedule.ChannelSchedule.Programme;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

import com.google.common.collect.Lists;

@SuppressWarnings("unchecked")
public class BbcScheduledProgrammeFetcherTest extends MockObjectTestCase {

	RemoteSiteClient<ChannelSchedule> scheduleClient = mock(RemoteSiteClient.class);
	Fetcher<Identified> fetcher = mock(Fetcher.class);
	ContentResolver localFetcher = mock(ContentResolver.class);
	ContentWriter writer = mock(ContentWriter.class);
	
	ChannelSchedule schedule = new ChannelSchedule();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		schedule.withProgrammes(Lists.newArrayList(new Programme("episode", "b00abcd"), new Programme("episode", "b00efgh"), new Programme("brand", "b00xyz")));
	}
	
	public void testFetchExtractedEpisodePid() throws Exception {
	    
	    final Episode containedInBrand = new Episode("containedInBrandUri", "containedInBrandCurie", Publisher.BBC);
        
	    final Brand brand = new Brand("brandUri", "brandCurie", Publisher.BBC);
        brand.setContents(containedInBrand);
        
        final Episode replacingEpisode = new Episode("containedInBrandUri", "replacingCurie", Publisher.BBC);
        replacingEpisode.setContainer(brand);
        
        final Episode withoutBrand = new Episode("episodeWithoutBrandUri", "episodeWithoutBrandCurie", Publisher.BBC);
		
		BbcScheduledProgrammeUpdater scheduleFetcher = new BbcScheduledProgrammeUpdater(scheduleClient, localFetcher, fetcher, writer, Lists.newArrayList("http://www.bbc.co.uk/bbctwo/programmes/schedules/england/2009/11/05.xml"), new NullAdapterLog());
		
		checking(new Expectations() {{ 
			one(scheduleClient).get("http://www.bbc.co.uk/bbctwo/programmes/schedules/england/2009/11/05.xml"); will(returnValue(schedule));
			one(fetcher).fetch("http://www.bbc.co.uk/programmes/b00abcd"); will(returnValue(replacingEpisode));
			one(fetcher).fetch("http://www.bbc.co.uk/programmes/b00efgh"); will(returnValue(withoutBrand));
			one(localFetcher).findByCanonicalUri("brandUri"); will(returnValue(brand));
			one(writer).createOrUpdate(with(any(Brand.class)), with(true));
			one(writer).createOrUpdate(with(any(Item.class)));
		}});

		scheduleFetcher.run();
		
		assertThat(brand.getContents().size(), is(equalTo(1)));
		assertThat(brand.getContents().get(0).getCurie(), is(equalTo("replacingCurie")));
	}
}
