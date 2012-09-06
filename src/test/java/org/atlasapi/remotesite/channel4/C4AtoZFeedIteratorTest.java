package org.atlasapi.remotesite.channel4;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpRequest;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.atom.Link;

@RunWith(MockitoJUnitRunner.class)
public class C4AtoZFeedIteratorTest {

    private final SimpleHttpClient client = mock(SimpleHttpClient.class);
    
    @Test
    public void testIteratesOverAtoZFeeds() throws HttpException, Exception {
        
        String base = "base/";
        Optional<String> platform = Optional.absent();
        C4AtoZFeedIterator iterator = new C4AtoZFeedIterator(client, base, platform);
        
        String next = "http://asdf.channel4.com/zxcv/atoz/z/page-1.atom";
        when(client.get(requestFor("base/atoz/z.atom"))).thenReturn(feedWithNextLink(next));
        when(client.get(requestFor("base/atoz/z/page-1.atom"))).thenReturn(new Feed());
        
        iterator.next();
        iterator.next();
        iterator.next();
        
        verify(client).get(requestFor("base/atoz/z.atom"));
        verify(client).get(requestFor("base/atoz/z/page-1.atom"));
        verify(client).get(requestFor("base/atoz/y.atom"));
    }
    
    @Test
    public void testIteratesOverAtoZFeedsWithPlatformPresent() throws HttpException, Exception {
        
        String base = "base/";
        Optional<String> platform = Optional.of("ps3");
        C4AtoZFeedIterator iterator = new C4AtoZFeedIterator(client, base, platform);
        
        String next = "http://ps3.channel4.com/zxcv/atoz/z/page-1.atom?platform=ps3";
        when(client.get(requestFor("base/atoz/z.atom?platform=ps3"))).thenReturn(feedWithNextLink(next));
        when(client.get(requestFor("base/atoz/z/page-1.atom?platform=ps3"))).thenReturn(new Feed());
        
        iterator.next();
        iterator.next();
        iterator.next();
        
        verify(client).get(requestFor("base/atoz/z.atom?platform=ps3"));
        verify(client).get(requestFor("base/atoz/z/page-1.atom?platform=ps3"));
        verify(client).get(requestFor("base/atoz/y.atom?platform=ps3"));
    }
    
    private Feed feedWithNextLink(String uri) {
        Feed feed = new Feed();
        feed.setOtherLinks(ImmutableList.<Link>of(
            nextLinkWithUri(uri)
        ));
        return feed;
    }

    private Link nextLinkWithUri(String uri) {
        Link link = new Link();
        link.setRel("next");
        link.setHref(uri);
        return link;
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
