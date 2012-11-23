package org.atlasapi.remotesite.lovefilm;

import static org.atlasapi.media.entity.Policy.RevenueContract.FREE_TO_VIEW;
import static org.atlasapi.media.entity.Policy.RevenueContract.PAY_TO_RENT;
import static org.atlasapi.media.entity.Policy.RevenueContract.SUBSCRIPTION;
import static org.atlasapi.media.entity.Publisher.LOVEFILM;
import static org.atlasapi.remotesite.lovefilm.LoveFilmCsvColumn.ACCESS_METHOD;
import static org.atlasapi.remotesite.lovefilm.LoveFilmCsvColumn.BBFC_RATING;
import static org.atlasapi.remotesite.lovefilm.LoveFilmCsvColumn.CONTRIBUTOR;
import static org.atlasapi.remotesite.lovefilm.LoveFilmCsvColumn.DRM_RIGHTS;
import static org.atlasapi.remotesite.lovefilm.LoveFilmCsvColumn.EPISODE_SEQUENCE;
import static org.atlasapi.remotesite.lovefilm.LoveFilmCsvColumn.EXTERNAL_PRODUCT_DESCRIPTION_URL;
import static org.atlasapi.remotesite.lovefilm.LoveFilmCsvColumn.GENRE;
import static org.atlasapi.remotesite.lovefilm.LoveFilmCsvColumn.HEROSHOT_URL;
import static org.atlasapi.remotesite.lovefilm.LoveFilmCsvColumn.ITEM_NAME;
import static org.atlasapi.remotesite.lovefilm.LoveFilmCsvColumn.ITEM_TYPE_KEYWORD;
import static org.atlasapi.remotesite.lovefilm.LoveFilmCsvColumn.LANGUAGE;
import static org.atlasapi.remotesite.lovefilm.LoveFilmCsvColumn.ORIGINAL_PUBLICATION_DATE;
import static org.atlasapi.remotesite.lovefilm.LoveFilmCsvColumn.PRODUCT_SITE_LAUNCH_DATE;
import static org.atlasapi.remotesite.lovefilm.LoveFilmCsvColumn.RELEASE_WINDOW_END_DATE;
import static org.atlasapi.remotesite.lovefilm.LoveFilmCsvColumn.SERIES_ID;
import static org.atlasapi.remotesite.lovefilm.LoveFilmCsvColumn.SHOW_ID;
import static org.atlasapi.remotesite.lovefilm.LoveFilmCsvColumn.SKU;

import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.media.entity.Policy.RevenueContract;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.atlasapi.remotesite.ContentExtractor;
import org.atlasapi.remotesite.lovefilm.LoveFilmData.LoveFilmDataRow;
import org.atlasapi.remotesite.util.EnglishLanguageCodeMap;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.collect.ImmutableOptionalMap;
import com.metabroadcast.common.collect.OptionalMap;
import com.metabroadcast.common.currency.Price;
import com.metabroadcast.common.intl.Countries;

public class LoveFilmDataRowContentExtractor implements ContentExtractor<LoveFilmDataRow, Optional<Content>> {

    private static final String UNKNOWN_LANGUAGE = "unknown";
    private static final String LOVEFILM_PEOPLE_PREFIX = "http://lovefilm.com/people/";
    private static final String LOVEFILM_CURIE_PATTERN = "lf:%s-%s";
    private static final String LOVEFILM_URI_PATTERN = "http://lovefilm.com/%s/%s";
    private static final String LOVEFILM_GENRES_PREFIX = "http://lovefilm.com/genres/";

    private static final String EPISODE_RESOURCE_TYPE = "episodes";
    private static final String SEASON_RESOURCE_TYPE = "seasons";
    private static final String SHOW_RESOURCE_TYPE = "shows";
    
    private static final String TELE_VIDEO_RECS = "television-video-recordings";
    private static final String VOD = "VOD";
    private static final String EPISODE = "episode";
    private static final String SEASON = "season";
    private static final String SHOW = "show";

    private static final Splitter COMMA_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();
    private final DateTimeFormatter dateMonthYearFormat = DateTimeFormat.forPattern("dd/MM/YYYY").withZoneUTC();
    
    private static final EnglishLanguageCodeMap languageCodeMap = new EnglishLanguageCodeMap();
    private static final OptionalMap<String, Certificate> certificateMap = ImmutableOptionalMap.fromMap(
        ImmutableMap.<String,Certificate>builder()
            .put("exempt",new Certificate("E", Countries.GB))
            .put("universal_childrens",new Certificate("U", Countries.GB))
            .put("parental_guidance",new Certificate("PG", Countries.GB))
            .put("ages_12_and_over",new Certificate("12", Countries.GB))
            .put("ages_15_and_over",new Certificate("15", Countries.GB))
            .put("ages_18_and_over",new Certificate("18", Countries.GB))
        .build()
    );
    private final Map<String, RevenueContract> revenueContractMap = ImmutableMap.of(
        "avod", FREE_TO_VIEW, 
        "svod", SUBSCRIPTION, 
        "tvod", PAY_TO_RENT
    );

    @Override
    public Optional<Content> extract(LoveFilmDataRow source) {
        if (LoveFilmCsvColumn.ENTITY.valueIs(source, SHOW)) {
            return extractBrand(source);
        }
        if (LoveFilmCsvColumn.ENTITY.valueIs(source, SEASON)) {
            return extractSeries(source);
        }
        if (LoveFilmCsvColumn.ENTITY.valueIs(source, EPISODE)) {
            if(!(ITEM_TYPE_KEYWORD.valueIs(source, TELE_VIDEO_RECS)
                    && ACCESS_METHOD.valueIs(source, VOD))){
                return Optional.absent();
            }
            return extractEpisode(source);
        }
        return Optional.absent();
    }

    private Optional<Content> extractBrand(LoveFilmDataRow source) {
        Brand brand = createBrand(source);
        
        return Optional.of(setCommonFields(brand, source));
    }

    private Optional<Content> extractSeries(LoveFilmDataRow source) {
        Content content;
        if (SKU.valueIs(source, SERIES_ID.valueFrom(source))) {
            content = createBrand(source);
        } else {
            Series series = createSeason(source);
            
            String episodeSequence = EPISODE_SEQUENCE.valueFrom(source);
            if (!Strings.isNullOrEmpty(episodeSequence)) {
                series.withSeriesNumber(Integer.valueOf(episodeSequence));
            }
            series.setParentRef(new ParentRef(uri(SHOW_ID.valueFrom(source),SHOW_RESOURCE_TYPE)));
            content = series;
        }
        
        return Optional.of(setCommonFields(content, source));
    }

    private Optional<Content> extractEpisode(LoveFilmDataRow source) {
        String parentId = SHOW_ID.valueFrom(source);
        Item item = createItem(source);
        
        String episodeSequence = EPISODE_SEQUENCE.valueFrom(source);
        if (!Strings.isNullOrEmpty(parentId) && !Strings.isNullOrEmpty(episodeSequence)) {
            Episode episode = createEpisode(source);
            if (Strings.emptyToNull(episodeSequence) != null) {
                episode.setEpisodeNumber(Integer.valueOf(episodeSequence));
            }
            if (!SERIES_ID.valueIs(source, parentId)) {
                episode.setSeriesRef(new ParentRef(uri(SERIES_ID.valueFrom(source), SEASON_RESOURCE_TYPE)));
            }
            item = episode;
        } 

        if (!Strings.isNullOrEmpty(parentId)) {
            item.setParentRef(new ParentRef(uri(parentId, SHOW_RESOURCE_TYPE)));
        }
        
        item.setVersions(versionAndLocationFrom(source));
        
        Content itemWithCommonFields = setCommonFields(item, source);
        itemWithCommonFields.setTitle(episodeTitle(source, item));
        return Optional.of(itemWithCommonFields);
    }
    
    private String episodeTitle(LoveFilmDataRow source, Item item) {
        String sourceTitle = ITEM_NAME.valueFrom(source);
        String extractedTitle = extractTitle(sourceTitle);
        Integer episodeNumber = episodeNumber(item);
        if (Strings.isNullOrEmpty(extractedTitle)) {
            if (episodeNumber != null) {
                return "Episode " + episodeNumber;
            }
            return sourceTitle;
        }
        return extractedTitle;
    }

    protected Integer episodeNumber(Item item) {
        if (item instanceof Episode) {
            return ((Episode)item).getEpisodeNumber();
        }
        return null;
    }

    private static final Pattern EPISODE_TITLE_PATTERN = Pattern.compile("^[\\S ]+ - (E\\d+|S\\d+ E\\d+|)( - )?(.*)$");

    private String extractTitle(String title) {
        Matcher matcher = EPISODE_TITLE_PATTERN.matcher(title);
        if (matcher.matches() && matcher.groupCount() >= 3) {
            return matcher.group(3);
        }
        return title;
    }

    private Brand createBrand(LoveFilmDataRow source) {
        return createContent(new Brand(), source, SHOW_RESOURCE_TYPE, "b");
    }
    
    private Series createSeason(LoveFilmDataRow source) {
        return createContent(new Series(), source, SEASON_RESOURCE_TYPE, "s");
    }

    private Episode createEpisode(LoveFilmDataRow source) {
        return createContent(new Episode(), source, EPISODE_RESOURCE_TYPE, "e");
    }

    private Item createItem(LoveFilmDataRow source) {
        return createContent(new Item(), source, EPISODE_RESOURCE_TYPE, "e");
    }

    private <C extends Content> C createContent(C content, LoveFilmDataRow row, String resourceType, String curieType) {
        String sku = SKU.valueFrom(row);
        content.setPublisher(LOVEFILM);
        content.setCanonicalUri(uri(sku, resourceType));
        content.setCurie(curie(sku, curieType));
        return content;
    }
    
    private String uri(String id, String resource) {
        String uri = String.format(LOVEFILM_URI_PATTERN, resource, id);
        return uri;
    }
    
    private String curie(String id, String curieType) {
        String curie = String.format(LOVEFILM_CURIE_PATTERN, curieType, id);
        return curie;
    }

    private Content setCommonFields(Content content, LoveFilmDataRow source) {
        content.setTitle(ITEM_NAME.valueFrom(source));
        content.setImage(HEROSHOT_URL.valueFrom(source));
        content.setYear(yearFrom(ORIGINAL_PUBLICATION_DATE.valueFrom(source)));
        content.setGenres(genresFrom(GENRE.valueFrom(source)));
        content.setPeople(peopleFrom(CONTRIBUTOR.valueFrom(source)));
        content.setLanguages(languagesFrom(LANGUAGE.valueFrom(source)));
        content.setCertificates(certificatesFrom(BBFC_RATING.valueFrom(source)));
        content.setMediaType(MediaType.VIDEO);
        content.setSpecialization(Specialization.TV);
        return content;
    }
    
    private Integer yearFrom(String pubDate) {
        if (Strings.isNullOrEmpty(pubDate)) {
            return null;
        }
        return dateMonthYearFormat.parseDateTime(pubDate).getYear();
    }
    
    private Iterable<String> genresFrom(String genreCsv) {
        Iterable<String> genres = COMMA_SPLITTER.split(genreCsv);
        return Iterables.transform(genres, new Function<String, String>() {
            @Override
            public String apply(@Nullable String input) {
                input = input.toLowerCase();
                return LOVEFILM_GENRES_PREFIX + input.replace('/', '-').replace(" ", "");
            }
        });
    }


    private List<CrewMember> peopleFrom(String contributorCsv) {
        Iterable<String> contributors = COMMA_SPLITTER.split(contributorCsv);
        Builder<CrewMember> people = ImmutableList.builder();
        for (String contributor : contributors) {
            String[] nameAndId = contributor.split(":");
            if (nameAndId.length == 2) {
                CrewMember member = new CrewMember()
                    .withName(nameAndId[0])
                    .withPublisher(LOVEFILM);
                member.setCanonicalUri(LOVEFILM_PEOPLE_PREFIX+nameAndId[1]);
                people.add(member);
            }
        }
        return people.build();
    }

    private Iterable<String> languagesFrom(String language) {
        if (language.isEmpty() || UNKNOWN_LANGUAGE.equals(language)) {
            return ImmutableList.of();
        }
        return languageCodeMap.codeForEnglishLanguageName(language.toLowerCase()).asSet();
    }
    
    private Iterable<Certificate> certificatesFrom(String certificate) {
        return certificateMap.get(certificate).asSet();
    }
    
    private Set<Version> versionAndLocationFrom(LoveFilmDataRow source) {
        Version version = new Version();
        Encoding encoding = new Encoding();
        
        Location location = new Location();
        location.setUri(EXTERNAL_PRODUCT_DESCRIPTION_URL.valueFrom(source));
        location.setTransportType(TransportType.LINK);
        location.setPolicy(policyFrom(source));
        
        encoding.addAvailableAt(location);
        version.addManifestedAs(encoding);
        return ImmutableSet.of(version);
    }

    private Policy policyFrom(LoveFilmDataRow source) {
        Policy policy = new Policy();
        
        String launchDate = PRODUCT_SITE_LAUNCH_DATE.valueFrom(source);
        policy.setAvailabilityStart(dateTimeFrom(launchDate));
        
        String windowEndDate = RELEASE_WINDOW_END_DATE.valueFrom(source);
        policy.setAvailabilityEnd(dateTimeFrom(windowEndDate));
        
        String drmRights = DRM_RIGHTS.valueFrom(source);
        RevenueContract revenueContract = revenueContractMap.get(drmRights);
        if (SUBSCRIPTION.equals(revenueContract)) {
            policy.setPrice(new Price(Currency.getInstance("GBP"), 4.99));
        }
        policy.setRevenueContract(revenueContract);
        
        policy.setAvailableCountries(ImmutableSet.of(Countries.GB));
        
        return policy;
    }

    private DateTime dateTimeFrom(String date) {
        if (Strings.isNullOrEmpty(date)) {
            return null;
        }
        return dateMonthYearFormat.parseDateTime(date);
    }
}