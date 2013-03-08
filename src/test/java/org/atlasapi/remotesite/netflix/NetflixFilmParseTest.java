package org.atlasapi.remotesite.netflix;

import static org.atlasapi.remotesite.netflix.NetflixEpisodeParseTest.extractXmlFromFile;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Set;

import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.CrewMember.Role;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.media.entity.Version;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.internal.Iterables;
import com.metabroadcast.common.intl.Countries;


public class NetflixFilmParseTest {

    private final NetflixContentExtractor<Film> filmExtractor = new NetflixFilmExtractor();
    
    @SuppressWarnings("unchecked")
    @Test
    public void testFilmParsing() throws ValidityException, ParsingException, IOException {
        Element element = extractXmlFromFile("netflix-film.xml");
        
        NetflixXmlElementContentExtractor extractor = new NetflixXmlElementContentExtractor(filmExtractor, Mockito.mock(NetflixContentExtractor.class), Mockito.mock(NetflixContentExtractor.class), Mockito.mock(NetflixContentExtractor.class));

        
        Set<? extends Content> contents = extractor.extract(element);
        
        Content content = Iterables.getOnlyElement(contents);
        Film film = (Film) content;
        
        // check contents of item
        assertThat(film.getCanonicalUri(), equalTo("http://gb.netflix.com/movies/21930861"));
        assertThat(film.getTitle(), equalTo("The Night Porter"));
        assertThat(film.getDescription(), equalTo("Twelve years after World War II, an ex-Nazi " +
        		"concentration camp guard working as a porter at a hotel is reunited with one of " +
        		"his victims, and contrary to expectations, they resume a twisted and doomed " +
        		"relationship."));
        assertThat(film.getYear(), equalTo(1974));
        
        assertThat(film.getGenres().size(), is(3));
        assertEquals(film.getGenres(), ImmutableSet.of("http://gb.netflix.com/genres/dramas", "http://gb.netflix.com/genres/classicdramas", "http://gb.netflix.com/genres/italianmovies"));
        
        CrewMember dirk = new CrewMember();
        dirk.withName("Dirk Bogarde").withRole(Role.ACTOR);
        dirk.setCanonicalUri("http://gb.netflix.com/people/9321");
        CrewMember charlotte = new CrewMember();
        charlotte.withName("Charlotte Rampling").withRole(Role.ACTOR);
        charlotte.setCanonicalUri("http://gb.netflix.com/people/76167");
        CrewMember philippe = new CrewMember();
        philippe.withName("Philippe Leroy").withRole(Role.ACTOR);
        philippe.setCanonicalUri("http://gb.netflix.com/people/20003720");
        CrewMember gabriele = new CrewMember();
        gabriele.withName("Gabriele Ferzetti").withRole(Role.ACTOR);
        gabriele.setCanonicalUri("http://gb.netflix.com/people/20014925");
        CrewMember giuseppe = new CrewMember();
        giuseppe.withName("Giuseppe Addobbati").withRole(Role.ACTOR);
        giuseppe.setCanonicalUri("http://gb.netflix.com/people/20040845");
        CrewMember isa = new CrewMember();
        isa.withName("Isa Miranda").withRole(Role.ACTOR);
        isa.setCanonicalUri("http://gb.netflix.com/people/64305");
        CrewMember liliana = new CrewMember();
        liliana.withName("Liliana Cavani").withRole(Role.DIRECTOR);
        liliana.setCanonicalUri("http://gb.netflix.com/people/15506");
        
        assertThat(film.getPeople().size(), is(7));
        assertEquals(ImmutableList.copyOf(film.getPeople()), ImmutableList.of(dirk, charlotte, philippe, gabriele, giuseppe, isa, liliana));
        
        assertThat(film.getCertificates().size(), is(1));
        for (Certificate cert : film.getCertificates()) {
            assertThat(cert.classification(), equalTo("18"));
            assertThat(cert.country(), equalTo(Countries.GB));
        }
        
        assertThat(film.getAliasUrls().size(), is(1));
        // TODO new alias
        for (String alias : film.getAliasUrls()) {
            assertThat(alias, equalTo("http://api.netflix.com/catalog/titles/movies/21930861"));
        }

        Version version = Iterables.getOnlyElement(film.getVersions());
        assertThat(version.getDuration(), equalTo(7039));
        Encoding encoding = Iterables.getOnlyElement(version.getManifestedAs());
        Location location = Iterables.getOnlyElement(encoding.getAvailableAt());
        assertEquals(location.getUri(), "http://movies.netflix.com/movie/21930861");
        
        assertEquals(film.getSpecialization(), Specialization.FILM);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testFilmParsingNoLongSynopsis() throws ValidityException, ParsingException, IOException {
        Element element = extractXmlFromFile("netflix-film-short-synopsis.xml");
        
        NetflixXmlElementContentExtractor extractor = new NetflixXmlElementContentExtractor(filmExtractor, Mockito.mock(NetflixContentExtractor.class), Mockito.mock(NetflixContentExtractor.class), Mockito.mock(NetflixContentExtractor.class));


        
        Set<? extends Content> contents = extractor.extract(element);
        
        Content content = Iterables.getOnlyElement(contents);
        Film film = (Film) content;
        
        // check contents of item
        assertThat(film.getDescription(), equalTo("Twelve years after World War II, an ex-Nazi " +
        		"concentration camp guard is reunited with one of his victims, and they resume a " +
        		"twisted relationship."));
    }
}
