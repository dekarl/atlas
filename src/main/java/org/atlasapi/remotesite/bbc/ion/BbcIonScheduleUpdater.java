package org.atlasapi.remotesite.bbc.ion;

import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.DEBUG;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.WARN;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.atlasapi.remotesite.HttpClients;
import org.atlasapi.remotesite.bbc.BbcAliasCompiler;
import org.atlasapi.remotesite.bbc.ion.BbcIonDeserializers.BbcIonDeserializer;
import org.atlasapi.remotesite.bbc.ion.model.IonBroadcast;
import org.atlasapi.remotesite.bbc.ion.model.IonEpisode;
import org.atlasapi.remotesite.bbc.ion.model.IonSchedule;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.time.DateTimeZones;

public class BbcIonScheduleUpdater implements Runnable {

    private final Iterable<String> uriSource;
    private final ContentResolver localFetcher;
    private final AdapterLog log;

    private final ContentWriter writer;
    private final BbcIonDeserializer<IonSchedule> deserialiser;
    private final ExecutorService executor;
    private BbcItemFetcherClient fetcherClient;
    private SimpleHttpClient httpClient;

    public BbcIonScheduleUpdater(ExecutorService executor, Iterable<String> uriSource, ContentResolver localFetcher, ContentWriter writer, BbcIonDeserializer<IonSchedule> deserialiser, AdapterLog log) {
        this.executor = executor;
        this.uriSource = uriSource;
        this.localFetcher = localFetcher;
        this.writer = writer;
        this.deserialiser = deserialiser;
        this.log = log;
    }
    
    public BbcIonScheduleUpdater(Iterable<String> uriSource, ContentResolver localFetcher, ContentWriter writer, BbcIonDeserializer<IonSchedule> deserialiser, AdapterLog log) {
        this(Executors.newFixedThreadPool(5), uriSource,localFetcher, writer,deserialiser, log);
    }

    public BbcIonScheduleUpdater withItemFetchClient(BbcItemFetcherClient fetcherClient) {
        this.fetcherClient = fetcherClient;
        return this;
    }
    
    public BbcIonScheduleUpdater withHttpClient(SimpleHttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }
    
    @Override
    public void run() {
        DateTime start = new DateTime(DateTimeZones.UTC);
        log.record(new AdapterLogEntry(Severity.INFO).withSource(getClass()).withDescription("BBC Ion Schedule Update initiated"));

        for (String uri : uriSource) {
            executor.submit(new BbcIonScheduleUpdateTask(uri,this.httpClient != null ? this.httpClient : HttpClients.webserviceClient()));
        }
        executor.shutdown();
        boolean completion = false;
        try {
            completion = executor.awaitTermination(30, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            log.record(new AdapterLogEntry(Severity.WARN).withSource(getClass()).withDescription("BBC Ion Schedule Update interrupted waiting for completion").withCause(e));
        }
        
        String runTime = new Period(start, new DateTime(DateTimeZones.UTC)).toString(PeriodFormat.getDefault());
        log.record(new AdapterLogEntry(Severity.INFO).withSource(getClass()).withDescription("BBC Ion Schedule Update finished in " + runTime + (completion ? "" : " (timed-out)")));
    }

    private class BbcIonScheduleUpdateTask implements Runnable {

        private static final String BBC_CURIE_BASE = "bbc:";
        private static final String SLASH_PROGRAMMES_ROOT = "http://www.bbc.co.uk/programmes/";
        private final String uri;
        private final SimpleHttpClient httpClient;

        public BbcIonScheduleUpdateTask(String uri, SimpleHttpClient httpClient){
            this.uri = uri;
            this.httpClient = httpClient;
        }

        @Override
        public void run() {
            log.record(new AdapterLogEntry(DEBUG).withSource(getClass()).withDescription("BBC Ion Schedule update for " + uri));
            try {
                IonSchedule schedule = deserialiser.deserialise(httpClient.getContentsOf(uri));
                for (IonBroadcast broadcast : schedule.getBlocklist()) {
                    //find and (create and) update item
                    Item item = (Item) localFetcher.findByCanonicalUri(SLASH_PROGRAMMES_ROOT + broadcast.getEpisodeId());
                    if (item == null) {
                        if(fetcherClient != null) {
                            item = fetcherClient.createItem(broadcast.getEpisodeId());
                        } 
                        if(item == null) {
                            item = createItemFrom(broadcast);
                        }
                    }
                    updateItemDetails(item, broadcast);
                    if (item instanceof Episode) {
                        updateEpisodeDetails((Episode) item, broadcast);
                    }
                    //if no series and no brand just store item
                    if(Strings.isNullOrEmpty(broadcast.getSeriesId()) && Strings.isNullOrEmpty(broadcast.getBrandId())) {
                        writer.createOrUpdate(item);
                    } else {
                        Series series = null;
                        if (!Strings.isNullOrEmpty(broadcast.getSeriesId())) {
                            series = (Series) localFetcher.findByCanonicalUri(SLASH_PROGRAMMES_ROOT + broadcast.getSeriesId());
                            if (series == null) {
                                series = createSeries(broadcast);
                            }
                            updateSeries(series, broadcast);
                            addOrReplaceItemInPlaylist(item, series);
                        }
                        if (Strings.isNullOrEmpty(broadcast.getBrandId())) {
                            if(series != null) { //no brand so just save series if it exists
                                writer.createOrUpdate(series, true);
                            }
                        } else {
                            Brand brand = (Brand) localFetcher.findByCanonicalUri(SLASH_PROGRAMMES_ROOT + broadcast.getBrandId());
                            if (brand == null) {
                                brand = createBrandFrom(broadcast);
                            }
                            updateBrand(brand, broadcast);
                            addOrReplaceItemInPlaylist(item, brand);
                            writer.createOrUpdate(brand, true);
                        }
                    }
                }
            } catch (Exception e) {
                log.record(new AdapterLogEntry(Severity.ERROR).withCause(e).withDescription("BBC Ion Updater failed for " + uri).withSource(getClass()));
            }
        }

        @SuppressWarnings("unchecked")
		private <T extends Item> void addOrReplaceItemInPlaylist(Item item, Container<T> playlist) {
            int itemIndex = playlist.getContents().indexOf(item);
            if (itemIndex >= 0) {
                List<T> items = Lists.newArrayList(playlist.getContents());
                items.set(itemIndex, (T) item);
                playlist.setContents(items);
            } else {
                playlist.addContents((T) item);
            }
        }

        private void updateSeries(Series series, IonBroadcast broadcast) {
            series.setTitle(broadcast.getEpisode().getSeriesTitle());
        }

        private Series createSeries(IonBroadcast broadcast) {
            return new Series(SLASH_PROGRAMMES_ROOT + broadcast.getSeriesId(), BBC_CURIE_BASE + broadcast.getSeriesId(), Publisher.BBC);
        }

        private Brand createBrandFrom(IonBroadcast broadcast) {
            return new Brand(SLASH_PROGRAMMES_ROOT + broadcast.getBrandId(), BBC_CURIE_BASE + broadcast.getBrandId(), Publisher.BBC);
        }

        private void updateBrand(Brand brand, IonBroadcast broadcast) {
            brand.setTitle(broadcast.getEpisode().getBrandTitle());
        }

        private Item createItemFrom(IonBroadcast broadcast) {
            IonEpisode ionEpisode = broadcast.getEpisode();
            Item item;
            if (!Strings.isNullOrEmpty(broadcast.getBrandId()) || !Strings.isNullOrEmpty(broadcast.getSeriesId())) {
                item = new Episode(SLASH_PROGRAMMES_ROOT + broadcast.getEpisodeId(), BBC_CURIE_BASE + ionEpisode.getId(), Publisher.BBC);
            } else {
                item = new Item(SLASH_PROGRAMMES_ROOT + broadcast.getEpisodeId(), BBC_CURIE_BASE + ionEpisode.getId(), Publisher.BBC);
            }
            item.setAliases(BbcAliasCompiler.bbcAliasUrisFor(item.getCanonicalUri()));
            item.setIsLongForm(true);
            if (!Strings.isNullOrEmpty(broadcast.getMediaType())) {
                item.setMediaType(MediaType.valueOf(broadcast.getMediaType().toUpperCase()));
            }
            if (ionEpisode.getIsFilm() != null && ionEpisode.getIsFilm()) {
                item.setSpecialization(Specialization.FILM);
            }
            return item;
        }

        private void updateEpisodeDetails(Episode item, IonBroadcast broadcast) {
            // not sure how useful this is. there isn't mush else we can do.
            IonEpisode episode = broadcast.getEpisode();
            if (Strings.isNullOrEmpty(episode.getSubseriesId()) && episode.getPosition() != null) {
                item.setEpisodeNumber(Ints.saturatedCast(episode.getPosition()));
            }
        }

        private void updateItemDetails(Item item, IonBroadcast ionBroadcast) {


            Version broadcastVersion = getBroadcastVersion(item, ionBroadcast);
            if (broadcastVersion == null) {
                broadcastVersion = versionFrom(ionBroadcast);
                item.addVersion(broadcastVersion);
            }
            
            Broadcast broadcast = atlasBroadcastFrom(ionBroadcast);
            if(broadcast!= null) {
                broadcastVersion.addBroadcast(broadcast);
            }

            IonEpisode episode = ionBroadcast.getEpisode();
            item.setTitle(episode.getTitle());
            item.setDescription(episode.getSynopsis());
            item.setThumbnail(episode.getMyImageBaseUrl() + episode.getId() + "_150_84.jpg");
            item.setImage(episode.getMyImageBaseUrl() + episode.getId() + "_640_360.jpg");

        }

        private Version versionFrom(IonBroadcast ionBroadcast) {
            Version version = new Version();

            version.setCanonicalUri(SLASH_PROGRAMMES_ROOT + ionBroadcast.getVersionId());
            version.setDuration(Duration.standardSeconds(ionBroadcast.getDuration()));
            version.setProvider(Publisher.BBC);
            
            return version;
        }

        private Version getBroadcastVersion(Item item, IonBroadcast ionBroadcast) {
            for (Version version : item.getVersions()) {
                if (version.getCanonicalUri().equals(SLASH_PROGRAMMES_ROOT + ionBroadcast.getVersionId())) {
                    return version;
                }
            }
            return null;
        }

        private Broadcast atlasBroadcastFrom(IonBroadcast ionBroadcast) {
            String serviceUri = BbcIonServices.get(ionBroadcast.getService());
            if(serviceUri == null) {
                log.record(new AdapterLogEntry(WARN).withDescription("Couldn't find service URI for Ion Service " + ionBroadcast.getService()).withSource(getClass()));
                return null;
            } else {
                Broadcast broadcast = new Broadcast(serviceUri, ionBroadcast.getStart(), ionBroadcast.getEnd());
                broadcast.withId(BBC_CURIE_BASE + ionBroadcast.getId()).setScheduleDate(ionBroadcast.getDate().toLocalDate());
                broadcast.setLastUpdated(ionBroadcast.getUpdated());
                return broadcast;
            }
        }

    }

}
