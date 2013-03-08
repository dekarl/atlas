package org.atlasapi.remotesite.pa.channels;

import java.util.Map;

import org.atlasapi.media.channel.Platform;
import org.atlasapi.media.channel.Region;

public class ChannelGroupTree {
    private final Platform platform;
    private final Map<String, Region> regions;
    
    public ChannelGroupTree(Platform platform, Map<String, Region> regions) {
        this.platform = platform;
        this.regions = regions;
    }
    
    public Platform getPlatform() {
        return platform;
    }
    
    public Map<String, Region> getRegions() {
        return regions;
    }
}
