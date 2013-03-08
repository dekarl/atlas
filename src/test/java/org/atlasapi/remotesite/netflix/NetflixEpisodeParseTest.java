package org.atlasapi.remotesite.netflix;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.media.entity.Version;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.ImmutableSet;
import com.google.inject.internal.Iterables;
import com.metabroadcast.common.intl.Countries;

public class NetflixEpisodeParseTest {
    
    private final NetflixContentExtractor<Episode> episodeExtractor = new NetflixEpisodeExtractor();

    @SuppressWarnings("unchecked")
    @Test
    public void testEpisodeParsing() throws ValidityException, ParsingException, IOException {
        Element element = extractXmlFromFile("netflix-episode.xml");
        
        NetflixContentExtractor<Series> seriesExtractor = new NetflixSeriesExtractor();
        NetflixXmlElementContentExtractor extractor = new NetflixXmlElementContentExtractor(Mockito.mock(NetflixContentExtractor.class), Mockito.mock(NetflixContentExtractor.class), episodeExtractor, seriesExtractor);

        
        Set<? extends Content> contents = extractor.extract(element);
        
        assertThat(contents.size(), is(2));
        
        Episode episode = null;
        for (Content content : contents) {
            if (content instanceof Episode) {
                episode = (Episode) content;
                break;
            }
        }
        
        // check that an Episode was in the contents list
        assertFalse(episode == null);
        
        // check contents of item
        assertThat(episode.getCanonicalUri(), equalTo("http://gb.netflix.com/episodes/70151113"));
        assertThat(episode.getTitle(), equalTo("Orientation"));
        assertThat(episode.getDescription(), equalTo("Recent events have all the heroes looking for answers. " +
        		"Claire tries to start over at college, but she hits a snag."));
        assertThat(episode.getYear(), equalTo(2009));

        assertThat(episode.getGenres().size(), is(6));
        assertEquals(episode.getGenres(), ImmutableSet.of(
                "http://gb.netflix.com/genres/tvshows", 
                "http://gb.netflix.com/genres/ustvshows", 
                "http://gb.netflix.com/genres/tvaction&adventure",
                "http://gb.netflix.com/genres/tvdramas",
                "http://gb.netflix.com/genres/tvsci-fi&fantasy",
                "http://gb.netflix.com/genres/ustvdramas"
            ));
        
        assertThat(episode.getCertificates().size(), is(1));
        for (Certificate cert : episode.getCertificates()) {
            assertThat(cert.classification(), equalTo("15"));
            assertThat(cert.country(), equalTo(Countries.GB));
        }
        
        assertThat(episode.getAliasUrls().size(), is(1));
        // TODO new alias
        for (String alias : episode.getAliasUrls()) {
            assertThat(alias, equalTo("http://api.netflix.com/catalog/titles/programs/262101/70151113"));
        }

        Version version = Iterables.getOnlyElement(episode.getVersions());
        assertThat(version.getDuration(), equalTo(2608));
        Encoding encoding = Iterables.getOnlyElement(version.getManifestedAs());
        Location location = Iterables.getOnlyElement(encoding.getAvailableAt());
        assertEquals(location.getUri(), "http://movies.netflix.com/movie/70151113");
        
        assertEquals(episode.getSeriesRef(), new ParentRef("http://gb.netflix.com/seasons/70136130-4"));
        assertEquals(episode.getContainer(), new ParentRef("http://gb.netflix.com/shows/70136130"));

        assertThat(episode.getSeriesNumber(), is(4));
        assertThat(episode.getEpisodeNumber(), is(1));

        assertEquals(episode.getSpecialization(), Specialization.TV);
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testEpisodeParsingNoLongSynopsis() throws ValidityException, ParsingException, IOException {
        Element element = extractXmlFromFile("netflix-episode.xml");
        
        NetflixContentExtractor<Series> seriesExtractor = new NetflixSeriesExtractor();       
        NetflixXmlElementContentExtractor extractor = new NetflixXmlElementContentExtractor(Mockito.mock(NetflixContentExtractor.class), Mockito.mock(NetflixContentExtractor.class), episodeExtractor, seriesExtractor);

        
        Set<? extends Content> contents = extractor.extract(element);
        
        assertThat(contents.size(), is(2));
        
        Episode episode = null;
        for (Content content : contents) {
            if (content instanceof Episode) {
                episode = (Episode) content;
                break;
            }
        }
        
        // check that an Episode was in the contents list
        assertFalse(episode == null);
        
        // check contents of item
        assertThat(episode.getDescription(), equalTo("Recent events have all the heroes looking for answers. " +
                "Claire tries to start over at college, but she hits a snag."));
    }
    
    public static Element extractXmlFromFile(String fileName) throws ValidityException, ParsingException, IOException {
        Document netflixData = new Builder().build(new ClassPathResource(fileName).getInputStream());
        return netflixData.getRootElement().getChildElements().get(0);
    }
}
