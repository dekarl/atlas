package org.atlasapi.remotesite.netflix;

import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.TITLE_KEY;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getAlias;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getCertificates;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getDescription;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getEpisodeNumber;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getGenres;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getPublisher;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getSeriesNumber;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getShowId;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getVersion;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getYear;

import java.util.Set;

import nu.xom.Element;

import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Specialization;

import com.google.common.collect.ImmutableSet;

public class NetflixEpisodeExtractor implements NetflixContentExtractor<Episode> {
    
    private static final String EPISODE_URL_PREFIX = "http://gb.netflix.com/episodes/";

    @Override
    public Set<Episode> extract(Element source, int id) {
        Episode episode = new Episode();

        episode.setCanonicalUri(EPISODE_URL_PREFIX + id);

        episode.setTitle(getTitle(source));
        episode.setDescription(getDescription(source));
        episode.addVersion(getVersion(source, id));
        episode.setYear(getYear(source));
        episode.setGenres(getGenres(source));
        episode.setCertificates(getCertificates(source));
        // TODO new alias
        episode.addAliasUrl(getAlias(source));
        episode.setParentRef(getParentRef(source));
        episode.setSeriesRef(getSeriesRef(source));
        episode.setEpisodeNumber(getEpisodeNumber(source));
        episode.setSeriesNumber(getSeriesNumber(source));
        episode.setPublisher(getPublisher());
        episode.setSpecialization(Specialization.TV);

        return ImmutableSet.of(episode);
    }

    private ParentRef getParentRef(Element episodeElement) {
        return new ParentRef(NetflixBrandExtractor.BRAND_URL_PREFIX + getShowId(episodeElement));
    }

    private ParentRef getSeriesRef(Element episodeElement) {
        return new ParentRef(NetflixSeriesExtractor.SERIES_URL_PREFIX + getShowId(episodeElement) + "-" + getSeriesNumber(episodeElement));
    }

    String getTitle(Element episodeElement) {
        Element titleElement = episodeElement.getFirstChildElement(TITLE_KEY);
        if (titleElement != null) {
            String[] parts = titleElement.getValue().split(":");
            // return last part of title, trimmed of whitespace
            // leading and trailing quotes removed if present
            return parts[parts.length - 1].trim().replaceAll("^\"|\"$", "");
        }
        throw new ElementNotFoundException(episodeElement, TITLE_KEY);
    }

}
