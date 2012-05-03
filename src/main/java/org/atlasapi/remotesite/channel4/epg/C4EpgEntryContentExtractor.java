package org.atlasapi.remotesite.channel4.epg;

import java.util.Set;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.remotesite.ContentExtractor;
import org.atlasapi.remotesite.channel4.C4BrandUpdater;
import org.atlasapi.remotesite.channel4.epg.model.C4EpgEntry;
import org.joda.time.DateTime;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.SystemClock;

public class C4EpgEntryContentExtractor implements
        ContentExtractor<C4EpgChannelEntry, ContentHierarchyAndBroadcast> {

    private final C4EpgEntryContentResolver resolver;
    private final C4BrandUpdater brandUpdater;
    private final Clock clock;

    private final C4EpgEntryBroadcastExtractor broadcastExtractor = new C4EpgEntryBroadcastExtractor();
    private final C4EpgEntryUriExtractor uriExtractor = new C4EpgEntryUriExtractor();
    private final C4EpgEntryBrandExtractor brandExtractor = new C4EpgEntryBrandExtractor();
    private final C4EpgEntrySeriesExtractor seriesExtractor = new C4EpgEntrySeriesExtractor();
    private final C4EpgEntryItemExtractor itemExtractor;
    
    public C4EpgEntryContentExtractor(ContentResolver resolver, C4BrandUpdater updater, Clock clock) {
        this.resolver =  new C4EpgEntryContentResolver(resolver);
        this.brandUpdater = new C4EpgRelatedLinkBrandUpdater(updater);
        this.itemExtractor = new C4EpgEntryItemExtractor(clock);
        this.clock = clock;
    }

    public C4EpgEntryContentExtractor(ContentResolver contentStore, C4BrandUpdater brandUpdater) {
        this(contentStore, brandUpdater, new SystemClock());
    }

    @Override
    public ContentHierarchyAndBroadcast extract(C4EpgChannelEntry source) {
        DateTime now = clock.now();

        Optional<Brand> brand = resolveBrand(source).or(fetchBrand(source)).or(createBrandFrom(source));
        Optional<Series> series = resolveSeries(source).or(createSeriesFrom(source));
        Item item = resolveItem(source, brand).or(createItem(source, brand, series, now));
        
        ensureAliases(source.getEpgEntry(), item, brand);

        Broadcast broadcast = broadcastExtractor.extract(source);
        broadcast.setLastUpdated(now);
        
        addOrReplaceBroadcast(item, broadcast);
        
        connectHeirarchy(item, series, brand);

        return new ContentHierarchyAndBroadcast(brand, series, item, broadcast);
    }

    private void connectHeirarchy(Item item, Optional<Series> series, Optional<Brand> brand) {
        if (brand.isPresent()) {
            item.setContainer(brand.get());
            if (series.isPresent()) {
                series.get().setParent(brand.get());
                if (item instanceof Episode) {
                    ((Episode) item).setSeries(series.get());
                }
            }
        }
    }

    private void ensureAliases(C4EpgEntry epgEntry, Item item, Optional<Brand> brand) {
        String idUri = uriExtractor.uriForItemId(epgEntry);
        if (!idUri.equals(item.getCanonicalUri())) {
            item.addAlias(idUri);
        }
        Optional<String> hierarchyUri = uriExtractor.uriForItemHierarchy(epgEntry, brand);
        if (hierarchyUri.isPresent() && !hierarchyUri.get().equals(item.getCanonicalUri())) {
            item.addAlias(hierarchyUri.get());
        }
    }

    private void addOrReplaceBroadcast(Item item, Broadcast broadcast) {
        Version version = Iterables.getOnlyElement(item.nativeVersions());
        
        Broadcast existingBroadcast = existingBroadcast(version, broadcast);
        if (existingBroadcast != null && changed(broadcast, existingBroadcast)) {
            Set<Broadcast> broadcasts = Sets.newHashSet(broadcast);
            broadcasts.addAll(version.getBroadcasts());
            version.setBroadcasts(broadcasts);
        } else {
            version.addBroadcast(broadcast);
        }
    }
    
    private static boolean changed(Broadcast newBroadcast, Broadcast broadcast) {
        return !Objects.equal(newBroadcast.getTransmissionTime(),broadcast.getTransmissionTime())
            || !Objects.equal(newBroadcast.getTransmissionEndTime(), broadcast.getTransmissionEndTime())
            || !Objects.equal(newBroadcast.getBroadcastDuration(), broadcast.getBroadcastDuration());
    }

    private Broadcast existingBroadcast(Version version, Broadcast newBroadcast) {
        for (Broadcast broadcast : version.getBroadcasts()) {
            if (broadcast.getSourceId().equals(newBroadcast.getSourceId())) {
                return broadcast;
            }
        }
        return null;
    }

    private Item createItem(C4EpgChannelEntry source, Optional<Brand> brand, Optional<Series> series, DateTime now) {
        return itemExtractor.extract(new C4EpgEntryItemSource(source, brand, series));
    }

    private Optional<Item> resolveItem(C4EpgChannelEntry source, Optional<Brand> brand) {
        return resolver.itemFor(source.getEpgEntry(), brand);
    }

    private Optional<Series> createSeriesFrom(C4EpgChannelEntry source) {
        return seriesExtractor.extract(source.getEpgEntry());
    }

    private Optional<Series> resolveSeries(C4EpgChannelEntry source) {
        return resolver.resolveSeries(source.getEpgEntry());
    }

    private Optional<Brand> createBrandFrom(C4EpgChannelEntry source) {
        return brandExtractor.extract(source.getEpgEntry());
    }

    private Optional<Brand> fetchBrand(C4EpgChannelEntry source) {
        return source.hasRelatedLink()  ? fetchBrand(source.getRelatedLinkUri()) 
                                        : Optional.<Brand>absent();
    }

    private Optional<Brand> fetchBrand(String relatedLinkUri) {
        return Optional.fromNullable(brandUpdater.createOrUpdateBrand(relatedLinkUri));
    }

    private Optional<Brand> resolveBrand(C4EpgChannelEntry source) {
        return resolver.resolveBrand(source.getEpgEntry());
    }


}
