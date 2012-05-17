/* Copyright 2010 Meta Broadcast Ltd

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */

package org.atlasapi.remotesite.support.atom;

import static com.metabroadcast.common.http.SimpleHttpRequest.httpRequestFrom;

import java.io.InputStreamReader;

import org.atlasapi.http.AbstractHttpResponseTransformer;
import org.atlasapi.persistence.system.RemoteSiteClient;
import org.atlasapi.remotesite.HttpClients;

import com.metabroadcast.common.http.SimpleHttpClient;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.io.WireFeedInput;

public class AtomClient implements RemoteSiteClient<Feed> {
		
	private final SimpleHttpClient client;

	public AtomClient(SimpleHttpClient client) {
		this.client = client;
	}
	
	public AtomClient() {
		this(HttpClients.webserviceClient());
	}

	@Override
	public Feed get(final String uri) throws Exception {
	    return client.get(httpRequestFrom(uri, new AbstractHttpResponseTransformer<Feed>() {
            @Override
            protected Feed transform(InputStreamReader bodyReader) throws Exception {
                return (Feed) new WireFeedInput().build(bodyReader);
            }
        }));
	}
}
