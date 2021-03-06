package org.atlasapi.remotesite.metabroadcast.picks;

import java.util.Map;
import java.util.Set;

import org.atlasapi.genres.AtlasGenre;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.util.ItemAndBroadcast;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.time.LocalTimeRange;

/**
 * A predicate to decide whether a given {@link ItemAndBroadcast} is for an item on
 * in primetime
 * 
 * @author tom
 *
 */
public class PrimetimePredicate implements Predicate<ItemAndBroadcast> {

    private static final Set<String> PRIORITY_CHANNELS = ImmutableSet.of(
                "http://www.bbc.co.uk/services/bbcone/london",
                "http://www.bbc.co.uk/services/bbctwo/england",
                "http://www.itv.com/channels/itv1/london",
                "http://www.channel4.com",
                "http://www.five.tv",
                "http://www.itv.com/itv2",
                "http://www.bbc.co.uk/services/bbcthree",
                "http://www.bbc.co.uk/services/bbcfour",
                "http://ref.atlasapi.org/channels/sky1",
                "http://ref.atlasapi.org/channels/skyliving",
                "http://ref.atlasapi.org/channels/skyatlantic",
                "http://ref.atlasapi.org/channels/comedycentral",
                "http://ref.atlasapi.org/channels/universal",
                "http://ref.atlasapi.org/channels/syfy"
            );
    
    private static final LocalTimeRange CHILDRENS_PRIMETIME = new LocalTimeRange(new LocalTime(15, 0, 0), new LocalTime(19, 15, 0));
    private static final Map<String, LocalTimeRange> CHANNEL_PRIMETIME_OVERRIDES = ImmutableMap.of();
    
    private static final Map<String, LocalTimeRange> GENRE_OVERRIDES = ImmutableMap.of(
            AtlasGenre.CHILDRENS.getUri(), CHILDRENS_PRIMETIME,
            AtlasGenre.FACTUAL.getUri(), new LocalTimeRange(new LocalTime(0, 0, 0), new LocalTime(23, 59, 59)));
    
    private static final DateTimeZone UK_TIMEZONE = DateTimeZone.forID("Europe/London");
    private static final LocalTimeRange PRIMETIME = new LocalTimeRange(
            new LocalTime(19, 0, 0), new LocalTime(23, 15, 0));

    
    private final Set<String> channelUris;
    
    public PrimetimePredicate(Iterable<Channel> channels) {
        this.channelUris = ImmutableSet.copyOf(Iterables.transform(channels, Channel.TO_URI));
    }
    
    @Override
    public boolean apply(ItemAndBroadcast itemAndBroadcast) {
        
       
        Broadcast broadcast = itemAndBroadcast.getBroadcast().requireValue();
        
        return isInterestingBroadcast(itemAndBroadcast.getItem(), broadcast) 
                || isInterestingBroadcast(itemAndBroadcast.getItem(), 
                                            firstBroadcast(itemAndBroadcast.getItem()));
    }
    
    private Broadcast firstBroadcast(Item item) {
        return TRANSMISSION_TIME_ORDERING.min(Item.FLATTEN_BROADCASTS.apply(item));
    }
    
    private boolean isInterestingBroadcast(Item item, Broadcast broadcast) {
        return channelUris.contains(broadcast.getBroadcastOn()) 
                && isPrimetime(item, broadcast);
                // remove this check for now, since the flags aren't set reliably
                //&& isNewEpisode(item, broadcast); 
    }

    private boolean isNewEpisode(Item item, Broadcast broadcast) {
        // The use of a set of PRIORITY_CHANNELS is due to New(Episode|Series) being
        // unreliable right now. So we'll assume that anything on a priority channel
        // is a new episode, until we can rely on the New(Episode|Series) flags.
        return PRIORITY_CHANNELS.contains(broadcast.getBroadcastOn())
                || Boolean.TRUE.equals(broadcast.getPremiere()) 
                || Boolean.TRUE.equals(broadcast.getNewEpisode()) 
                || Boolean.TRUE.equals(broadcast.getNewSeries())
                || Boolean.TRUE.equals(broadcast.getLive());
    }

    /**
     * A naive initial implementation. Further improvements may include having different 
     * primetimes for channels, as well as not assuming everything is in the UK timezone.
     * 
     * @param transmissionTime
     * @return
     */
    private boolean isPrimetime(Item item, Broadcast broadcast) {
        LocalTimeRange primetime = primetimeForGenres(item.getGenres())
                                    .or(primetimeForChannel(broadcast.getBroadcastOn()))
                                    .or(PRIMETIME);
        
        return primetime.contains(broadcast.getTransmissionTime().withZone(UK_TIMEZONE));
    }
    
    private Optional<LocalTimeRange> primetimeForGenres(Set<String> genres) {
        for (String genre : genres) {
            LocalTimeRange genreOverride = GENRE_OVERRIDES.get(genre);
            if (genreOverride != null) {
                return Optional.of(genreOverride);
            }
        }
        return Optional.absent();
    }
    
    private Optional<LocalTimeRange> primetimeForChannel(String channelUri) {
        return Optional.fromNullable(CHANNEL_PRIMETIME_OVERRIDES.get(channelUri));
    }
    
    private static Ordering<Broadcast> TRANSMISSION_TIME_ORDERING = new Ordering<Broadcast>() {

        @Override
        public int compare(Broadcast b1, Broadcast b2) {            
            return ComparisonChain.start()
                                  .compare(b1.getTransmissionTime(), b2.getTransmissionEndTime(), Ordering.natural().nullsLast())
                                  .result();
                                  
        }
        
    };
}
