package org.atlasapi.output.simple;

import java.math.BigInteger;
import java.util.List;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.channel.TemporalString;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.simple.HistoricalChannelEntry;
import org.atlasapi.media.entity.simple.PublisherDetails;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.ids.NumberToShortStringCodec;

public class ChannelSimplifier {

    private final NumberToShortStringCodec idCodec;
    private final ChannelResolver channelResolver;
    private final PublisherSimplifier publisherSimplifier;
    
    public ChannelSimplifier(NumberToShortStringCodec idCodec, ChannelResolver channelResolver, PublisherSimplifier publisherSimplifier) {
        this.idCodec = idCodec;
        this.channelResolver = channelResolver;
        this.publisherSimplifier = publisherSimplifier;
    }
    
    public org.atlasapi.media.entity.simple.Channel simplify(Channel input, final boolean showHistory, boolean showParent, final boolean showVariations) {
        
        org.atlasapi.media.entity.simple.Channel simple = new org.atlasapi.media.entity.simple.Channel();
        
        simple.setType("channel");
        simple.setUri(input.getCanonicalUri());
        if (input.getId() != null) {
            simple.setId(idCodec.encode(BigInteger.valueOf(input.getId())));
        }
        simple.setAliases(input.getAliasUrls());
        simple.setHighDefinition(input.highDefinition());
        simple.setRegional(input.regional());
        if (input.timeshift() != null) {
            simple.setTimeshift(input.timeshift().getStandardSeconds());
        }
        simple.setTitle(input.title());
        simple.setImage(input.image());
        simple.setMediaType(input.mediaType() != null ? input.mediaType().toString().toLowerCase() : null);
        simple.setStartDate(input.startDate());            
        simple.setEndDate(input.endDate());    
        
        simple.setPublisherDetails(publisherSimplifier.simplify(input.source()));
        simple.setBroadcaster(publisherSimplifier.simplify(input.broadcaster()));
        simple.setAvailableFrom(Iterables.transform(input.availableFrom(), new Function<Publisher, PublisherDetails>() {
            @Override
            public PublisherDetails apply(Publisher input) {
                return publisherSimplifier.simplify(input);
            }
        }));
        
        if (input.parent() != null) {
            Maybe<Channel> channel = channelResolver.fromId(input.parent());
            if (!channel.hasValue()) {
                throw new RuntimeException("Could not resolve channel with id " +  input.parent());
            }
            if (showParent) {
                simple.setParent(simplify(channel.requireValue(), showHistory, false, false));
            } else {
                simple.setParent(toSubChannel(channel.requireValue()));
            }
        }
        if (input.variations() != null && !input.variations().isEmpty()) {
            simple.setVariations(Iterables.transform(
                channelResolver.forIds(input.variations()), 
                new Function<Channel, org.atlasapi.media.entity.simple.Channel>() {
                    @Override
                    public org.atlasapi.media.entity.simple.Channel apply(Channel input) {
                        if (showVariations) {
                            return simplify(input, showHistory, false, false);
                        } else {
                            return toSubChannel(input);
                        }
                    }
                }
            ));
        }
        
        if (showHistory) {            
            simple.setHistory(calculateChannelHistory(input));
        }

        return simple;
    }
    
    private List<HistoricalChannelEntry> calculateChannelHistory(Channel input) {
        List<HistoricalChannelEntry> entries = Lists.newArrayList();
        for (TemporalString title : input.allTitles()) {
            HistoricalChannelEntry entry = new HistoricalChannelEntry();
            entry.setStartDate(title.getStartDate());
            entry.setTitle(title.getValue());
            entry.setImage(input.imageForDate(title.getStartDate()));
            entries.add(entry);
        }
        for (TemporalString image : input.allImages()) {
            HistoricalChannelEntry entry = new HistoricalChannelEntry();
            entry.setStartDate(image.getStartDate());
            entry.setTitle(input.titleForDate(image.getStartDate()));
            entry.setImage(image.getValue());
            entries.add(entry);
        }
        return entries;
    }

    private org.atlasapi.media.entity.simple.Channel toSubChannel(Channel input) {
        org.atlasapi.media.entity.simple.Channel simple = new org.atlasapi.media.entity.simple.Channel();
        simple.setId(idCodec.encode(BigInteger.valueOf(input.getId())));
        simple.setTitle(input.title());
        return simple;
    }
}
