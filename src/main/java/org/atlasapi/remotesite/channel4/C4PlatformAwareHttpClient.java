package org.atlasapi.remotesite.channel4;

import com.google.common.base.Optional;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.Payload;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpRequest;
import com.metabroadcast.common.url.Urls;

public class C4PlatformAwareHttpClient implements SimpleHttpClient {

    private final SimpleHttpClient delegate;
    private final Optional<String> platform;
    
    public C4PlatformAwareHttpClient(SimpleHttpClient delegate) {
        this(delegate, Optional.<String>absent());
    }

    public C4PlatformAwareHttpClient(SimpleHttpClient delegate, Optional<String> platform) {
        this.delegate = delegate;
        this.platform = platform;
    }
    
    @Override
    public String getContentsOf(String url) throws HttpException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T get(SimpleHttpRequest<T> request) throws HttpException, Exception {
        return platform.isPresent() ? delegate.get(appendPlatformTo(request))
                                    : delegate.get(request);
    }

    private <T> SimpleHttpRequest<T> appendPlatformTo(SimpleHttpRequest<T> request) {
        return new SimpleHttpRequest<T>(appendPlatform(request.getUrl()), request.getTransformer());
    }

    private String appendPlatform(String url) {
        return Urls.appendParameters(url, "platform", platform.get());
    }

    @Override
    public HttpResponse get(String url) throws HttpException {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpResponse post(String url, Payload data) throws HttpException {
        return delegate.post(optionallyAppendPlatform(url), data);
    }

    @Override
    public HttpResponse head(String url) throws HttpException {
        return delegate.head(optionallyAppendPlatform(url));
    }

    @Override
    public HttpResponse put(String url, Payload data) throws HttpException {
        return delegate.put(optionallyAppendPlatform(url), data);
    }
    
    private String optionallyAppendPlatform(String url) {
        return platform.isPresent() ? appendPlatform(url) : url;
    }

}
