package org.atlasapi.remotesite.five;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import nu.xom.Element;
import nu.xom.Elements;

import org.atlasapi.genres.GenreMap;
import org.atlasapi.media.TransportSubType;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Policy.RevenueContract;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.system.RemoteSiteClient;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.intl.Countries;

public class FiveEpisodeProcessor {

    private static final Logger log = LoggerFactory.getLogger(FiveEpisodeProcessor.class);

    private final GenreMap genreMap = new FiveGenreMap();
    private final DateTimeFormatter dateParser = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");

    private final String baseApiUrl;
    private final RemoteSiteClient<HttpResponse> httpClient;
    private final Multimap<String, Channel> channelMap;
    private final Map<String, Series> seriesMap = Maps.newHashMap();

    public FiveEpisodeProcessor(String baseApiUrl, RemoteSiteClient<HttpResponse> httpClient, Multimap<String, Channel> channelMap) {
        this.baseApiUrl = baseApiUrl;
        this.httpClient = httpClient;
        this.channelMap = channelMap;
    }
    
    public Item processEpisode(Element element, Brand brand) throws Exception {
        
        String id = childValue(element, "id");

        Item item;
        if (brand.getSpecialization() == Specialization.FILM) {
            item = new Film(getEpisodeUri(id), getEpisodeCurie(id), Publisher.FIVE);
            item.setMediaType(MediaType.VIDEO);
            item.setSpecialization(Specialization.FILM);
        } else {
            Episode episode = new Episode(getEpisodeUri(id), getEpisodeCurie(id), Publisher.FIVE);
            episode.setMediaType(MediaType.VIDEO);
            episode.setSpecialization(Specialization.TV);
            
            String episodeNumber = childValue(element, "episode_number");
            if (!Strings.isNullOrEmpty(episodeNumber)) {
                episode.setEpisodeNumber(Integer.valueOf(episodeNumber));
            }
            processSeries(episode, element, brand);
            item = episode;
        }
        
        Maybe<String> description = getDescription(element);
        if (description.hasValue()) {
            item.setDescription(description.requireValue());
        }

        item.setGenres(getGenres(element));
        Maybe<String> image = getImage(element);
        if (image.hasValue()) {
            item.setImage(image.requireValue());
        }
        
        Version version = getVersion(element);
        
        item.setTitle(childValue(element, "title"));
        
        item.addVersion(version);
        
        return item;
    }

    private Version getVersion(Element element) throws Exception {
        Version version = new Version();
        version.setDuration(Duration.standardSeconds(Long.parseLong(childValue(element, "duration"))));
        version.setProvider(Publisher.FIVE);
        
        Encoding encoding = new Encoding();

        Location location = getLocation(element);
        encoding.addAvailableAt(location);
        version.addManifestedAs(encoding);

        version.setBroadcasts(getBroadcasts(element));
        return version;
    }
    
    private Set<Broadcast> getBroadcasts(Element element) {
        Elements transmissionElements = element.getFirstChildElement("transmissions").getChildElements("transmission");
        
        Set<Broadcast> broadcasts = Sets.newHashSet();
        for (int i = 0; i < transmissionElements.size(); i++) {
            broadcasts.addAll(createBroadcasts((Element) transmissionElements.get(i)));
        }
        return broadcasts;
    }

    private Location getLocation(Element element) throws Exception {
        
        String originalVodUri = childValue(element, "vod_url").trim();
        
        Location location = new Location();
        location.setUri(getLocationUri(originalVodUri));
        location.setTransportType(TransportType.LINK);
        location.setTransportSubType(TransportSubType.HTTP);

        Policy policy = new Policy();
        policy.setRevenueContract(RevenueContract.FREE_TO_VIEW);
        policy.setAvailableCountries(ImmutableSet.of(Countries.GB));

        String availabilityStart = childValue(element, "vod_start");
        if (Strings.isNullOrEmpty(availabilityStart)) {
            location.setAvailable(false);
        } else {
            location.setAvailable(true);
            policy.setAvailabilityStart(dateParser.parseDateTime(availabilityStart));

            String availabilityEnd = childValue(element, "vod_end");
            if (!Strings.isNullOrEmpty(availabilityEnd)) {
                policy.setAvailabilityEnd(dateParser.parseDateTime(availabilityEnd));
            }
        }
        
        String scheduledAvailabilityStart = childValue(element, "scheduled_vod_start");
        String scheduledAvailabilityEnd = childValue(element, "scheduled_vod_end");
        
        if (!Strings.isNullOrEmpty(scheduledAvailabilityStart)
                && policy.getAvailabilityStart() == null) {
            policy.setAvailabilityStart(dateParser.parseDateTime(scheduledAvailabilityStart));
        }
        
        if (!Strings.isNullOrEmpty(scheduledAvailabilityEnd)
                && policy.getAvailabilityEnd() == null) {
            policy.setAvailabilityEnd(dateParser.parseDateTime(scheduledAvailabilityEnd));
        }
        
        location.setPolicy(policy);
        return location;
    }
    

    private void processSeries(Episode episode, Element element, Brand brand) {
        Element seasonLinkElement = element.getFirstChildElement("season_link");
        if (seasonLinkElement != null) {
            Element seasonElement = seasonLinkElement.getFirstChildElement("season");
            String id = childValue(seasonElement, "id");
            Series series = getSeriesMap().get(id);
            if (series == null){ 
                series = new Series(seasonLinkElement.getAttributeValue("href"), getSeriesCurie(id), Publisher.FIVE);
                series.setParent(brand);
                series.setGenres(genreMap.mapRecognised(ImmutableSet.of(childValue(seasonElement, "genre"))));
                
                Maybe<String> image = getImage(seasonElement);
                if (image.hasValue()) {
                    series.setImage(image.requireValue());
                }
                
                Maybe<String> description = getDescription(seasonElement);
                if (description.hasValue()) {
                    series.setDescription(description.requireValue());
                }
                
                String title = childValue(seasonElement, "title");
                if (title != null) {
                    try {
                        series.withSeriesNumber(Integer.valueOf(title));
                    }
                    catch (NumberFormatException e) {
                        // ignore if the title is not a series number
                    }
                    finally {
                        series.setTitle(title);
                    }
                }

                getSeriesMap().put(id, series);
            }
            
            episode.setSeries(series);
            episode.setSeriesNumber(series.getSeriesNumber());
        }
    }

    private Set<Broadcast> createBroadcasts(Element element) {

        String channelUri = element.getFirstChildElement("channel_link").getAttributeValue("href");
        Collection<Channel> channels = channelMap.get(channelUri);

        if (channels.isEmpty()) {
            log.warn("No channels for " + channelUri);
        }
        
        ImmutableSet.Builder<Broadcast> broadcasts = ImmutableSet.builder(); 
        for (Channel channel : channels) {
            broadcasts.add(new Broadcast(
                channel.getUri(), 
                dateParser.parseDateTime(childValue(element, "transmission_start")), 
                dateParser.parseDateTime(childValue(element, "transmission_end"))
            ));
        }
        
        return broadcasts.build();
    }

    private String getEpisodeUri(String id) {
        
        return baseApiUrl + "/watchables/" + id;
    }

    private String getLocationUri(String originalUri) throws Exception {

        HttpResponse httpResponse = httpClient.get(originalUri);

        return httpResponse.finalUrl();
    }

    private Set<String> getGenres(Element element) {
        
        return genreMap.mapRecognised(ImmutableSet.of("http://www.five.tv/genres/" + element.getFirstChildElement("genre").getValue()));
    }

    private Maybe<String> getImage(Element element) {
        
        Elements imageElements = element.getFirstChildElement("images").getChildElements("image");
        if (imageElements.size() > 0) {
            return Maybe.just(imageElements.get(0).getValue());
        }

        return Maybe.nothing();
    }

    private Maybe<String> getDescription(Element element) {
        
        Element longDescriptionElement = element.getFirstChildElement("long_description");
        if (longDescriptionElement != null) {
            String description = longDescriptionElement.getValue();
        
            if (!Strings.isNullOrEmpty(description)) {
                return Maybe.just(description.trim());
            }
        }

        Element mediumDescriptionElement = element.getFirstChildElement("medium_description");
        if (mediumDescriptionElement != null) {
            String description = mediumDescriptionElement.getValue();
        
            if (!Strings.isNullOrEmpty(description)) {
                return Maybe.just(description.trim());
            }
        }

        Element shortDescriptionElement = element.getFirstChildElement("short_description");
        if (shortDescriptionElement != null) {
            String description = shortDescriptionElement.getValue();
        
            if (!Strings.isNullOrEmpty(description)) {
                return Maybe.just(description.trim());
            }
        }

        return Maybe.nothing();
    }
    
    private String getSeriesCurie(String id) {
        return "five:s-" + id;
    }

    private String getEpisodeCurie(String id) {
        
        return "five:e-" + id;
    }

    private String childValue(Element element, String childName) {
        
        Element child = element.getFirstChildElement(childName);
        if (child != null) {
            return child.getValue();
        }
        
        return null;
    }

    public Map<String, Series> getSeriesMap() {
        return seriesMap;
    }
}
