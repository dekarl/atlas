package org.atlasapi.remotesite.youview;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;

import org.apache.commons.lang.StringEscapeUtils;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Policy.Platform;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.atlasapi.remotesite.AttributeNotFoundException;
import org.atlasapi.remotesite.ContentExtractor;
import org.atlasapi.remotesite.netflix.ElementNotFoundException;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Optional;
import com.metabroadcast.common.intl.Countries;

public class YouViewContentExtractor implements ContentExtractor<Element, Item> {

    private static final String ATOM_PREFIX = "atom";
    private static final String YV_PREFIX = "yv";
    private static final String MEDIA_PREFIX = "media";
    private static final String ID_KEY = "id";
    private static final String TITLE_KEY = "title";
    private static final String PROGRAMME_ID_KEY = "programmeId";
    private static final String IDENTIFIER_KEY = "identifier";
    private static final String PROGRAMME_CRID_KEY = "programmeCRID";
    private static final String SERVICE_ID_KEY = "serviceId";
    private static final String EVENT_LOCATOR_KEY = "eventLocator";
    private static final String MEDIA_CONTENT_KEY = "content";
    private static final String DURATION_KEY = "duration";
    private static final String YOUVIEW_PREFIX = "youview:";
    private static final String YOUVIEW_URI_PREFIX = "http://youview.com/programme/";
    private static final String SCHEDULE_SLOT_KEY = "scheduleSlot";
    private static final String AVAILABLE_KEY = "available";
    private static final String START_KEY = "start";
    private static final String END_KEY = "end";
    private static final String SCHEDULE_EVENT_PREFIX = "http://youview.com/scheduleevent/";

    private final YouViewChannelResolver channelResolver;
    
    private final DateTimeFormatter dateFormatter = ISODateTimeFormat.dateTimeNoMillis();
    
    public YouViewContentExtractor(YouViewChannelResolver channelResolver) {
        this.channelResolver = channelResolver;
    }
    
    @Override
    public Item extract(Element source) {
        
        Item item = new Item();

        String id = getId(source);
        item.setCanonicalUri(SCHEDULE_EVENT_PREFIX + id);
        item.addAlias(new Alias("youview:scheduleevent", id));
        item.setTitle(getTitle(source));
        item.setMediaType(getMediaType(source));
        item.setPublisher(Publisher.YOUVIEW);

        Optional<String> programmeId = getProgrammeId(source);
        if (programmeId.isPresent()) {
            item.addAliasUrl(YOUVIEW_URI_PREFIX + programmeId.get());
            item.addAlias(new Alias("youview:programme", programmeId.get()));
        }
        Optional<String> programmeCrid = getProgrammeCrid(source);
        if (programmeCrid.isPresent()) {
            item.addAliasUrl(programmeCrid.get());
            item.addAlias(new Alias("dvb:pcrid", programmeCrid.get()));
        }
        item.addVersion(getVersion(source));
        return item;
    }
    
    private Optional<Location> getLocation(Element source) {

        Element available = source.getFirstChildElement(AVAILABLE_KEY, source.getNamespaceURI(YV_PREFIX));
        if (available == null) {
            return Optional.absent();
        }
        
        Attribute start = available.getAttribute(START_KEY);
        Attribute end = available.getAttribute(END_KEY);
        if (start == null || end == null) {
            return Optional.absent();
        }
        
        Policy policy = new Policy();
        policy.setPlatform(Platform.YOUVIEW);
        policy.addAvailableCountry(Countries.GB);
        policy.setAvailabilityStart(dateFormatter.parseDateTime(start.getValue()));
        policy.setAvailabilityEnd(dateFormatter.parseDateTime(end.getValue()));

        Location location = new Location();
        location.setPolicy(policy);
        
        return Optional.of(location);
    }

    private Broadcast getBroadcast(Element source) {
        String id = getBroadcastId(source);
        String broadcastOn = getBroadcastOn(source);
        String eventLocator = getEventLocator(source);
        DateTime transmissionTime = getTransmissionTime(source);
        Duration broadcastDuration = getBroadcastDuration(source);
        
        Broadcast broadcast = new Broadcast(broadcastOn, transmissionTime, transmissionTime.plus(broadcastDuration));
        broadcast.withId(id);
        broadcast.addAliasUrl(eventLocator);
        broadcast.addAlias(new Alias("dvb:event-locator", eventLocator));
        
        return broadcast;
    }

    private String getBroadcastId(Element source) {
        Element broadcastId = source.getFirstChildElement(ID_KEY, source.getNamespaceURI(ATOM_PREFIX));
        if (broadcastId == null) {
            throw new ElementNotFoundException(source, ATOM_PREFIX + ":" + ID_KEY);
        }
        return YOUVIEW_PREFIX + broadcastId.getValue();
    }

    private String getBroadcastOn(Element source) {
        Element serviceId = source.getFirstChildElement(SERVICE_ID_KEY, source.getNamespaceURI(YV_PREFIX));
        if (serviceId == null) {
            throw new ElementNotFoundException(source, YV_PREFIX + ":" + SERVICE_ID_KEY);
        }
        return channelResolver.getChannelUri(Integer.parseInt(serviceId.getValue()));
    }

    private Duration getBroadcastDuration(Element source) {
        Element mediaContent = source.getFirstChildElement(MEDIA_CONTENT_KEY, source.getNamespaceURI(MEDIA_PREFIX));
        if (mediaContent == null) {
            throw new ElementNotFoundException(source, MEDIA_PREFIX + ":" + MEDIA_CONTENT_KEY);
        }
        Attribute duration = mediaContent.getAttribute(DURATION_KEY);
        if (duration == null) {
            throw new AttributeNotFoundException(mediaContent, DURATION_KEY);
        }
        return Duration.standardSeconds(Integer.parseInt(duration.getValue()));
    }

    private String getEventLocator(Element source) {
        Element eventLocator = getElementOfType(source, IDENTIFIER_KEY, YV_PREFIX, EVENT_LOCATOR_KEY);
        if (eventLocator == null) {
            throw new ElementNotFoundException(source, YV_PREFIX + ":" + IDENTIFIER_KEY + " with type: " + EVENT_LOCATOR_KEY);
        }
        return eventLocator.getValue();
    }

    private DateTime getTransmissionTime(Element source) {
        Element transmissionTime = source.getFirstChildElement(SCHEDULE_SLOT_KEY, source.getNamespaceURI(YV_PREFIX));
        if (transmissionTime == null) {
            throw new ElementNotFoundException(source, YV_PREFIX + ":" + SCHEDULE_SLOT_KEY);
        }
        return dateFormatter.parseDateTime(transmissionTime.getValue());
    }

    private Version getVersion(Element source) {
        Optional<Location> location = getLocation(source);
        
        Version version = new Version();
        version.setDuration(getBroadcastDuration(source));
        version.setPublishedDuration(version.getDuration());
        version.addBroadcast(getBroadcast(source));
        if (location.isPresent()) {
            Encoding encoding = new Encoding();
            encoding.addAvailableAt(location.get());
            version.addManifestedAs(encoding);
        }
        return version;
    }

    private Optional<String> getProgrammeCrid(Element source) {
        Element programmeCrid = getElementOfType(source, IDENTIFIER_KEY, YV_PREFIX, PROGRAMME_CRID_KEY);
        if (programmeCrid == null) {
            return Optional.absent();
        }
        return Optional.fromNullable(programmeCrid.getValue());
    }

    private Element getElementOfType(Element source, String elementName, String prefixName, String elementType) {
        Elements elements = source.getChildElements(elementName, source.getNamespaceURI(prefixName));
        for (int i = 0; i < elements.size(); i++) {
            Attribute typeAttr = elements.get(i).getAttribute("type");
            if (typeAttr != null && typeAttr.getValue().contains(elementType)) {
                return elements.get(i);
            }
        }
        return null;
    }

    private Optional<String> getProgrammeId(Element source) {
        Element programmeId = source.getFirstChildElement(PROGRAMME_ID_KEY, source.getNamespaceURI(YV_PREFIX));
        if (programmeId == null) {
            return Optional.absent();
        }
        return Optional.fromNullable(programmeId.getValue());
    }

    private MediaType getMediaType(Element source) {
        Element serviceId = source.getFirstChildElement(SERVICE_ID_KEY, source.getNamespaceURI(YV_PREFIX));
        if (serviceId == null) {
            throw new ElementNotFoundException(source, YV_PREFIX + ":" + SERVICE_ID_KEY);
        }
        Optional<Channel> channel = channelResolver.getChannel(Integer.parseInt(serviceId.getValue()));
        if (!channel.isPresent()) {
            throw new RuntimeException("Channel with YouView Id: " + serviceId.getValue() + " not found");
        }
        return channel.get().mediaType();
    }

    private String getTitle(Element source) {
        Element atomTitle = source.getFirstChildElement(TITLE_KEY, source.getNamespaceURI(ATOM_PREFIX));
        if (atomTitle == null) {
            throw new ElementNotFoundException(source, ATOM_PREFIX + ":" + TITLE_KEY);
        }
        return StringEscapeUtils.unescapeHtml(atomTitle.getValue());
    }

    private String getId(Element source) {
        Element atomId = source.getFirstChildElement(ID_KEY, source.getNamespaceURI(ATOM_PREFIX));
        if (atomId == null) {
            throw new ElementNotFoundException(source, ATOM_PREFIX + ":" + ID_KEY);
        }
        return atomId.getValue();
    } 
}
