package org.atlasapi.remotesite.channel4.epg;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.remotesite.channel4.C4AtomContentResolver;
import org.atlasapi.remotesite.channel4.epg.model.C4EpgEntry;

import com.google.common.base.Optional;

public class C4EpgEntryContentResolver {

    private final C4AtomContentResolver resolver;
    private final C4EpgEntryUriExtractor uriExtractor = new C4EpgEntryUriExtractor();
    
    public C4EpgEntryContentResolver(ContentResolver resolver) {
        this.resolver = new C4AtomContentResolver(resolver);
    }
    
    public Optional<Brand> resolveBrand(C4EpgEntry entry) {
        Optional<String> brandUri = uriExtractor.uriForBrand(entry);
        return brandUri.isPresent() ? resolver.brandFor(brandUri.get())
                                    : Optional.<Brand>absent();
    }
    
    public Optional<Series> resolveSeries(C4EpgEntry entry) {
        Optional<String> seriesUri = uriExtractor.uriForSeries(entry);
        return seriesUri.isPresent() ? resolver.seriesFor(seriesUri.get())
                                    : Optional.<Series>absent();
    }

    public Optional<Item> itemFor(C4EpgEntry entry, Optional<Brand> brand) {
        String idBasedUri = uriExtractor.uriForItemId(entry);
        Optional<String> hierarchyUri = uriExtractor.uriForItemHierarchy(entry, brand);
        Optional<String> synthUri = uriExtractor.uriForItemSynthesized(entry, brand);
        
        return resolver.itemFor(idBasedUri, hierarchyUri, synthUri);
    }

}
