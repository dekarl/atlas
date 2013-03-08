package org.atlasapi.remotesite.youview;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import nu.xom.Builder;
import nu.xom.Document;

import org.atlasapi.remotesite.HttpClients;
import org.joda.time.DateTime;

import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponsePrologue;
import com.metabroadcast.common.http.HttpResponseTransformer;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpClientBuilder;
import com.metabroadcast.common.http.SimpleHttpRequest;
import com.metabroadcast.common.url.QueryStringParameters;

public class YouViewScheduleFetcher {
    
    private static final String DATE_TIME_FORMAT = "YYYY-MM-dd'T'HH:mm:ss'Z'";
    
    private final String youviewUrl;
    private final SimpleHttpClient client;
    private final XmlHttpResponseTransformer xmlTransformer = new XmlHttpResponseTransformer();
    
    public YouViewScheduleFetcher(String youviewUrl, int timeout) {
        this.youviewUrl = youviewUrl;
        client = new SimpleHttpClientBuilder()
            .withUserAgent(HttpClients.ATLAS_USER_AGENT)
            .withSocketTimeout(timeout, TimeUnit.SECONDS)
            .build();
    }

    // fetch schedule from start to finish
    public Document getSchedule(DateTime start, DateTime finish, int service) throws HttpException, Exception {
        QueryStringParameters qsp = new QueryStringParameters();
        qsp.add("service", "" + service);
        qsp.add("starttime", start.toString(DATE_TIME_FORMAT));
        qsp.add("endtime", finish.toString(DATE_TIME_FORMAT));
        client.get(new SimpleHttpRequest<Void>(youviewUrl + "?" + qsp.toQueryString(), xmlTransformer));
        return xmlTransformer.getXml();
    }
    
    private class XmlHttpResponseTransformer implements HttpResponseTransformer<Void> {
        private Document xml;
        
        @Override
        public Void transform(HttpResponsePrologue prologue, InputStream body) throws HttpException, Exception {
            xml = new Builder().build(body);
            return null;
        }
        
        public Document getXml() {
            return xml;
        }
    }
}
