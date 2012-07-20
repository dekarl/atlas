package org.atlasapi.remotesite.channel4;

import java.util.List;
import java.util.regex.Pattern;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.ContentResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.metabroadcast.common.base.Maybe;

public class C4AtomContentResolver {

    private Logger log = LoggerFactory.getLogger(C4AtomContentResolver.class);
    
    private final Pattern PROGRAMME_ID_URI = Pattern.compile("http://www.channel4.com/programmes/\\d+/\\d+");
    private final ContentResolver resolver;

    public C4AtomContentResolver(ContentResolver resolver) {
        this.resolver = resolver;
    }
    
    public Optional<Item> itemFor(String id, Optional<String> canonicalUri, Optional<String> slotIdUri) {
        List<String> ids = Lists.newArrayList(id);
        if (canonicalUri.isPresent()) {
            ids.add(canonicalUri.get());
        }
        if (slotIdUri.isPresent()) {
            ids.add(slotIdUri.get());
        }
        List<Identified> results = resolver.findByCanonicalUris(ids).getAllResolvedResults();
        Iterable<Item> itemFilteredResults = ImmutableSet.copyOf(Iterables.filter(results, Item.class));
        /*
         * CODE FOR STAGE ONLY: Filter to return only hierarchy URI. programmeId URI 
         * created erroneously in stage. Data cleanup will be done to remove once
         * we have identified items
         */

        if(Iterables.size(itemFilteredResults) > 1) {
            log.error(String.format("Multiple items found for item. ProgrammeId URI [%s]. Hierarchy URI [%s]. SlotId URI [%s]", id, canonicalUri.orNull(), slotIdUri.orNull()));
            itemFilteredResults = Iterables.filter(itemFilteredResults, new Predicate<Item>() {

                @Override
                public boolean apply(Item input) {
                    return !PROGRAMME_ID_URI.matcher(input.getCanonicalUri()).matches();
                }
                
            });
        }
        /*
         * END CODE FOR STAGE ONLY
         */
        return Optional.fromNullable(Iterables.getOnlyElement(itemFilteredResults,null));
    }
    
    public Optional<Brand> brandFor(String canonicalUri) {
        Maybe<Identified> result = resolver.findByCanonicalUris(ImmutableList.of(canonicalUri)).get(canonicalUri);
        if (result.hasValue() && result.requireValue() instanceof Brand) {
            return Optional.of((Brand)result.requireValue());
        }
        return Optional.absent();
    }
    
    public Optional<Series> seriesFor(String canonicalUri) {
        Maybe<Identified> result = resolver.findByCanonicalUris(ImmutableList.of(canonicalUri)).get(canonicalUri);
        if (result.hasValue() && result.requireValue() instanceof Brand) {
            return Optional.of((Series)result.requireValue());
        }
        return Optional.absent();
    }
}
