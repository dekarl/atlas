package org.atlasapi.remotesite.netflix;

import static org.atlasapi.remotesite.netflix.NetflixEpisodeParseTest.extractXmlFromFile;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Set;

import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Specialization;
import org.junit.Test;
import org.mockito.Mockito;

public class NetflixSeriesParseTest {
    
    private final NetflixContentExtractor<Series> seriesExtractor = new NetflixSeriesExtractor();

    @SuppressWarnings("unchecked")
    @Test
    public void testSeriesParsing() throws ValidityException, ParsingException, IOException {
        Element element = extractXmlFromFile("netflix-episode.xml");
        
        NetflixContentExtractor<Episode> episodeExtractor = new NetflixEpisodeExtractor();
        NetflixXmlElementContentExtractor extractor = new NetflixXmlElementContentExtractor(Mockito.mock(NetflixContentExtractor.class), Mockito.mock(NetflixContentExtractor.class), episodeExtractor, seriesExtractor);

        
        Set<? extends Content> contents = extractor.extract(element);
        
        assertThat(contents.size(), is(2));
        
        Series series= null;
        for (Content content : contents) {
            if (content instanceof Series) {
                series = (Series) content;
                break;
            }
        }
        
        // check that an Series was in the contents list
        assertFalse(series == null);
        
        assertThat(series.getCanonicalUri(), equalTo("http://gb.netflix.com/seasons/70136130-4"));
        assertThat(series.getTitle(), equalTo("Season 4"));
        assertThat(series.getSeriesNumber(), is(4));

        assertEquals(series.getSpecialization(), Specialization.TV);
    }

}
