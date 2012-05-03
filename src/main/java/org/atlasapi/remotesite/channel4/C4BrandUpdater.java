package org.atlasapi.remotesite.channel4;

import org.atlasapi.media.entity.Brand;

//Why is this here? Isn't it just a SiteSpecificAdapter<Brand>?
public interface C4BrandUpdater {
	
	Brand createOrUpdateBrand(String uri);

	boolean canFetch(String uri);

}
