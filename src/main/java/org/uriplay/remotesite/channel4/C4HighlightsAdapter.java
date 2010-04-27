/* Copyright 2009 Meta Broadcast Ltd

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */

package org.uriplay.remotesite.channel4;

import javax.xml.bind.JAXBException;

import org.jherd.beans.id.IdGeneratorFactory;
import org.jherd.remotesite.SiteSpecificAdapter;
import org.jherd.remotesite.SiteSpecificRepresentationAdapter;
import org.jherd.remotesite.http.RemoteSiteClient;
import org.uriplay.media.entity.Brand;

/**
 * {@link SiteSpecificRepresentationAdapter} for screen-scraping from Channel4's 4OD website
 *  
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class C4HighlightsAdapter extends BaseC4PlaylistClient {

	private static final String HIGHLIGHTS_URI = "http://www.channel4.com/programmes/4od/highlights";
	private static final String CURRENT_MOST_POPULAR_URI = "http://www.channel4.com/programmes/4od/most-popular";
	
	public C4HighlightsAdapter() throws JAXBException {
		this(new C4HomePageClient(), new C4AtomBackedBrandAdapter());
	}
	
	public C4HighlightsAdapter(RemoteSiteClient<BrandListingPage> brandListClient, SiteSpecificAdapter<Brand> brandClient) {
		super(brandListClient, brandClient);
	}

	public boolean canFetch(String uri) {
		return uri.startsWith(CURRENT_MOST_POPULAR_URI) || uri.startsWith(HIGHLIGHTS_URI);
	}
}
