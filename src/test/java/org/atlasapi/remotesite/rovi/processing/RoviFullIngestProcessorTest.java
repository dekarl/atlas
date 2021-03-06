package org.atlasapi.remotesite.rovi.processing;

import static org.atlasapi.remotesite.rovi.RoviCanonicalUriGenerator.canonicalUriForProgram;
import static org.atlasapi.remotesite.rovi.RoviCanonicalUriGenerator.canonicalUriForSeason;
import static org.atlasapi.remotesite.rovi.RoviTestUtils.fileFromResource;
import static org.atlasapi.remotesite.rovi.RoviTestUtils.resolvedContent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Locale;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.LookupRef;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.remotesite.rovi.RoviConstants;
import org.atlasapi.remotesite.rovi.RoviContentWriter;
import org.atlasapi.remotesite.rovi.RoviTestUtils;
import org.atlasapi.remotesite.rovi.indexing.MapBasedKeyedFileIndexer;
import org.atlasapi.remotesite.rovi.model.RoviCulture;
import org.atlasapi.remotesite.rovi.model.RoviEpisodeSequenceLine;
import org.atlasapi.remotesite.rovi.model.RoviProgramDescriptionLine;
import org.atlasapi.remotesite.rovi.parsers.RoviEpisodeSequenceLineParser;
import org.atlasapi.remotesite.rovi.parsers.RoviProgramDescriptionLineParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

@RunWith(MockitoJUnitRunner.class)
public class RoviFullIngestProcessorTest {

    private static final String PARENT_BRAND_ID = "19914879";
    private static final String PARENT_SERIES_ID = "19914933";
    private static final String PARENT_FILM_ID = "2353207";
    private static final int SEASON_NUMBER = 5;
    private static final Locale EN_US_LOCALE = RoviCulture.localeFromCulture("English - NA");

    private static final String PROGRAM_FILE = "org/atlasapi/remotesite/rovi/program.txt";
    private static final String PROGRAM_DESCRIPTION = "org/atlasapi/remotesite/rovi/program_description.txt";
    private static final String EPISODE_SEQUENCE = "org/atlasapi/remotesite/rovi/episode_sequence.txt";
    private static final String SEASON_HISTORY_SEQUENCE = "org/atlasapi/remotesite/rovi/season_history.txt";
    private static final String SCHEDULE = "org/atlasapi/remotesite/rovi/schedule.txt";

    private RoviFullIngestProcessor processor;

    @Mock private RoviContentWriter contentWriter;

    @Mock private ContentResolver contentResolver;

    @Mock private ScheduleFileProcessor scheduleProcessor;

    private ArgumentCaptor<? extends Content> argument = ArgumentCaptor.forClass(Content.class);

    @Before
    public void init() {

        instructContentResolver();

        processor = new RoviFullIngestProcessor(
                descriptionsIndexer(),
                episodeSequenceIndexer(),
                contentWriter,
                contentResolver,
                scheduleProcessor,
                new AuxiliaryCacheSupplier(contentResolver));
    }

    @Test
    public void testProcessing() throws IOException {
        processor.process(fileFromResource(PROGRAM_FILE),
                fileFromResource(SEASON_HISTORY_SEQUENCE),
                fileFromResource(SCHEDULE),
                fileFromResource(PROGRAM_DESCRIPTION),
                fileFromResource(EPISODE_SEQUENCE));

        Mockito.verify(contentWriter, atLeastOnce()).writeContent(argument.capture());

        ImmutableMap<String, ? extends Content> items = Maps.uniqueIndex(argument.getAllValues(),
                new Function<Content, String>() {

                    @Override
                    public String apply(Content input) {
                        return input.getCanonicalUri();
                    }
                });
        
        Content film = items.get(canonicalUriForProgram("15354310"));
        assertThat(film, notNullValue());
        assertThat(film, is(Film.class));
        assertThat(film.getTitle(), equalTo("Puritan: European Flings"));
        assertThat(film.getPublisher(), equalTo(Publisher.ROVI_EN));
        assertThat(film.getEquivalentTo().isEmpty(), is(true));
        assertThat(film.getDescription(), nullValue());

        Content film2 = items.get(canonicalUriForProgram("15340869"));
        assertThat(film2, notNullValue());
        assertThat(film2, is(Film.class));
        assertThat(film2.getTitle(), equalTo("Operation Sandman"));
        assertThat(film2.getPublisher(), equalTo(Publisher.ROVI_EN));
        assertThat(film2.getEquivalentTo().isEmpty(), is(false));
        assertThat(film2.getEquivalentTo(), hasItem(LookupRef.from(parentFilm())));
        assertThat(film2.getLocalizedDescription(EN_US_LOCALE).get().getDescription(),
                equalTo("Ron Perlman and wrestler Hardcore Holly star in a thriller in which a military study on sleep deprivation causes the soldiers' deepest fears to become real. Winslow: Persia White. Martin: John Haymes Newton. Jean: Mary Ward. Riggins: Richard Tyson."));

        Content item = items.get(canonicalUriForProgram("15342102"));
        assertThat(item, notNullValue());
        assertThat(item, is(Item.class));
        assertThat(item.getTitle(), equalTo("Party People - The Great Digital Debate"));
        assertThat(item.getPublisher(), equalTo(Publisher.ROVI_EN));
        assertThat(item.getEquivalentTo().isEmpty(), is(true));
        assertThat(item.getLocalizedDescriptions().isEmpty(), is(true));

        Content brand = items.get(canonicalUriForProgram("19914879"));
        assertThat(brand, notNullValue());
        assertThat(brand, is(Brand.class));
        assertThat(brand.getTitle(), equalTo("Doctor Who"));
        assertThat(brand.getPublisher(), equalTo(Publisher.ROVI_EN));
        assertThat(brand.getEquivalentTo().isEmpty(), is(true));
        assertThat(brand.getLocalizedDescriptions().size(), is(2));
        assertThat(brand.getLocalizedDescription(EN_US_LOCALE).get().getShortDescription(),
                equalTo("Following the adventures of a time-traveling alien called \"The Doctor\" and his human companions as they deal with crises set on Earth and other worlds."));

        Content seriesContent = items.get(canonicalUriForSeason("19914933"));
        assertThat(seriesContent, notNullValue());
        assertThat(seriesContent, is(Series.class));
        assertThat(seriesContent.getTitle(), equalTo("Series 5"));
        assertThat(seriesContent.getPublisher(), equalTo(Publisher.ROVI_EN));

        Series series = (Series) seriesContent;
        assertThat(series.getSeriesNumber(), equalTo(5));
        assertThat(series.getParent(), equalTo(ParentRef.parentRefFrom(parentBrand())));

        Content episode1Content = items.get(canonicalUriForProgram("16004587"));
        assertThat(episode1Content, notNullValue());
        assertThat(episode1Content, is(Episode.class));
        assertThat(episode1Content.getTitle(), equalTo("The Time of Angels"));
        assertThat(episode1Content.getPublisher(), equalTo(Publisher.ROVI_EN));

        Episode episode1 = (Episode) episode1Content;
        assertThat(episode1.getEpisodeNumber(), equalTo(4));
        assertThat(episode1.getSeriesNumber(), equalTo(5));
        assertThat(episode1.getSeriesRef(), equalTo(ParentRef.parentRefFrom(parentSeries())));
        assertThat(episode1.getContainer(), equalTo(ParentRef.parentRefFrom(parentBrand())));

        Content episode2Content = items.get(canonicalUriForProgram("16233385"));
        assertThat(episode2Content, notNullValue());
        assertThat(episode2Content, is(Episode.class));
        assertThat(episode2Content.getTitle(), equalTo("The Vampires of Venice"));
        assertThat(episode2Content.getPublisher(), equalTo(Publisher.ROVI_EN));

        Episode episode2 = (Episode) episode2Content;
        assertThat(episode2.getEpisodeNumber(), equalTo(6));
        assertThat(episode2.getSeriesNumber(), equalTo(5));
        assertThat(episode2.getSeriesRef(), equalTo(ParentRef.parentRefFrom(parentSeries())));
        assertThat(episode2.getContainer(), equalTo(ParentRef.parentRefFrom(parentBrand())));

    }

    private MapBasedKeyedFileIndexer<String, RoviProgramDescriptionLine> descriptionsIndexer() {
        return new MapBasedKeyedFileIndexer<>(
                RoviConstants.FILE_CHARSET,
                new RoviProgramDescriptionLineParser());
    }

    private MapBasedKeyedFileIndexer<String, RoviEpisodeSequenceLine> episodeSequenceIndexer() {
        return new MapBasedKeyedFileIndexer<>(
                RoviConstants.FILE_CHARSET,
                new RoviEpisodeSequenceLineParser());
    }

    private void instructContentResolver() {
        when(contentResolver.findByCanonicalUris(Mockito.anyCollectionOf(String.class)))
            .thenReturn(RoviTestUtils.unresolvedContent());
        
        when(contentResolver.findByCanonicalUris(ImmutableList.of(canonicalUriForProgram(PARENT_BRAND_ID))))
                .thenReturn(resolvedContent(parentBrand()));

        when(contentResolver.findByCanonicalUris(ImmutableList.of(canonicalUriForProgram(PARENT_FILM_ID))))
                .thenReturn(resolvedContent(parentFilm()));
        
        Series series = parentSeries();
        series.withSeriesNumber(SEASON_NUMBER);
        when(contentResolver.findByCanonicalUris(ImmutableList.of(canonicalUriForSeason(PARENT_SERIES_ID))))
                .thenReturn(resolvedContent(series));     
    }

    private Brand parentBrand() {
        return basicBrand(PARENT_BRAND_ID, Publisher.ROVI_EN);
    }

    private Film parentFilm() {
        return basicFilm(PARENT_FILM_ID, Publisher.ROVI_FR);
    }

    private Series parentSeries() {
        return basicSeries(PARENT_SERIES_ID, Publisher.ROVI_EN);
    }

    private Brand basicBrand(String id, Publisher publisher) {
        Brand brand = new Brand();
        brand.setCanonicalUri(canonicalUriForProgram(id));
        brand.setPublisher(publisher);
        return brand;
    }

    private Film basicFilm(String id, Publisher publisher) {
        Film film = new Film();
        film.setCanonicalUri(canonicalUriForProgram(id));
        film.setPublisher(publisher);
        return film;
    }

    private Series basicSeries(String id, Publisher publisher) {
        Series series = new Series();
        series.setCanonicalUri(canonicalUriForSeason(id));
        series.setPublisher(publisher);
        return series;
    }

}
