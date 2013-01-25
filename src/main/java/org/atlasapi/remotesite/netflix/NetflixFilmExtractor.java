package org.atlasapi.remotesite.netflix;

import java.util.Set;

import nu.xom.Element;

import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Specialization;

import com.google.inject.internal.ImmutableSet;

public class NetflixFilmExtractor extends NetflixContentExtractor<Film>  {
    
    private static final String MOVIES_URL_PREFIX = "http://gb.netflix.com/movies/";
    private static final String LOCATIONS_URL_PREFIX = "http://movies.netflix.com/movie/";
    
    @Override
    public Set<Film> extract(Element source, int id) {
        Film film = new Film();

        film.setCanonicalUri(MOVIES_URL_PREFIX + id);

        film.setTitle(getTitle(source));
        film.setYear(getYear(source));
        film.setDescription(getDescription(source));
        film.addVersion(getVersion(source, getEncoding(LOCATIONS_URL_PREFIX + id)));
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
