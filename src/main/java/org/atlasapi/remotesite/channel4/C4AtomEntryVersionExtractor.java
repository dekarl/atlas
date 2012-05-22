package org.atlasapi.remotesite.channel4;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.media.TransportSubType;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy.Platform;
import org.atlasapi.media.entity.Restriction;
import org.atlasapi.media.entity.Version;
import org.atlasapi.remotesite.ContentExtractor;
import org.jdom.Element;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.google.common.base.Optional;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.intl.Country;
import com.metabroadcast.common.time.Clock;
import com.sun.syndication.feed.atom.Entry;

public class C4AtomEntryVersionExtractor implements ContentExtractor<Entry, Version> {
    
    private static final String DC_AGE_RATING = "dc:relation.AgeRating";
    private static final String DC_GUIDANCE = "dc:relation.Guidance";
    private static final Pattern CLIP_ID_PATTERN = Pattern.compile("tag:www\\.channel4\\.com,\\d+:clip\\/(.+)");
    private static final String EMBED_CODE = "<object id='flashObj' width='480' height='290' classid='clsid:D27CDB6E-AE6D-11cf-96B8-444553540000' codebase='http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=9,0,47,0'><param name='movie' value='http://c.brightcove.com/services/viewer/federated_f9/86700592001?isVid=1&amp;isUI=1&amp;publisherID=1213940598' /><param name='bgcolor' value='#000000' /><param name='flashVars' value='videoId=%VIDEOID%&amp;playerID=86700592001&amp;domain=embed&amp;' /><param name='base' value='http://admin.brightcove.com' /><param name='seamlesstabbing' value='false' /><param name='allowFullScreen' value='true' /><param name='swLiveConnect' value='true' /><param name='allowScriptAccess' value='always' /><embed src='http://c.brightcove.com/services/viewer/federated_f9/86700592001?isVid=1&amp;isUI=1&amp;publisherID=1213940598' bgcolor='#000000' flashVars='videoId=%VIDEOID%&amp;playerID=86700592001&amp;domain=embed&amp;' base='http://admin.brightcove.com' name='flashObj' width='480' height='290' seamlesstabbing='false' type='application/x-shockwave-flash' allowFullScreen='true' swLiveConnect='true' allowScriptAccess='always' pluginspage='http://www.macromedia.com/shockwave/download/index.cgi?P1_Prod_Version=ShockwaveFlash'></embed></object>";
    
    private final Clock clock;
    private final Optional<Platform> platform;

    public C4AtomEntryVersionExtractor(Optional<Platform> platform, Clock clock) {
        this.clock = clock;
        this.platform = platform;
    }
    
    @Override
    public Version extract(Entry entry) {
        Element mediaGroup = C4AtomApi.mediaGroup(entry);

        Set<Country> availableCountries = null;
        if (mediaGroup != null) {
            Element restriction = mediaGroup.getChild("restriction", C4AtomApi.NS_MEDIA_RSS);
            if (restriction != null && restriction.getValue() != null) {
                availableCountries = Countries.fromDelimtedList(restriction.getValue());
            }
        }
        
        String uri = C4AtomApi.fourOdUri(entry);
        if (uri == null) {
            uri = C4AtomApi.clipUri(entry);
        }
        return version(uri, entry.getId(), C4AtomApi.foreignElementLookup(entry), availableCountries);
    }
    
    private Version version(String uri, String locationId, Map<String, String> lookup, Set<Country> availableCountries) {
        DateTime lastUpdated = clock.now();

        Version version = new Version();
        version.setLastUpdated(lastUpdated);
        Duration duration = C4AtomApi.durationFrom(lookup);
        
        if (duration != null) {
            version.setDuration(duration);
        }
        
        Integer ageRating = lookup.get(DC_AGE_RATING) != null ? Integer.parseInt(lookup.get(DC_AGE_RATING)) : null;
        String guidance = lookup.get(DC_GUIDANCE);

        Restriction restriction = null;
        if (ageRating != null && ageRating > 0 && guidance != null) {
            restriction = Restriction.from(ageRating, guidance);
        } else {
            restriction = Restriction.from(guidance);
        }
        if (restriction != null) {
            restriction.setLastUpdated(lastUpdated);
            version.setRestriction(restriction);
        }
        
        Encoding encoding = new Encoding();
        Location location = C4AtomApi.locationFrom(uri, locationId, lookup, availableCountries, platform);
        location.setLastUpdated(lastUpdated);
        if (location.getPolicy() != null) {
            location.getPolicy().setLastUpdated(lastUpdated);
        }
        
        encoding.addAvailableAt(location);
        
        Matcher matcher = CLIP_ID_PATTERN.matcher(locationId);
        if (matcher.matches()) {
            Location embedLocation = embedLocation(matcher.group(1), location);
            encoding.addAvailableAt(embedLocation);
        }
        
        version.addManifestedAs(encoding);
                
        return version.getBroadcasts().isEmpty() && version.getManifestedAs().isEmpty() ? null : version;
    }

    private Location embedLocation(String embedId, Location linkLocation) {
        Location location = new Location();
        location.setTransportType(TransportType.EMBED);
        location.setTransportSubType(TransportSubType.BRIGHTCOVE);
        location.setLastUpdated(linkLocation.getLastUpdated());
        location.setEmbedId(embedId);
        location.setEmbedCode(EMBED_CODE.replaceAll("%VIDEOID%", embedId));
        
        // The feed only contains available content
        location.setAvailable(linkLocation.getAvailable());
        if (linkLocation.getPolicy() != null) {
            location.setPolicy(linkLocation.getPolicy().copy());
        }

        return location;
    }
}
