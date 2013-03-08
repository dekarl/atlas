package org.atlasapi.remotesite.netflix;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import nu.xom.Document;

import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponsePrologue;
import com.metabroadcast.common.http.HttpResponseTransformer;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpClientBuilder;
import com.metabroadcast.common.http.SimpleHttpRequest;

public class NetflixFileUpdater {
    
    private final String url;
    private final NetflixDataStore fileStore;
    private SimpleHttpClient client;
    
    
    public NetflixFileUpdater(String url, NetflixDataStore fileStore, int timeout) {
        this.url = url;
        this.fileStore = fileStore;
        SimpleHttpClientBuilder httpClientBuilder = new SimpleHttpClientBuilder();
        this.client = httpClientBuilder.withSocketTimeout(timeout, TimeUnit.SECONDS).build();
    }

    public Document updateFile() throws HttpException, Exception {
        client.get(new SimpleHttpRequest<Void>(url, new HttpResponseTransformer<Void>(){
            @Override
            public Void transform(HttpResponsePrologue prologue, InputStream body) throws HttpException, Exception {
                fileStore.save(body);
                return null;
            }
        }));
        return fileStore.getData();
    }
}
