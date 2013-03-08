package org.atlasapi.remotesite.bbc.ion;

import static org.atlasapi.media.entity.Publisher.BBC;
import static org.atlasapi.persistence.logging.AdapterLogEntry.warnEntry;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.WARN;
import static org.atlasapi.remotesite.bbc.ion.BbcIonContainerFetcherClient.CONTAINER_DETAIL_PATTERN;

import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.system.RemoteSiteClient;
import org.atlasapi.remotesite.bbc.BbcAliasCompiler;
import org.atlasapi.remotesite.bbc.BbcFeeds;
import org.atlasapi.remotesite.bbc.BbcProgrammesGenreMap;
import org.atlasapi.remotesite.bbc.ion.model.IonContainer;
import org.atlasapi.remotesite.bbc.ion.model.IonContainerFeed;
import org.atlasapi.remotesite.bbc.ion.model.IonContributor;
import org.atlasapi.remotesite.bbc.ion.model.IonEpisode;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;
import com.metabroadcast.common.base.Maybe;

public abstract class BaseBbcIonEpisodeItemExtractor {

    protected static final String CURIE_BASE = "bbc:";

    private final BbcIonContributorPersonExtractor personExtractor = new BbcIonContributorPersonExtractor();
    private final BbcIonGenreMap genreMap = new BbcIonGenreMap(new BbcProgrammesGenreMap());
    private final RemoteSiteClient<IonContainerFeed> containerClient;
    private final AdapterLog log;
    
    public BaseBbcIonEpisodeItemExtractor(AdapterLog log) {
        this(log, null);
    }
    
    public BaseBbcIonEpisodeItemExtractor(AdapterLog log, RemoteSiteClient<IonContainerFeed> containerClient) {
        this.log = log;
        this.containerClient = containerClient;
    }

    protected Item extract(IonEpisode source) {
        Item item = null;
        if (source.getIsFilm()) {
            item = new Film(BbcFeeds.slashProgrammesUriForPid(source.getId()), CURIE_BASE+source.getId(), BBC);
            item.setMediaType(MediaType.VIDEO);
            item.setSpecialization(Specialization.FILM);
            
        } else if (!Strings.isNullOrEmpty(source.getBrandId()) || !Strings.isNullOrEmpty(source.getSeriesId())) {
            item = new Episode(BbcFeeds.slashProgrammesUriForPid(source.getId()), CURIE_BASE+source.getId(), BBC);
            setEpisodeDetails((Episode)item, source);
            setMediaTypeAndSpecialisation(item, source);
            
        } else {
            item = new Item(BbcFeeds.slashProgrammesUriForPid(source.getId()), CURIE_BASE+source.getId(), BBC);
            setMediaTypeAndSpecialisation(item, source);
        }
        return setItemDetails(item, source);
    }

    protected void setEpisodeDetails(Episode item, IonEpisode episodeDetail) {
        if(!Strings.isNullOrEmpty(episodeDetail.getSeriesId())) {
            item.setSeriesRef(new ParentRef(BbcFeeds.slashProgrammesUriForPid(episodeDetail.getSeriesId())));
        }
        
        if(episodeDetail.getPosition() == null) {
            return;
        }
        
        String subseriesId = episodeDetail.getSubseriesId();

        if (Strings.isNullOrEmpty(subseriesId)) {
            item.setEpisodeNumber(Ints.saturatedCast(episodeDetail.getPosition()));
            return;
        }

        if (item.getPartNumber() == null && containerClient != null) {
            try {
                IonContainer subseries = Iterables.getOnlyElement(containerClient.get(String.format(CONTAINER_DETAIL_PATTERN, subseriesId)).getBlocklist());
                item.setEpisodeNumber(Ints.saturatedCast(subseries.getPosition()));
                item.setPartNumber(Ints.saturatedCast(episodeDetail.getPosition()));
            } catch (Exception e) {
                log.record(warnEntry().withSource(getClass()).withDescription("Updating item %s, couldn't fetch subseries %s", subseriesId));
            }
        }
    }

    protected Item setItemDetails(Item item, IonEpisode episode) {
        
        item.setTitle(getTitle(episode));
        item.setDescription(episode.getSynopsis());
        // TODO new alias
        item.setAliasUrls(BbcAliasCompiler.bbcAliasUrisFor(item.getCanonicalUri()));
        item.setIsLongForm(true);
        item.setLastUpdated(episode.getUpdated());
        item.setGenres(genreMap.fromIon(episode.getGenres()));
        if (!Strings.isNullOrEmpty(episode.getId())) {
            BbcImageUrlCreator.addImagesTo(episode.getMyImageBaseUrl().toString(), episode.getId(), item);
        }
        
        if (episode.getContributors() != null) {
            for (IonContributor contributor: episode.getContributors()) {
                Maybe<CrewMember> possiblePerson = personExtractor.extract(contributor);
                if (possiblePerson.hasValue()) {
                    item.addPerson(possiblePerson.requireValue());
                } else {
                    log.record(new AdapterLogEntry(WARN).withSource(getClass()).withDescription("Unknown person: " + contributor.getRoleName()));
                }
            }
        }

        return item;
    }

    protected void setMediaTypeAndSpecialisation(Item item, IonEpisode episode) {
        if(!Strings.isNullOrEmpty(episode.getMediaType())) {
            String mediaType = episode.getMediaType();
            if (mediaType.equals("audio")) {
                item.setMediaType(MediaType.AUDIO);
                item.setSpecialization(Specialization.RADIO);
            } else if (mediaType.equals("video")) {
                item.setMediaType(MediaType.VIDEO);
                item.setSpecialization(Specialization.TV);
            }
        }
    }
    
    protected String getTitle(IonEpisode episode) {
        String title = !Strings.isNullOrEmpty(episode.getOriginalTitle()) ? episode.getOriginalTitle() : episode.getTitle();
        if(!Strings.isNullOrEmpty(episode.getSubseriesTitle())) {
            title = String.format("%s %s", episode.getSubseriesTitle(), title);
        }
        return title;
    }
}
