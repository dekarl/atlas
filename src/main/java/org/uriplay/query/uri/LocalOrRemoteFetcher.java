/* Copyright 2009 British Broadcasting Corporation
   Copyright 2009 Meta Broadcast Ltd

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */

package org.uriplay.query.uri;


import org.uriplay.media.entity.Content;
import org.uriplay.persistence.content.ContentStore;
import org.uriplay.persistence.system.Fetcher;

/**
 * Aggregate {@link Fetcher} that checks a local datastore for resources,
 * before trying to fetch them remotely.
 *
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class LocalOrRemoteFetcher implements Fetcher<Content> {

	private final ContentStore localStore;
	
	private final Fetcher<Content> remoteFetcher;
	
	public LocalOrRemoteFetcher(ContentStore localStore, Fetcher<Content> remoteFetcher) {
		this.localStore = localStore;
		this.remoteFetcher = remoteFetcher;
	}

	public Content fetch(String uri) {
		
		Content local = localStore.findByUri(uri);
		
		if (local == null) {
			return remoteFetcher.fetch(uri); 
		} else {
		    return local;
		}
	}

}
