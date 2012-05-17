package org.atlasapi.remotesite.channel4;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import junit.framework.TestCase;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Publisher;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Optional;
import com.google.common.io.Resources;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpRequest;
import com.sun.syndication.feed.atom.Feed;

@RunWith(MockitoJUnitRunner.class)
public class C4AtoZAtomContentUpdateTaskTest extends TestCase {

    String apiBase = "http://pmlsc.channel4.com/pmlsd";
	String uri = "http://www.channel4.com/programmes/atoz/a";
	
	private C4BrandUpdater brandAdapter = mock(C4BrandUpdater.class);
	private SimpleHttpClient client = mock(SimpleHttpClient.class);
	
	Brand brand101 = new Brand("http://www.channel4.com/programmes/a-bipolar-expedition", "curie:101", Publisher.C4);
	Brand brand202 = new Brand("http://www.channel4.com/programmes/a-bipolar-expedition-part-2", "curie:202", Publisher.C4);
	
	private final AtomFeedBuilder atoza = new AtomFeedBuilder(Resources.getResource(getClass(), "a.atom"));
	private final AtomFeedBuilder atoza2 = new AtomFeedBuilder(Resources.getResource(getClass(), "a2.atom"));

	private final AtomFeedBuilder ps3atoza = new AtomFeedBuilder(Resources.getResource(getClass(), "atoz-ps3.atom"));

    @Test
	public void testRequestsFeedsAndPassesExtractedUrisToAdapter() throws Exception {
        
        C4AtoZAtomContentUpdateTask adapter = new C4AtoZAtomContentUpdateTask(client, apiBase, brandAdapter);
	
		when(client.get(requestFor("http://pmlsc.channel4.com/pmlsd/atoz/a.atom"))).thenReturn(atoza.build());
		when(client.get(requestFor("http://pmlsc.channel4.com/pmlsd/atoz/a/page-2.atom"))).thenReturn(atoza2.build());
		when(brandAdapter.canFetch(anyString())).thenReturn(true);

		adapter.run();

		verify(brandAdapter).canFetch("http://www.channel4.com/programmes/a-bipolar-expedition");
		verify(brandAdapter).createOrUpdateBrand("http://www.channel4.com/programmes/a-bipolar-expedition");
		verify(brandAdapter).canFetch("http://www.channel4.com/programmes/a-bipolar-expedition-part-2");
		verify(brandAdapter).createOrUpdateBrand("http://www.channel4.com/programmes/a-bipolar-expedition-part-2");
	
    }
    
    @Test
    public void testRequestsFeedsAndPassesExtractedUrisToAdapterWithPlatform() throws Exception {
        
        C4AtoZAtomContentUpdateTask adapter = new C4AtoZAtomContentUpdateTask(client, apiBase, Optional.of("ps3"), brandAdapter);
    
        when(client.get(requestFor("http://pmlsc.channel4.com/pmlsd/atoz/a.atom?platform=ps3"))).thenReturn(ps3atoza.build());
        when(brandAdapter.canFetch(anyString())).thenReturn(true);

        adapter.run();

        verify(brandAdapter).canFetch("http://www.channel4.com/programmes/a-place-by-the-sea");
        verify(brandAdapter).createOrUpdateBrand("http://www.channel4.com/programmes/a-place-by-the-sea");
        verify(brandAdapter).canFetch("http://www.channel4.com/programmes/a-place-in-the-sun-home-or-away");
        verify(brandAdapter).createOrUpdateBrand("http://www.channel4.com/programmes/a-place-in-the-sun-home-or-away");
    
    }
    
    private SimpleHttpRequest<Feed> requestFor(final String uri) {
        return Mockito.argThat(new TypeSafeMatcher<SimpleHttpRequest<Feed>>() {

            @Override
            public void describeTo(Description desc) {
                desc.appendText("request for " + uri);
            }

            @Override
            public boolean matchesSafely(SimpleHttpRequest<Feed> request) {
                return uri.equals(request.getUrl());
            }
        });
    }
}
