package org.atlasapi.remotesite.channel4.epg;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.query.content.PerPublisherCurieExpander;
import org.atlasapi.remotesite.ContentExtractor;
import org.atlasapi.remotesite.channel4.epg.model.C4EpgEntry;

import com.google.common.base.Optional;

public class C4EpgEntryBrandExtractor implements ContentExtractor<C4EpgEntry, Optional<Brand>> {

    private final C4EpgEntryUriExtractor uriExtractor = new C4EpgEntryUriExtractor();
    
    @Override
    public Optional<Brand> extract(C4EpgEntry source) {
        if (!source.hasRelatedLink()) {
            return Optional.absent();
        }
        
        Optional<String> possibleBrandUri = uriExtractor.uriForBrand(source);
        if (!possibleBrandUri.isPresent()) {
            return Optional.absent();
        }
        
        String brandUri = possibleBrandUri.get();
        String curie = PerPublisherCurieExpander.CurieAlgorithm.C4.compact(brandUri);
        
        Brand brand = new Brand(brandUri, curie, Publisher.C4);
        
        if (source.brandTitle() != null) {
            brand.setTitle(source.brandTitle());
        } else {
            brand.setTitle(source.title());
        }
        
        return Optional.of(brand);
    }

}
