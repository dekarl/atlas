package org.atlasapi.remotesite.pa.channels;

import java.util.List;

import org.atlasapi.media.channel.Channel;

public class ChannelTree {
    private final Channel parent;
    private final List<Channel> children;
    
    public ChannelTree(Channel parent, List<Channel> children) {
        this.parent = parent;
        this.children = children;
    }
    
    public Channel getParent() {
        return parent;
    }
    
    public List<Channel> getChildren() {
        return children;
    }
}
