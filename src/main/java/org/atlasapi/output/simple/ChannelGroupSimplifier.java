package org.atlasapi.output.simple;

import java.math.BigInteger;
import java.util.List;

import org.atlasapi.media.channel.ChannelGroup;
import org.atlasapi.media.channel.ChannelGroupResolver;
import org.atlasapi.media.channel.Platform;
import org.atlasapi.media.channel.Region;
import org.atlasapi.media.channel.TemporalString;
import org.atlasapi.media.entity.simple.HistoricalChannelGroupEntry;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.metabroadcast.common.ids.NumberToShortStringCodec;
import com.metabroadcast.common.intl.Countries;

public class ChannelGroupSimplifier {
    
    private final NumberToShortStringCodec idCodec;
    private final ChannelGroupResolver channelGroupResolver;
    private final PublisherSimplifier publisherSimplifier;

    public ChannelGroupSimplifier(NumberToShortStringCodec idCodec, ChannelGroupResolver channelGroupResolver, PublisherSimplifier publisherSimplifier) {
        this.idCodec = idCodec;
        this.channelGroupResolver = channelGroupResolver;
        this.publisherSimplifier = publisherSimplifier;
    }

    public org.atlasapi.media.entity.simple.ChannelGroup simplify(ChannelGroup input, boolean showHistory) {
        org.atlasapi.media.entity.simple.ChannelGroup simple = new org.atlasapi.media.entity.simple.ChannelGroup();
        
        if (input instanceof Platform) {
            simple.setType("platform");
        } else if (input instanceof Region) {
            simple.setType("region");
        }
        simple.setUri(input.getCanonicalUri());
        if (input.getId() != null) {
            simple.setId(idCodec.encode(BigInteger.valueOf(input.getId())));
        }
        simple.setAliases(input.getAliasUrls());
        simple.setTitle(input.getTitle());
        if (input.getAvailableCountries() != null) {
            simple.setAvailableCountries(Countries.toCodes(input.getAvailableCountries()));
        }
        simple.setPublisherDetails(publisherSimplifier.simplify(input.getPublisher()));
        
        if (input instanceof Platform) {
            simple.setRegions(Iterables.transform(
                channelGroupResolver.channelGroupsFor(((Platform)input).getRegions()), 
                new Function<ChannelGroup, org.atlasapi.media.entity.simple.ChannelGroup>() {
                    @Override
                    public org.atlasapi.media.entity.simple.ChannelGroup apply(ChannelGroup input) {
                        return toSubChannelGroup(input);
                    }
                }
            ));
        }
        if (input instanceof Region) {
            if (((Region)input).getPlatform() != null) {
                Optional<ChannelGroup> platform = channelGroupResolver.channelGroupFor(((Region)input).getPlatform());
                if (!platform.isPresent()) {
                    throw new RuntimeException("Could not resolve platform on Region " +  input.getTitle());
                }
                simple.setPlatform(toSubChannelGroup(platform.get()));
            }
        }
        if (showHistory) {
            simple.setHistory(calculateChannelHistory(input));
        }
        
        return simple;
    }

    private org.atlasapi.media.entity.simple.ChannelGroup toSubChannelGroup(ChannelGroup input) {
        org.atlasapi.media.entity.simple.ChannelGroup simple = new org.atlasapi.media.entity.simple.ChannelGroup();
        simple.setId(idCodec.encode(BigInteger.valueOf(input.getId())));
        simple.setTitle(input.getTitle());
        return simple;
    }
    
    private List<HistoricalChannelGroupEntry> calculateChannelHistory(ChannelGroup input) {
        List<HistoricalChannelGroupEntry> entries = Lists.newArrayList();
        for (TemporalString title : input.getAllTitles()) {
            HistoricalChannelGroupEntry entry = new HistoricalChannelGroupEntry();
            entry.setStartDate(title.getStartDate());
            entry.setTitle(title.getValue());
            entries.add(entry);
        }
        return entries;
    }
}
