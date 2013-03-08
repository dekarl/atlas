package org.atlasapi.remotesite.pa.channels;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.Platform;
import org.atlasapi.media.channel.Region;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.remotesite.pa.PaChannelMap;
import org.atlasapi.remotesite.pa.channels.bindings.EpgContent;
import org.atlasapi.remotesite.pa.channels.bindings.Name;
import org.atlasapi.remotesite.pa.channels.bindings.Regionalisation;
import org.atlasapi.remotesite.pa.channels.bindings.RegionalisationList;
import org.atlasapi.remotesite.pa.channels.bindings.ServiceProvider;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.intl.Country;

public class PaChannelGroupsIngester {

    private static final String PLATFORM_ALIAS_PREFIX = "http://pressassociation.com/platforms/";
    private static final String REGION_ALIAS_PREFIX = "http://pressassociation.com/regions/";
    private static final String PLATFORM_PREFIX = "http://ref.atlasapi.org/platforms/pressassociation.com/";
    private static final String REGION_PREFIX = "http://ref.atlasapi.org/regions/pressassociation.com/";
    

    private final DateTimeFormatter formatter = ISODateTimeFormat.date();
    private final Logger log = LoggerFactory.getLogger(PaChannelGroupsIngester.class);
          
    public ChannelGroupTree processPlatform(org.atlasapi.remotesite.pa.channels.bindings.Platform paPlatform, List<ServiceProvider> serviceProviders, List<org.atlasapi.remotesite.pa.channels.bindings.Region> paRegions) {
       
        ServiceProvider serviceProvider = getServiceProvider(paPlatform.getServiceProviderId(), serviceProviders);
        if (serviceProvider == null) {
            log.error("ServiceProvider with id " + paPlatform.getServiceProviderId() + " not found in the channel data file");
            return new ChannelGroupTree(null, ImmutableMap.<String, Region>of());
        }
        
        Platform platform = processBasicPlatform(paPlatform);

        if (serviceProvider.getRegionalisationList() == null || serviceProvider.getRegionalisationList().getRegionalisation().isEmpty()) {
            return new ChannelGroupTree(platform, ImmutableMap.<String, Region>of());
        } else {
            return new ChannelGroupTree(platform, createRegionsForPlatform(serviceProvider.getRegionalisationList().getRegionalisation(), paRegions, paPlatform, platform.getAvailableCountries()));
        }
    }
    
    public void addChannelNumberings(List<EpgContent> epgContents, Map<String, Region> regions, Map<String, Channel> channelMap) {
        for (EpgContent epgContent : epgContents) {
            Channel channel = channelMap.get(PaChannelMap.createUriFromId(epgContent.getChannelId()));
            if (channel == null) {
                throw new RuntimeException("PA Channel with id " + epgContent.getChannelId() + " not found");
            }

            LocalDate startDate = formatter.parseLocalDate(epgContent.getStartDate());
            LocalDate endDate;
            // add a day, due to PA considering a date range to run from the start of startDate to the end of endDate,
            // whereas we consider a range to run from the start of startDate to the start of endDate
            if (epgContent.getEndDate() != null) {
                endDate = formatter.parseLocalDate(epgContent.getEndDate());
                endDate.plusDays(1);
            } else {
                endDate = null;
            }

            RegionalisationList regionalisationList = epgContent.getRegionalisationList();
            if (regionalisationList == null) {
                // add to all regions
                for (Region region : regions.values()) {
                    channel.addChannelNumber(region, epgContent.getChannelNumber(), startDate, endDate);
                }
            } else {
                // add to selected regions
                for (Regionalisation epgRegion : regionalisationList.getRegionalisation()) {
                    channel.addChannelNumber(regions.get(epgRegion.getRegionId()), epgContent.getChannelNumber(), startDate, endDate);
                }
            }
        }
    }

    private Map<String, Region> createRegionsForPlatform(List<Regionalisation> regionalisations, List<org.atlasapi.remotesite.pa.channels.bindings.Region> paRegions, org.atlasapi.remotesite.pa.channels.bindings.Platform paPlatform, Set<Country> countries) {
        Map<String, Region> regions = Maps.newHashMap();
        // If there are regions, create/update the regions as appropriate and then add the regions to the platform.
        for (Regionalisation regionalisation : regionalisations) {
            Region region = new Region();            
            for (org.atlasapi.remotesite.pa.channels.bindings.Region paRegion : paRegions) {
                if (paRegion.getId().equals(regionalisation.getRegionId())) {
                    for (Name name : paRegion.getNames().getName()) {
                        LocalDate titleStartDate = formatter.parseLocalDate(name.getStartDate());
                        if (name.getEndDate() != null) {
                            LocalDate titleEndDate = formatter.parseLocalDate(name.getEndDate());
                            region.addTitle(name.getvalue(), titleStartDate, titleEndDate.plusDays(1));
                        } else {
                            region.addTitle(name.getvalue(), titleStartDate);
                        }
                    }
                    break;
                }
            }
            
            region.setCanonicalUri(REGION_PREFIX + paPlatform.getId() + "-" + regionalisation.getRegionId());
            region.addAliasUrl(REGION_ALIAS_PREFIX + paPlatform.getId() + "-" + regionalisation.getRegionId());
            region.setPublisher(Publisher.METABROADCAST);
            region.setAvailableCountries(countries);            
            regions.put(regionalisation.getRegionId(), region);
        }
        return regions;
    }

    private Platform processBasicPlatform(org.atlasapi.remotesite.pa.channels.bindings.Platform paPlatform) {

        Platform platform = new Platform();
        platform.setCanonicalUri(PLATFORM_PREFIX + paPlatform.getId());
        platform.addAliasUrl(PLATFORM_ALIAS_PREFIX + paPlatform.getId());
        platform.setPublisher(Publisher.METABROADCAST);
        
        if (paPlatform.getCountries() != null) {
            for (org.atlasapi.remotesite.pa.channels.bindings.Country country : paPlatform.getCountries().getCountry()) {
                platform.addAvailableCountry(Countries.fromCode(country.getCode()));
            }
        }
        
        for (Name name : paPlatform.getNames().getName()) {
            LocalDate titleStartDate = formatter.parseLocalDate(name.getStartDate());
            if (name.getEndDate() != null) {
                LocalDate titleEndDate = formatter.parseLocalDate(name.getEndDate());
                platform.addTitle(name.getvalue(), titleStartDate, titleEndDate.plusDays(1));
            } else {
                platform.addTitle(name.getvalue(), titleStartDate);
            }
        }
        
        return platform;
    }

    public void addChannelsToPlatform(Platform platform, List<EpgContent> epgContents, Map<String,Channel> channelMap) {
        for (EpgContent epgContent : epgContents) {
            Channel channel = channelMap.get(PaChannelMap.createUriFromId(epgContent.getChannelId()));
            if (channel == null) {
                throw new RuntimeException("PA Channel with id " + epgContent.getChannelId() + " not found");
            }
            LocalDate startDate = formatter.parseLocalDate(epgContent.getStartDate());
            LocalDate endDate;
            // add a day, due to PA considering a date range to run from the start of startDate to the end of endDate,
            // whereas we consider a range to run from the start of startDate to the start of endDate
            if (epgContent.getEndDate() != null) {
                endDate = formatter.parseLocalDate(epgContent.getEndDate());
                endDate.plusDays(1);
            } else {
                endDate = null;
            }
            
            channel.addChannelNumber(platform, epgContent.getChannelNumber(), startDate, endDate);
        }
    }

    public static ServiceProvider getServiceProvider(String serviceProviderId, List<ServiceProvider> serviceProviders) {
        for (ServiceProvider provider : serviceProviders) {
            if (provider.getId().equals(serviceProviderId)) {
                return provider;
            }
        }
        return null;
    }
}
