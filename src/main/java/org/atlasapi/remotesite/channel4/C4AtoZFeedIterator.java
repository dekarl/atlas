package org.atlasapi.remotesite.channel4;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atlasapi.persistence.system.AToZUriSource;
import org.atlasapi.remotesite.support.atom.AtomClient;

import com.google.common.base.Optional;
import com.google.common.collect.AbstractIterator;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.url.Urls;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.atom.Link;

public class C4AtoZFeedIterator extends AbstractIterator<Optional<Feed>> {
    
    private final Log log = LogFactory.getLog(getClass());

    private static final Pattern PAGE_PATTERN = Pattern.compile("http://[^.]*\\.channel4\\.com/[^/]*/atoz/(.+/page-\\d+).atom.*");

    private final AtomClient client;
    private final String uriBase;
    private final Optional<String> platform;
    private final Iterator<String> letterIterator;
    
    private String nextPageUri = null;

    public C4AtoZFeedIterator(SimpleHttpClient client, String uriBase, Optional<String> platform) {
        this.client = new AtomClient(client);
        this.uriBase = uriBase;
        this.platform = platform;
        this.letterIterator = new AToZUriSource("", "", true).iterator();
    }
    
    @Override
    protected Optional<Feed> computeNext() {
        String nextUri = nextUri();
        if (nextUri == null) {
            return endOfData();
        }
        nextUri = optionallyAppendPlatform(nextUri);
        Feed feed = null;
        try {
            feed = client.get(nextUri);
            nextPageUri = extractNextPageUri(feed);
        } catch (HttpException e) {
            log.warn(e.getResponse().statusCode() + ": Failed to fetch " + nextUri);
        } catch (Exception e) {
        }
        return Optional.fromNullable(feed);
    }

    private String optionallyAppendPlatform(String url) {
        return platform.isPresent() ? appendPlatform(url) : url;
    }
    
    private String appendPlatform(String url) {
        return Urls.appendParameters(url, "platform", platform.get());
    }

    private String nextUri() {
        if (nextPageUri != null) {
            return constructUri(nextPageUri);
        } else if (letterIterator.hasNext()) {
            return constructUri(letterIterator.next());
        }
        return null;
    }

    private String constructUri(String letter) {
        return String.format("%satoz/%s.atom", uriBase, letter);
    }

    @SuppressWarnings("unchecked")
    private String extractNextPageUri(Feed feed) {
        for (Link link : (List<Link>) feed.getOtherLinks()) {
            if ("next".equals(link.getRel())) {
                String next = link.getHref();
                Matcher matcher = PAGE_PATTERN.matcher(next);
                if (matcher.matches()) {
                    return matcher.group(1);
                }
            }
        }
        return null;
    }

}
