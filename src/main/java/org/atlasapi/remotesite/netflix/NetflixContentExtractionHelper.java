package org.atlasapi.remotesite.netflix;

import java.util.List;
import java.util.Map;
import java.util.Set;

import nu.xom.Element;

import org.apache.commons.lang.StringEscapeUtils;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.CrewMember.Role;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Policy.RevenueContract;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.atlasapi.remotesite.AttributeNotFoundException;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.internal.ImmutableSet;
import com.metabroadcast.common.intl.Countries;

public class NetflixContentExtractionHelper {
    private static final String LONG_SYNOPSIS_KEY = "long_synopsis";
    private static final String SHORT_SYNOPSIS_KEY = "short_synopsis";
    private static final String RELEASE_YEAR_KEY = "release_year";
    public static final String TITLE_KEY = "title";
    private static final String SYSTEM_ATTRIBUTE = "system";
    private static final String ID_ATTRIBUTE = "id";
    private static final String TYPE_ATTRIBUTE = "type";
    private static final String GENRES_KEY = "genres";
    private static final String PEOPLE_KEY = "people";
    private static final String NAME_KEY = "name";
    private static final String DURATION_KEY = "duration";
    private static final String URL_KEY = "url";
    private static final String SHOW_KEY = "show";
    private static final String SEASON_NUMBER_KEY = "season_number";
    private static final String EPISODE_NUMBER_KEY = "episode_number";
    private static final String BBFC = "BBFC";
    private static final String PARENTAL_ADVISORIES_KEY = "parental_advisories";
    private static final String GENRES_URL_PREFIX = "http://gb.netflix.com/genres/";
    private static final String PEOPLE_URL_PREFIX = "http://gb.netflix.com/people/";
    private static final String LOCATIONS_URL_PREFIX = "http://movies.netflix.com/movie/";
    private final static Logger log = LoggerFactory.getLogger(NetflixContentExtractionHelper.class);

    private static final Map<String, Role> TYPE_ROLE_MAPPING = ImmutableMap.<String, Role>builder()
            .put("actor", Role.ACTOR)
            .put("creator", Role.WRITER)
            .put("director", Role.DIRECTOR)
            .build();


    static String getTitle(Element contentElement) {
        Element titleElement = contentElement.getFirstChildElement(TITLE_KEY);
        if (titleElement == null) {
            throw new ElementNotFoundException(contentElement, TITLE_KEY);
        }
        return titleElement.getValue();
    }

    static Integer getYear(Element contentElement) {
        Element yearElement = contentElement.getFirstChildElement(RELEASE_YEAR_KEY);
        if (yearElement == null) {
            log.info("Child node " + RELEASE_YEAR_KEY + " not found on element " + contentElement);
            return null;
        }
        return Integer.parseInt(yearElement.getValue());
    }

    static String getDescription(Element contentElement) {
        Element synopsisElement = contentElement.getFirstChildElement(LONG_SYNOPSIS_KEY);
        if (synopsisElement != null) {
            return synopsisElement.getValue();
        }
        //fall back to short_synopsis
        Element shortSynopsisElement = contentElement.getFirstChildElement(SHORT_SYNOPSIS_KEY);
        if (shortSynopsisElement == null) {
            throw new ElementNotFoundException(contentElement, SHORT_SYNOPSIS_KEY);
        }
        return shortSynopsisElement.getValue();
    }
    
    static Publisher getPublisher() {
        return Publisher.NETFLIX;
    }

    static Integer getSeriesNumber(Element contentElement) {
        Element showElement = contentElement.getFirstChildElement(SHOW_KEY);
        if (showElement == null) {
            log.info("Child node " + SHOW_KEY + " not found on element " + contentElement);
            return null;
        }
        
        Element seasonNumberElement = showElement.getFirstChildElement(SEASON_NUMBER_KEY);
        if (seasonNumberElement == null) {
            log.info("Child node " + SEASON_NUMBER_KEY + " not found on element " + showElement);
            return null;
        }
        
        return Integer.parseInt(seasonNumberElement.getValue());
    }

    static Integer getEpisodeNumber(Element contentElement) {
        Element showElement = contentElement.getFirstChildElement(SHOW_KEY);
        if (showElement == null) {
            log.info("Child node " + SHOW_KEY + " not found on element " + contentElement);
            return null;
        }
        
        Element episodeNumberElement = showElement.getFirstChildElement(EPISODE_NUMBER_KEY);
        if (episodeNumberElement == null) {
            log.info("Child node " + EPISODE_NUMBER_KEY + " not found on element " + showElement);
            return null;
        }
        
        return Integer.parseInt(episodeNumberElement.getValue());
    }

    static Set<Certificate> getCertificates(Element contentElement) {
        Element parentalAdvisories = contentElement.getFirstChildElement(PARENTAL_ADVISORIES_KEY);
        if (parentalAdvisories == null) {
            log.info("Child node " + PARENTAL_ADVISORIES_KEY + " found on element " + contentElement);
            return ImmutableSet.of();
        }
        
        Set<Certificate> certificates =  Sets.newHashSet();
        for (int i = 0; i < parentalAdvisories.getChildElements().size(); i++) {
            Element parentalAdvisory = parentalAdvisories.getChildElements().get(i);
            String advisorySystem = advisorySystem(parentalAdvisory); 
            if (advisorySystem != null && advisorySystem.equals(BBFC)) {
                certificates.add(new Certificate(parentalAdvisory.getValue(), Countries.GB));
            }
        }
        return certificates;
    }

    private static String advisorySystem(Element parentalAdvisory) {
        String advisorySystem = parentalAdvisory.getAttributeValue(SYSTEM_ATTRIBUTE);
        if (advisorySystem == null) {
            throw new AttributeNotFoundException(parentalAdvisory, SYSTEM_ATTRIBUTE);
        }
        return advisorySystem;
    }

    public static Iterable<String> getGenres(Element contentElement) {
        Element genresElement = contentElement.getFirstChildElement(GENRES_KEY);
        if (genresElement == null) {
            return ImmutableList.of();
        }
        
        Set<String> genres = Sets.newHashSet();
        for (int i = 0; i < genresElement.getChildElements().size(); i++) {
            String netflixGenre = genresElement.getChildElements().get(i).getValue();
            netflixGenre = StringEscapeUtils.unescapeHtml(netflixGenre);
            netflixGenre = CharMatcher.WHITESPACE.removeFrom(netflixGenre);
            genres.add(GENRES_URL_PREFIX + netflixGenre.toLowerCase());
        }
        return genres;
    }
    
    static List<CrewMember> getPeople(Element contentElement) {
        Element peopleElement = contentElement.getFirstChildElement(PEOPLE_KEY);
        if (peopleElement == null) {
            return ImmutableList.of();
        }
        
        List<CrewMember> people = Lists.newArrayList();
        for (int i = 0; i < peopleElement.getChildElements().size(); i++) {
            Element personElement = peopleElement.getChildElements().get(i);
            CrewMember person = new CrewMember();
            person.setCanonicalUri(PEOPLE_URL_PREFIX + getId(personElement));
            person.withRole(TYPE_ROLE_MAPPING.get(getType(personElement)));
            person.withPublisher(getPublisher());

            Element nameElement = personElement.getFirstChildElement(NAME_KEY);
            if (nameElement == null) {
                throw new ElementNotFoundException(personElement, NAME_KEY);
            }
            person.withName(personElement.getValue().trim());
            people.add(person);
        }
        return people;
    }

    private static String getType(Element contentElement) {
        String type = contentElement.getAttributeValue(TYPE_ATTRIBUTE);
        if (type == null) {
            throw new AttributeNotFoundException(contentElement, TYPE_ATTRIBUTE);
        }
        return type;
    }

    static Version getVersion(Element contentElement, int id) {
        Version version = new Version();
        version.setManifestedAs(Sets.newHashSet(getEncoding(id)));
        version.setDuration(getDuration(contentElement));
        version.setPublishedDuration((int)getDuration(contentElement).getStandardSeconds());
        return version;
    }
    
    private static Duration getDuration(Element contentElement) {
        Element durationElement = contentElement.getFirstChildElement(DURATION_KEY);
        if (durationElement == null) {
            
            throw new ElementNotFoundException(contentElement, DURATION_KEY);
        }
        
        return Duration.standardSeconds(Integer.parseInt(durationElement.getValue()));
    }

    static int getId(Element contentElement) {
        String idString = contentElement.getAttributeValue(ID_ATTRIBUTE);
        if (idString == null) {
            throw new AttributeNotFoundException(contentElement, ID_ATTRIBUTE);
        }
        return Integer.parseInt(idString);
    }

    static String getAlias(Element filmElement) {
        Element urlElement = filmElement.getFirstChildElement(URL_KEY);
        if (urlElement == null) {
            throw new ElementNotFoundException(filmElement, URL_KEY);
        }
        
        return urlElement.getValue();
    }
    
    static int getShowId(Element contentElement) {
        Element showElement = contentElement.getFirstChildElement(SHOW_KEY);
        if (showElement == null) {
            throw new ElementNotFoundException(contentElement, SHOW_KEY);
        }
        return getId(showElement);
    }
    
    private static Encoding getEncoding(int id) {
        Policy policy = new Policy();
        policy.setAvailableCountries(Sets.newHashSet(Countries.GB));
        policy.setRevenueContract(RevenueContract.SUBSCRIPTION);

        Location location = new Location();
        location.setPolicy(policy);
        location.setTransportType(TransportType.LINK);
        location.setUri(LOCATIONS_URL_PREFIX + id);
        
        Encoding encoding = new Encoding();
        encoding.setAvailableAt(Sets.newHashSet(location));
        
        return encoding;
    }
}
