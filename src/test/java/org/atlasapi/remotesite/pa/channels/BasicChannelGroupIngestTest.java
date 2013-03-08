package org.atlasapi.remotesite.pa.channels;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.atlasapi.media.channel.Platform;
import org.atlasapi.media.channel.Region;
import org.atlasapi.media.channel.TemporalString;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.remotesite.pa.channels.bindings.Epg;
import org.atlasapi.remotesite.pa.channels.bindings.EpgContent;
import org.atlasapi.remotesite.pa.channels.bindings.Name;
import org.atlasapi.remotesite.pa.channels.bindings.Names;
import org.atlasapi.remotesite.pa.channels.bindings.Regionalisation;
import org.atlasapi.remotesite.pa.channels.bindings.RegionalisationList;
import org.atlasapi.remotesite.pa.channels.bindings.ServiceProvider;
import org.joda.time.LocalDate;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.metabroadcast.common.intl.Countries;

public class BasicChannelGroupIngestTest {

    private final PaChannelGroupsIngester ingester = new PaChannelGroupsIngester();
    
    @Test
    public void testBasicPlatformIngest() {
        PlatformInfo platformInfo = new PlatformInfo();
        platformInfo.setName("Freeview", "2011-09-28");
        platformInfo.setId("3");
        platformInfo.setServiceProviderId("2");
        platformInfo.setCountries(ImmutableList.of(Countries.GB.code()))
;        // create serviceProvider with no regions
        ServiceProviderInfo serviceProvider = new ServiceProviderInfo();
        serviceProvider.setId("2");
        serviceProvider.setRegions(Lists.<RegionalisationInfo>newArrayList());

        ChannelGroupTree result = ingester.processPlatform(platformInfo.createPlatform(), ImmutableList.of(serviceProvider.createServiceProvider()), ImmutableList.<org.atlasapi.remotesite.pa.channels.bindings.Region>of());
        Platform platform = result.getPlatform();
        
        // test that platform fields are picked up ok
        assertEquals("Freeview", platform.getTitle());
        assertEquals(new TemporalString("Freeview", new LocalDate(2011, 9, 28), null), Iterables.getOnlyElement(platform.getAllTitles()));
        assertEquals("http://ref.atlasapi.org/platforms/pressassociation.com/3", platform.getCanonicalUri());
        assertEquals("http://pressassociation.com/platforms/3", Iterables.getOnlyElement(platform.getAliasUrls()));
        assertEquals(Publisher.METABROADCAST, platform.getPublisher());
        assertEquals(ImmutableSet.of(Countries.GB), platform.getAvailableCountries());
    }
    
    @Test
    public void testBasicRegionIngest() {
        // create serviceProvider with a region
        RegionalisationInfo regionalisation = new RegionalisationInfo();
        regionalisation.setRegionId("61");
        
        ServiceProviderInfo serviceProvider = new ServiceProviderInfo();
        serviceProvider.setId("2");
        serviceProvider.setRegions(Lists.newArrayList(regionalisation));
        
        // create regions
        RegionInfo south = new RegionInfo();
        south.setId("61");
        south.setName("South", "2009-01-28");
        
        RegionInfo yorkshire = new RegionInfo();
        yorkshire.setId("67");
        yorkshire.setName("Yorkshire", "2007-06-10");
        
        PlatformInfo platformInfo = new PlatformInfo();
        platformInfo.setName("Freeview", "2011-09-28");
        platformInfo.setId("3");
        platformInfo.setServiceProviderId("2");
        platformInfo.setCountries(ImmutableList.of(Countries.GB.code()));
        
        ChannelGroupTree tree = ingester.processPlatform(platformInfo.createPlatform(), ImmutableList.of(serviceProvider.createServiceProvider()), ImmutableList.of(south.createRegion(), yorkshire.createRegion()));
        Map<String, Region> regions = tree.getRegions();
        
        Region region = Iterables.getOnlyElement(regions.values());
        
        assertEquals("South", region.getTitle());
        assertEquals(new TemporalString("South", new LocalDate(2009, 1, 28), null), Iterables.getOnlyElement(region.getAllTitles()));
        assertEquals("http://ref.atlasapi.org/regions/pressassociation.com/3-61", region.getCanonicalUri());
        assertEquals("http://pressassociation.com/regions/3-61", Iterables.getOnlyElement(region.getAliasUrls()));
        assertEquals(Publisher.METABROADCAST, region.getPublisher());
        assertEquals(ImmutableSet.of(Countries.GB), region.getAvailableCountries());
    }
}

class PlatformInfo {
    private String name;
    private String nameStartDate;
    private String id;
    private List<EpgContentInfo> epgContents = Lists.newArrayList();
    private String serviceProviderId;
    private List<String> countries = Lists.newArrayList();
    
    public void setName(String name, String startDate) {
        this.name = name;
        this.nameStartDate = startDate;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public void setEpgContents(List<EpgContentInfo> epgContents) {
        this.epgContents = epgContents;
    }
    
    public void setServiceProviderId(String id) {
        this.serviceProviderId = id;
    }
    
    public void setCountries(List<String> countries) {
        this.countries = countries;
    }
    
    public org.atlasapi.remotesite.pa.channels.bindings.Platform createPlatform() {
        org.atlasapi.remotesite.pa.channels.bindings.Platform platform = new org.atlasapi.remotesite.pa.channels.bindings.Platform();
        
        Names paPlatformNames = new Names();
        Name paPlatformName = new Name();
        paPlatformName.setvalue(name);
        paPlatformName.setStartDate(nameStartDate);
        paPlatformNames.getName().add(paPlatformName);
        platform.setNames(paPlatformNames);
        
        Epg epg = new Epg();
        for (EpgContentInfo epgContent : epgContents) {
            epg.getEpgContent().add(epgContent.createEpgContent());
        }
        platform.setEpg(epg);
        
        org.atlasapi.remotesite.pa.channels.bindings.Countries paCountries = new org.atlasapi.remotesite.pa.channels.bindings.Countries();
        for (String country : countries) {
            org.atlasapi.remotesite.pa.channels.bindings.Country paCountry = new org.atlasapi.remotesite.pa.channels.bindings.Country();
            paCountry.setCode(country);
            paCountries.getCountry().add(paCountry);
        }
        platform.setCountries(paCountries);
        
        platform.setId(id);
        platform.setServiceProviderId(serviceProviderId);
        
        return platform;
    }
}

class ServiceProviderInfo {
    private List<RegionalisationInfo> regions = Lists.newArrayList();
    private String id;
    private String name;
    private String nameStartDate;

    public void setRegions(List<RegionalisationInfo> regions) {
        this.regions = regions;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public void setName(String name, String nameStartDate) {
        this.name = name;
        this.nameStartDate = nameStartDate;
    }
    
    public ServiceProvider createServiceProvider() {
        ServiceProvider serviceProvider = new ServiceProvider();
        
        RegionalisationList regionalisationList = new RegionalisationList();
        for (RegionalisationInfo regionalisation : regions) {
            regionalisationList.getRegionalisation().add(regionalisation.createRegionalisation());                
        }
        serviceProvider.setRegionalisationList(regionalisationList);
        
        Names paSPNames = new Names();
        Name paSPName = new Name();
        paSPName.setvalue(name);
        paSPName.setStartDate(nameStartDate);
        paSPNames.getName().add(paSPName);
        serviceProvider.setNames(paSPNames);
        
        serviceProvider.setId(id);
        
        return serviceProvider;
    }
}

class RegionalisationInfo {
    private String regionId;
    
    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }
    
    public Regionalisation createRegionalisation() {
        Regionalisation regionalisation = new Regionalisation();
        
        regionalisation.setRegionId(regionId);
        
        return regionalisation;
    }
}

class RegionInfo {
    private String name;
    private String nameStartDate;
    private String id;
    
    public void setId(String id) {
        this.id = id;
    }
    
    public void setName(String name, String nameStartDate) {
        this.name = name;
        this.nameStartDate = nameStartDate;
    }
    
    public org.atlasapi.remotesite.pa.channels.bindings.Region createRegion() {
        org.atlasapi.remotesite.pa.channels.bindings.Region region = new org.atlasapi.remotesite.pa.channels.bindings.Region();
        
        Names paRegionNames = new Names();
        Name paRegionName = new Name();
        paRegionName.setvalue(name);
        paRegionName.setStartDate(nameStartDate);
        paRegionNames.getName().add(paRegionName);
        region.setNames(paRegionNames);
        
        region.setId(id);
        
        return region;
    }
}

class EpgContentInfo {
    private String channelId;
    private String channelNumber;
    private List<RegionalisationInfo> regions = Lists.newArrayList();
    
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
    
    public void setChannelNumber(String channelNumber) {
        this.channelNumber = channelNumber;
    }
    
    public void setRegions(List<RegionalisationInfo> regions) {
        this.regions = regions;
    }
    
    public EpgContent createEpgContent() {
        EpgContent epgContent = new EpgContent();
        
        epgContent.setChannelId(channelId);
        epgContent.setChannelNumber(channelNumber);
        
        RegionalisationList regionalisationList = new RegionalisationList();
        for (RegionalisationInfo regionalisation : regions) {
            regionalisationList.getRegionalisation().add(regionalisation.createRegionalisation());                
        }
        epgContent.setRegionalisationList(regionalisationList);
        
        return epgContent;
    }
}