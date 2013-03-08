package org.atlasapi.remotesite.youview;

import java.util.List;

import org.atlasapi.media.channel.Channel;

import com.google.common.base.Optional;

public interface YouViewChannelResolver {
    
    String getChannelUri(int channelId);
    
    Optional<Channel> getChannel(int channelId);
    
    List<Channel> getAllChannels();
}
