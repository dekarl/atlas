package org.atlasapi.remotesite.bbc.nitro;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.atlasapi.remotesite.ContentMerger;
import org.atlasapi.remotesite.ContentMerger.MergeStrategy;
import org.atlasapi.remotesite.bbc.BbcFeeds;
import org.atlasapi.remotesite.bbc.ion.BbcIonMediaTypeMapping;
import org.atlasapi.remotesite.bbc.nitro.extract.NitroUtil;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.metabroadcast.atlas.glycerin.model.Broadcast;
import com.metabroadcast.atlas.glycerin.model.PidReference;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.time.Clock;


public class LocalOrRemoteNitroFetcher {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final ContentResolver resolver;
    private final NitroContentAdapter contentAdapter;
    private final Clock clock;
    private final ContentMerger contentMerger;

    public LocalOrRemoteNitroFetcher(ContentResolver resolver, NitroContentAdapter contentAdapter, Clock clock) {
        this.resolver = resolver;
        this.contentAdapter = contentAdapter;
        this.clock = clock;
        this.contentMerger = new ContentMerger(MergeStrategy.MERGE);
    }
    
    public ResolveOrFetchResult<Item> resolveOrFetchItem(Iterable<Broadcast> broadcasts) throws NitroException {
        if (Iterables.isEmpty(broadcasts)) {
            return ResolveOrFetchResult.empty();
        }
        Iterable<PidReference> episodeRefs = toEpisodeRefs(broadcasts);
        ImmutableSet<String> itemUris = toItemUris(episodeRefs);
        ImmutableSet<Item> resolved = resolve(itemUris);
        if (fullFetchPermitted(broadcasts.iterator().next())) {
            ImmutableSet<Item> fetched = contentAdapter.fetchEpisodes(episodeRefs);
            return mergeWithExisting(fetched, resolved);
        } else {
            ImmutableSet<Item> fetched = contentAdapter.fetchEpisodes(unresolved(broadcasts, resolved));
            return new ResolveOrFetchResult<Item>(resolved, fetched);
        }
    }
    
    private ResolveOrFetchResult<Item> mergeWithExisting(ImmutableSet<Item> fetchedItems,
            Set<Item> existingItems) {
        Map<String, Item> fetchedIndex = Maps.newHashMap(Maps.uniqueIndex(fetchedItems, Identified.TO_URI));
        ImmutableSet.Builder<Item> resolved = ImmutableSet.builder();
        for (Item existing : existingItems) {
            Item fetched = fetchedIndex.remove(existing.getCanonicalUri());
            if (fetched != null) {
                existing = contentMerger.merge(existing, fetched);
            }
            resolved.add(existing);
        }
        return new ResolveOrFetchResult<Item>(resolved.build(), fetchedIndex.values());
    }

    private Iterable<PidReference> unresolved(Iterable<Broadcast> broadcasts, ImmutableSet<Item> resolved) {
        Collection<String> resolvedUris = Collections2.transform(resolved, Identified.TO_URI);
        ImmutableList.Builder<PidReference> unresolvedBroadcasts = ImmutableList.builder();
        for (PidReference epRef : toEpisodeRefs(broadcasts)) {
            if (!resolvedUris.contains(BbcFeeds.nitroUriForPid(epRef.getPid()))) {
                unresolvedBroadcasts.add(epRef);
            }
        }
        return unresolvedBroadcasts.build();
    }

    private ImmutableSet<Item> resolve(Iterable<String> itemUris) {
        ResolvedContent resolved = resolver.findByCanonicalUris(itemUris);
        return ImmutableSet.copyOf(Iterables.filter(resolved.getAllResolvedResults(), Item.class));
    }

    private ImmutableSet<String> toItemUris(Iterable<PidReference> pidRefs) {
        return ImmutableSet.copyOf(Iterables.transform(pidRefs, new Function<PidReference, String>() {
            @Override
            public String apply(PidReference input) {
                return BbcFeeds.nitroUriForPid(input.getPid());
            }
        }));
    }

    private Iterable<PidReference> toEpisodeRefs(Iterable<Broadcast> broadcasts) {
        return Iterables.filter(Iterables.transform(broadcasts, new Function<Broadcast, PidReference>() {
            @Override
            public PidReference apply(Broadcast input) {
                final PidReference pidRef = NitroUtil.programmePid(input);
                if (pidRef == null) {
                    log.warn("No programme pid for broadcast {}", input.getPid());
                    return null;
                }
                return pidRef;
            }
        }), Predicates.notNull());
    }

    protected boolean fullFetchPermitted(Broadcast broadcast) {
        LocalDate today = clock.now().toLocalDate();
        LocalDate broadcastDay = NitroUtil.toDateTime(broadcast.getPublishedTime().getStart()).toLocalDate();
        
        Maybe<MediaType> mediaType = BbcIonMediaTypeMapping.mediaTypeForService(broadcast.getService().getSid());
        
        // today -4 for radio, today ± 2 for tv
        // radio are more likely to publish clips after a show has been broadcast
        // so with a limited ingest window it is more important to go back as far as possible for radio
        // to ensure that clips are not missed
        if (mediaType.hasValue() && mediaType.requireValue().equals("audio")) {
            return today.plusDays(1).isAfter(broadcastDay) && today.minusDays(5).isBefore(broadcastDay);    
        } else {
            return today.plusDays(3).isAfter(broadcastDay) && today.minusDays(3).isBefore(broadcastDay);
        }
    }
    
    
    public ImmutableSet<Series> resolveOrFetchSeries(Iterable<Item> items) throws NitroException {
        if (Iterables.isEmpty(items)) {
            return ImmutableSet.of();
        }
        Iterable<Episode> episodes = Iterables.filter(items, Episode.class);
        Iterable<ParentRef> seriesRefs = getSeriesRefs(episodes);
        List<String> seriesUris = Lists.newArrayList(toUris(seriesRefs));
        ResolvedContent resolved = resolver.findByCanonicalUris(seriesUris);
        ImmutableSet<Series> fetched = contentAdapter.fetchSeries(asSeriesPidRefs(resolved.getUnresolved()));
        return ImmutableSet.<Series>builder()
                .addAll(Iterables.filter(resolved.getAllResolvedResults(), Series.class))
                .addAll(fetched)
                .build();
    }

    private Iterable<PidReference> asSeriesPidRefs(List<String> pids) {
        return asTypePidsRefs(pids, "series");
    }

    private Iterable<PidReference> asTypePidsRefs(List<String> pids, final String type) {
        return Iterables.transform(pids, new Function<String, PidReference>(){
            @Override
            public PidReference apply(String input) {
                PidReference pidRef = new PidReference();
                pidRef.setPid(BbcFeeds.pidFrom(input));
                pidRef.setResultType(type);
                return pidRef;
            }});
    }

    private ImmutableSet<String> toUris(Iterable<ParentRef> seriesRefs) {
        return ImmutableSet.copyOf(Iterables.transform(seriesRefs, new Function<ParentRef, String>() {
            @Override
            public String apply(ParentRef input) {
                return input.getUri();
            }
        }));
    }
    
    private Iterable<ParentRef> getSeriesRefs(Iterable<Episode> episodes) {
        return Iterables.filter(Iterables.transform(episodes, new Function<Episode, ParentRef>() {
            @Override
            public ParentRef apply(Episode input) {
                return input.getSeriesRef();
            }
        }), Predicates.notNull());
    }

    public ImmutableSet<Brand> resolveOrFetchBrand(Iterable<Item> items) throws NitroException {
        if (Iterables.isEmpty(items)) {
            return ImmutableSet.of();
        }
        Iterable<ParentRef> containerRefs = containerRefs(items);
        Iterable<String> containerUris = toUris(containerRefs);
        ResolvedContent resolved = resolver.findByCanonicalUris(containerUris);
        ImmutableSet<Brand> fetched = contentAdapter.fetchBrands(asBrandPidRefs(resolved.getUnresolved()));
        return ImmutableSet.<Brand>builder()
                .addAll(Iterables.filter(resolved.getAllResolvedResults(), Brand.class))
                .addAll(fetched)
                .build();
    }
    
    private Iterable<PidReference> asBrandPidRefs(List<String> pids) {
        return asTypePidsRefs(pids, "brand");
    }

    private Iterable<ParentRef> containerRefs(Iterable<Item> items) {
        return Iterables.filter(Iterables.transform(items, new Function<Item, ParentRef>() {
            @Override
            public ParentRef apply(Item input) {
                if (!inTopLevelSeries(input)) {
                    return input.getContainer();
                }
                return null;
            }
        }), Predicates.notNull());
    }

    private boolean inTopLevelSeries(Item item) {
        if (item instanceof Episode) {
            Episode ep = (Episode)item;
            return ep.getSeriesRef() != null 
                && ep.getContainer().equals(ep.getSeriesRef());
        }
        return false;
    }
    
}
