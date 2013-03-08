package org.atlasapi.remotesite.netflix;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Set;
import static org.atlasapi.remotesite.netflix.NetflixEpisodeParseTest.extractXmlFromFile;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Series;
import org.junit.Test;

import com.google.common.collect.Iterables;

public class NetflixContentExtractionDelegationTest {
    
    private final NetflixXmlElementContentExtractor contentExtractor = new NetflixXmlElementContentExtractor(new NetflixFilmExtractor(), new NetflixBrandExtractor(), new NetflixEpisodeExtractor(), new NetflixSeriesExtractor());    

    @Test
    public void testExtractorDelegationForEpisodeData() throws ValidityException, ParsingException, IOException {
        Set<? extends Content> contents = contentExtractor.extract(extractXmlFromFile("netflix-episode.xml"));
        
        assertThat(contents.size(), is(2));
        
        boolean foundEpisode = false;
        boolean foundSeries = false;
        
        for (Content content : contents) {
            if (content instanceof Episode) {
                foundEpisode = true;
            }
            if (content instanceof Series) {
                foundSeries = true;
            }
        }
        
        assertTrue(foundEpisode);
        assertTrue(foundSeries);
    }

    @Test
    public void testExtractorDelegationForFilmData() throws ValidityException, ParsingException, IOException {
        Set<? extends Content> contents = contentExtractor.extract(extractXmlFromFile("netflix-film.xml"));
        
        Content content = Iterables.getOnlyElement(contents);
        assertTrue(content instanceof Film);
    }

    @Test
    public void testExtractorDelegationForBrandData() throws ValidityException, ParsingException, IOException {
        Set<? extends Content> contents = contentExtractor.extract(extractXmlFromFile("netflix-brand.xml"));
        
        Content content = Iterables.getOnlyElement(contents);
        assertTrue(content instanceof Brand);
    }
}
