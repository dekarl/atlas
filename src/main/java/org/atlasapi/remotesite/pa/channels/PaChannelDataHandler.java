package org.atlasapi.remotesite.pa.channels;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Map.Entry;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelGroup;
import org.atlasapi.media.channel.ChannelGroupResolver;
import org.atlasapi.media.channel.ChannelGroupWriter;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.channel.ChannelWriter;
import org.atlasapi.media.channel.Platform;
import org.atlasapi.media.channel.Region;
import org.atlasapi.remotesite.pa.channels.bindings.Station;
import org.atlasapi.remotesite.pa.channels.bindings.TvChannelData;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.metabroadcast.common.base.Maybe;

public class PaChannelDataHandler {
    
    private static final String PRESS_ASSOCIATION_URL = "http://pressassociation.com"; 

    private final PaChannelsIngester channelsIngester;
    private final PaChannelGroupsIngester channelGroupsIngester;
    private final ChannelGroupResolver channelGroupResolver;
    private final ChannelGroupWriter channelGroupWriter;
    private final ChannelResolver channelResolver;
    private final ChannelWriter channelWriter;
    private final Map<String, Channel> channelMap = Maps.newHashMap();
    
    public PaChannelDataHandler(PaChannelsIngester channelsIngester, PaChannelGroupsIngester channelGroupsIngester, ChannelResolver channelResolver, ChannelWriter channelWriter, ChannelGroupResolver channelGroupResolver, ChannelGroupWriter channelGroupWriter) {
        this.channelsIngester = channelsIngester;
        this.channelGroupsIngester = channelGroupsIngester;
        this.channelResolver = channelResolver;
        this.channelWriter = channelWriter;
        this.channelGroupResolver = channelGroupResolver;
        this.channelGroupWriter = channelGroupWriter;
    }
    
    public void handle(TvChannelData channelData) {
        
        channelMap.clear();
        
        for (Station station : channelData.getStations().getStation()) {
            ChannelTree channelTree = channelsIngester.processStation(station, channelData.getServiceProviders().getServiceProvider());
            Channel parent = channelTree.getParent();
            if (parent != null) {
                parent = createOrMerge(parent);
            }
            for (Channel child : channelTree.getChildren()) {
                if (parent != null) {
                    child.setParent(parent);
                }
                for (String alias : child.getAliasUrls()) {
                    if (isPaAlias(alias)) {
                        channelMap.put(alias, child);
                    }
                }
            }
        }
        
        for (org.atlasapi.remotesite.pa.channels.bindings.Platform paPlatform : channelData.getPlatforms().getPlatform()) {
            ChannelGroupTree channelGroupTree = channelGroupsIngester.processPlatform(paPlatform, channelData.getServiceProviders().getServiceProvider(), channelData.getRegions().getRegion());
            
            Platform platform = (Platform) createOrMerge(channelGroupTree.getPlatform());
            
            if (channelGroupTree.getRegions().isEmpty()) {
                // non-regionalised platform
                channelGroupsIngester.addChannelsToPlatform(platform, paPlatform.getEpg().getEpgContent(), channelMap);
            } else {
                Map<String, Region> writtenRegions = Maps.newHashMap();
                for (Entry<String, Region> entry : channelGroupTree.getRegions().entrySet()) {
                    Region region = entry.getValue();
                    region.setPlatform(platform);
                    region = (Region) createOrMerge(region);
                    writtenRegions.put(entry.getKey(), region);
                }
                channelGroupsIngester.addChannelNumberings(paPlatform.getEpg().getEpgContent(), writtenRegions, channelMap);
            }
        }
        
        // write channels
        for (Channel child : channelMap.values()) {
            createOrMerge(child);
        }
    }
    
    private Channel createOrMerge(Channel newChannel) {
        String alias = null;
        for (String newAlias : newChannel.getAliasUrls()) {
            if (isPaAlias(newAlias)) {
                alias = newAlias;
                break;
            }
        }
        checkNotNull("channel with uri " + newChannel.getCanonicalUri() + " has no aliases");
        
        Maybe<Channel> existing = channelResolver.forAlias(alias);
        if (existing.hasValue()) {
            Channel existingChannel = existing.requireValue();

            existingChannel.setTitles(newChannel.allTitles());
            existingChannel.setImages(newChannel.allImages());
            existingChannel.setStartDate(newChannel.startDate());
            existingChannel.setEndDate(newChannel.endDate());
            existingChannel.addAliasUrls(newChannel.getAliasUrls());
            existingChannel.setParent(newChannel.parent());
            existingChannel.setMediaType(newChannel.mediaType());
            existingChannel.setHighDefinition(newChannel.highDefinition());
            existingChannel.setRegional(newChannel.regional());
            existingChannel.setTimeshift(newChannel.timeshift());
            existingChannel.setChannelNumbers(newChannel.channelNumbers());
            
            return channelWriter.createOrUpdate(existingChannel);
        } else {
            return channelWriter.createOrUpdate(newChannel);
        }
    }
    
    private ChannelGroup createOrMerge(ChannelGroup channelGroup) {
        String alias = null;
        for (String newAlias : channelGroup.getAliasUrls()) {
            if (isPaAlias(newAlias)) {
                alias = newAlias;
                break;
            }
        }
        checkNotNull("channel with uri " + channelGroup.getCanonicalUri() + " has no aliases");
        
        Optional<ChannelGroup> resolved = channelGroupResolver.fromAlias(alias);
        
        if (resolved.isPresent()) {
            ChannelGroup existing = resolved.get();

            existing.addAliasUrls(channelGroup.getAliasUrls());
            existing.setTitles(channelGroup.getAllTitles());
            
            if (channelGroup instanceof Region) {
                if (existing instanceof Region) {
                    ((Region)existing).setPlatform(((Region)channelGroup).getPlatform());
                } else {
                    throw new RuntimeException("new channelGroup with alias " + alias + " and type Region does not match existing channelGroup of type " + existing.getClass());
                }
            }
            
            return channelGroupWriter.createOrUpdate(existing);
        } else {
            return channelGroupWriter.createOrUpdate(channelGroup);
        }
    }

    private boolean isPaAlias(String alias) {
        return (alias.contains(PRESS_ASSOCIATION_URL));
    }
}
