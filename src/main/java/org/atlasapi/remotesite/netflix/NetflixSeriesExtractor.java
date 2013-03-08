package org.atlasapi.remotesite.netflix;

import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.TITLE_KEY;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getPublisher;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getSeriesNumber;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getShowId;

import java.util.Set;

import nu.xom.Element;

import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Specialization;

import com.google.common.collect.ImmutableSet;

public class NetflixSeriesExtractor implements NetflixContentExtractor<Series> {
    
    public static final String SERIES_URL_PREFIX = "http://gb.netflix.com/seasons/";

    @Override
    public Set<Series> extract(Element source, int id) {
        Series series = new Series();

        series.setCanonicalUri(SERIES_URL_PREFIX + getShowId(source) + "-" + getSeriesNumber(source));

        series.setTitle(getTitle(source));
        series.withSeriesNumber(getSeriesNumber(source));
        series.setPublisher(getPublisher());
        series.setSpecialization(Specialization.TV);

        return ImmutableSet.of(series);
    }

    String getTitle(Element filmElement) throws ElementNotFoundException {
        Element titleElement = filmElement.getFirstChildElement(TITLE_KEY);
        if (titleElement != null) {
            String[] parts = titleElement.getValue().split(":");
            // return second part of title, trimmed of whitespace
            return parts[1].trim();
        }
        throw new ElementNotFoundException(filmElement, TITLE_KEY);
    }

}
