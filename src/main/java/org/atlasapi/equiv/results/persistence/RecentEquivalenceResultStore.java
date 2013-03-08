package org.atlasapi.equiv.results.persistence;

import java.util.List;

import org.atlasapi.equiv.results.EquivalenceResult;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;

public class RecentEquivalenceResultStore implements EquivalenceResultStore {

    private final EquivalenceResultStore delegate;
    private final Cache<String, StoredEquivalenceResult> mrwItemCache;
    private final Cache<String, StoredEquivalenceResult> mrwContainerCache;

    public RecentEquivalenceResultStore(EquivalenceResultStore delegate) {
        this.delegate = delegate;
        this.mrwItemCache = CacheBuilder.newBuilder().maximumSize(50).build();
        this.mrwContainerCache = CacheBuilder.newBuilder().maximumSize(50).build();
    }
    
    @Override
    public <T extends Content> StoredEquivalenceResult store(EquivalenceResult<T> result) {
        StoredEquivalenceResult restoredResult = delegate.store(result);
        if(result.subject() instanceof Item) {
            mrwItemCache.put(result.subject().getCanonicalUri(), restoredResult);
        }
        if(result.subject() instanceof Container) {
            mrwContainerCache.put(result.subject().getCanonicalUri(), restoredResult);
        }
        return restoredResult;
    }

    @Override
    public StoredEquivalenceResult forId(String canonicalUri) {
        StoredEquivalenceResult equivalenceResult = mrwItemCache.getIfPresent(canonicalUri);
        if(equivalenceResult != null) {
            return equivalenceResult;
        }
        
        equivalenceResult = mrwContainerCache.getIfPresent(canonicalUri);
        if(equivalenceResult != null) {
            return equivalenceResult;
        }
        return delegate.forId(canonicalUri);
    }
    
    @Override
    public List<StoredEquivalenceResult> forIds(Iterable<String> canonicalUris) {
        return delegate.forIds(canonicalUris);
    }
    
    public List<StoredEquivalenceResult> latestItemResults() {
        return ImmutableList.copyOf(mrwItemCache.asMap().values());
    }

    public List<StoredEquivalenceResult> latestContainerResults() {
        return ImmutableList.copyOf(mrwContainerCache.asMap().values());
    }

}
