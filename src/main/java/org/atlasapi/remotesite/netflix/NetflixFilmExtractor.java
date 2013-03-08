package org.atlasapi.remotesite.netflix;

import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getAlias;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getCertificates;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getDescription;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getGenres;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getPeople;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getPublisher;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getTitle;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getVersion;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getYear;

import java.util.Set;

import nu.xom.Element;

import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Specialization;

import com.google.common.collect.ImmutableSet;

public class NetflixFilmExtractor implements NetflixContentExtractor<Film>  {
    
    private static final String MOVIES_URL_PREFIX = "http://gb.netflix.com/movies/";
    
    @Override
    public Set<Film> extract(Element source, int id) {
        Film film = new Film();

        film.setCanonicalUri(MOVIES_URL_PREFIX + id);

        film.setTitle(getTitle(source));
        film.setYear(getYear(source));
        film.setDescription(getDescription(source));
        film.addVersion(getVersion(source, id));
        film.setGenres(getGenres(source));
        film.setPeople(getPeople(source));
        film.setCertificates(getCertificates(source));
        // TODO new alias
        film.addAliasUrl(getAlias(source));
        film.setPublisher(getPublisher());
        film.setSpecialization(Specialization.FILM);

        return ImmutableSet.of(film);
    }
}
