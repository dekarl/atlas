package org.atlasapi.output.simple;

import java.math.BigInteger;
import java.util.Set;

import org.atlasapi.application.ApplicationConfiguration;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.EntityType;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.ReleaseDate;
import org.atlasapi.media.entity.Song;
import org.atlasapi.media.entity.Subtitles;
import org.atlasapi.media.entity.Version;
import org.atlasapi.media.entity.simple.BrandSummary;
import org.atlasapi.media.entity.simple.Channel;
import org.atlasapi.media.entity.simple.Language;
import org.atlasapi.media.entity.simple.Restriction;
import org.atlasapi.media.entity.simple.SeriesSummary;
import org.atlasapi.media.product.ProductResolver;
import org.atlasapi.media.segment.SegmentResolver;
import org.atlasapi.output.Annotation;
import org.atlasapi.persistence.content.ContentGroupResolver;
import org.atlasapi.persistence.output.ContainerSummaryResolver;
import org.atlasapi.persistence.topic.TopicQueryResolver;
import org.joda.time.DateTime;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.ids.NumberToShortStringCodec;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.SystemClock;

public class ItemModelSimplifier extends ContentModelSimplifier<Item, org.atlasapi.media.entity.simple.Item> {

    private final NumberToShortStringCodec idCodec;
    private final ContainerSummaryResolver containerSummaryResolver;
    private final ChannelResolver channelResolver;
    private final Clock clock;
    private final SegmentModelSimplifier segmentSimplifier;
    
    public ItemModelSimplifier(String localHostName, ContentGroupResolver contentGroupResolver, TopicQueryResolver topicResolver, ProductResolver productResolver, SegmentResolver segmentResolver, ContainerSummaryResolver containerSummaryResolver, ChannelResolver channelResolver, NumberToShortStringCodec idCodec){
        this(localHostName, contentGroupResolver, topicResolver, productResolver, segmentResolver, containerSummaryResolver, channelResolver, idCodec, new SystemClock());
    }

    public ItemModelSimplifier(String localHostName, ContentGroupResolver contentGroupResolver, TopicQueryResolver topicResolver, ProductResolver productResolver, SegmentResolver segmentResolver, ContainerSummaryResolver containerSummaryResolver, ChannelResolver channelResolver, NumberToShortStringCodec idCodec, Clock clock) {
        super(localHostName, contentGroupResolver, topicResolver, productResolver);
        this.containerSummaryResolver = containerSummaryResolver;
        this.clock = clock;
        this.segmentSimplifier = segmentResolver != null ? new SegmentModelSimplifier(segmentResolver) : null;
        this.channelResolver = channelResolver;
        this.idCodec = idCodec;
    }

    @Override
    public org.atlasapi.media.entity.simple.Item simplify(Item full, final Set<Annotation> annotations, final ApplicationConfiguration config) {

        org.atlasapi.media.entity.simple.Item simple = new org.atlasapi.media.entity.simple.Item();

        copyProperties(full, simple, annotations, config);

        boolean doneSegments = false;
        for (Version version : full.getVersions()) {
            addTo(simple, version, full, annotations);
            if (!doneSegments && !version.getSegmentEvents().isEmpty() && annotations.contains(Annotation.SEGMENT_EVENTS) && segmentSimplifier != null) {
                simple.setSegments(segmentSimplifier.simplify(version.getSegmentEvents(), annotations, config));
                doneSegments = true;
            }
        }

        return simple;
    }

    private void copyProperties(Item fullItem, org.atlasapi.media.entity.simple.Item simpleItem, Set<Annotation> annotations, ApplicationConfiguration config) {
        copyBasicContentAttributes(fullItem, simpleItem, annotations, config);
        simpleItem.setType(EntityType.from(fullItem).toString());

        if (annotations.contains(Annotation.EXTENDED_DESCRIPTION)) {
            simpleItem.setBlackAndWhite(fullItem.getBlackAndWhite());
            simpleItem.setCountriesOfOrigin(fullItem.getCountriesOfOrigin());
            simpleItem.setScheduleOnly(fullItem.isScheduleOnly());
        }

        if (fullItem.getContainer() != null) {
            simpleItem.setBrandSummary(summaryFromResolved(fullItem.getContainer(), annotations));
        }

        if (fullItem instanceof Episode) {
            Episode episode = (Episode) fullItem;

            if (annotations.contains(Annotation.DESCRIPTION) || annotations.contains(Annotation.EXTENDED_DESCRIPTION) || annotations.contains(Annotation.SERIES_SUMMARY)) {
                ParentRef series = episode.getSeriesRef();
                if (series != null) {
                    simpleItem.setSeriesSummary(seriesSummaryFromResolved(series, annotations));
                }
            }
            
            if (annotations.contains(Annotation.DESCRIPTION) || annotations.contains(Annotation.EXTENDED_DESCRIPTION)) {
                simpleItem.setEpisodeNumber(episode.getEpisodeNumber());
            }

            if (annotations.contains(Annotation.EXTENDED_DESCRIPTION)) {
                simpleItem.setSeriesNumber(episode.getSeriesNumber());
            }

        } else if (fullItem instanceof Film && annotations.contains(Annotation.EXTENDED_DESCRIPTION)) {
            Film film = (Film) fullItem;
            simpleItem.setSubtitles(simpleSubtitlesFrom(film.getSubtitles()));
            simpleItem.setReleaseDates(simpleReleaseDate(film.getReleaseDates()));
        } else if (fullItem instanceof Song) {
            Song song = (Song) fullItem;
            simpleItem.setIsrc(song.getIsrc());
            if (song.getDuration() != null) {
                simpleItem.setDuration(song.getDuration().getStandardSeconds());
            }
        }
    }

    private Iterable<org.atlasapi.media.entity.simple.ReleaseDate> simpleReleaseDate(Set<ReleaseDate> releaseDates) {
        return Iterables.transform(releaseDates, new Function<ReleaseDate, org.atlasapi.media.entity.simple.ReleaseDate>() {

            @Override
            public org.atlasapi.media.entity.simple.ReleaseDate apply(ReleaseDate input) {
                return new org.atlasapi.media.entity.simple.ReleaseDate(
                        input.date().toDateTimeAtStartOfDay(DateTimeZones.UTC).toDate(),
                        input.country().code(),
                        input.type().toString().toLowerCase());
            }
        });
    }

    private Iterable<org.atlasapi.media.entity.simple.Subtitles> simpleSubtitlesFrom(Set<Subtitles> subtitles) {
        return Iterables.filter(Iterables.transform(subtitles, new Function<Subtitles, org.atlasapi.media.entity.simple.Subtitles>() {

            @Override
            public org.atlasapi.media.entity.simple.Subtitles apply(Subtitles input) {
                Language lang = languageForCode(input.code());
                return lang == null ? null : new org.atlasapi.media.entity.simple.Subtitles(lang);
            }
        }), Predicates.notNull());
    }

    private void addTo(org.atlasapi.media.entity.simple.Item simpleItem, Version version, Item item, Set<Annotation> annotations) {

        if (annotations.contains(Annotation.LOCATIONS) || annotations.contains(Annotation.AVAILABLE_LOCATIONS)) {
            for (Encoding encoding : version.getManifestedAs()) {
                addTo(simpleItem, version, encoding, item, annotations);
            }
        }

        Iterable<Broadcast> broadcasts = null;
        if (annotations.contains(Annotation.BROADCASTS)) {
            broadcasts = filterInactive(version.getBroadcasts());
        } else if (annotations.contains(Annotation.FIRST_BROADCASTS)) {
            broadcasts = firstBroadcasts(filterInactive(version.getBroadcasts()));
        } else if (annotations.contains(Annotation.NEXT_BROADCASTS)) {
            broadcasts = nextBroadcast(filterInactive(version.getBroadcasts()));
        }
        if (broadcasts != null) {
            for (Broadcast broadcast : broadcasts) {
                org.atlasapi.media.entity.simple.Broadcast simpleBroadcast = simplify(broadcast, annotations);
                copyProperties(version, simpleBroadcast, item);
                simpleItem.addBroadcast(simpleBroadcast);
            }
        }
    }

    private Iterable<Broadcast> nextBroadcast(Iterable<Broadcast> broadcasts) {
        DateTime now = clock.now();
        DateTime earliest = null;
        Builder<Broadcast> filteredBroadcasts = ImmutableSet.builder();
        for (Broadcast broadcast : broadcasts) {
            DateTime transmissionTime = broadcast.getTransmissionTime();
            if (transmissionTime.isAfter(now) && (earliest == null || transmissionTime.isBefore(earliest))) {
                earliest = transmissionTime;
                filteredBroadcasts = ImmutableSet.<Broadcast>builder().add(broadcast);
            } else if (transmissionTime.isEqual(earliest)) {
                filteredBroadcasts.add(broadcast);
            }
        }
        return filteredBroadcasts.build();
    }

    private Iterable<Broadcast> firstBroadcasts(Iterable<Broadcast> broadcasts) {
        DateTime earliest = null;
        Builder<Broadcast> filteredBroadcasts = ImmutableSet.builder();
        for (Broadcast broadcast : broadcasts) {
            DateTime transmissionTime = broadcast.getTransmissionTime();
            if (earliest == null || transmissionTime.isBefore(earliest)) {
                earliest = transmissionTime;
                filteredBroadcasts = ImmutableSet.<Broadcast>builder().add(broadcast);
            } else if (transmissionTime.isEqual(earliest)) {
                filteredBroadcasts.add(broadcast);
            }
        }
        return filteredBroadcasts.build();
    }

    private Iterable<Broadcast> filterInactive(Iterable<Broadcast> broadcasts) {
        return Iterables.filter(broadcasts, new Predicate<Broadcast>() {

            @Override
            public boolean apply(Broadcast input) {
                return input.isActivelyPublished();
            }
        });
    }

    private org.atlasapi.media.entity.simple.Broadcast simplify(Broadcast broadcast, Set<Annotation> annotations) {
        org.atlasapi.media.entity.simple.Broadcast simpleModel = new org.atlasapi.media.entity.simple.Broadcast(broadcast.getBroadcastOn(), broadcast.getTransmissionTime(),
                broadcast.getTransmissionEndTime(), broadcast.getSourceId());

        simpleModel.setRepeat(broadcast.getRepeat());
        simpleModel.setSubtitled(broadcast.getSubtitled());
        simpleModel.setSigned(broadcast.getSigned());
        simpleModel.setAudioDescribed(broadcast.getAudioDescribed());
        simpleModel.setHighDefinition(broadcast.getHighDefinition());
        simpleModel.setWidescreen(broadcast.getWidescreen());
        simpleModel.setSurround(broadcast.getSurround());
        simpleModel.setLive(broadcast.getLive());
        simpleModel.setPremiere(broadcast.getPremiere());
        simpleModel.setNewSeries(broadcast.getNewSeries());
        simpleModel.setAliases(broadcast.getAliasUrls());
        simpleModel.setChannel(simplify(channelResolver.fromUri(broadcast.getBroadcastOn()).requireValue(), annotations));

        return simpleModel;
    }
    
    private Channel simplify(org.atlasapi.media.channel.Channel channel, Set<Annotation> annotations) {
        Channel simpleChannel = new Channel();
        simpleChannel.setId(idCodec.encode(BigInteger.valueOf(channel.getId())));
        if(annotations.contains(Annotation.CHANNEL_SUMMARY)) {
            simpleChannel.setTitle(channel.title());
            simpleChannel.setImage(channel.image());
        }
        return simpleChannel;
    }

    private void copyProperties(Version version, org.atlasapi.media.entity.simple.Version simpleLocation, org.atlasapi.media.entity.Item item) {

        simpleLocation.setPublishedDuration(version.getPublishedDuration());
        simpleLocation.setDuration(durationFrom(item, version));
        simpleLocation.set3d(version.is3d());

        Restriction restriction = new Restriction();

        if (version.getRestriction() != null) {
            restriction.setRestricted(version.getRestriction().isRestricted());
            restriction.setMinimumAge(version.getRestriction().getMinimumAge());
            restriction.setMessage(version.getRestriction().getMessage());
        }

        simpleLocation.setRestriction(restriction);
    }

    // temporary fix: some versions are missing durations so
    // we fall back to the broadcast and location durations
    private Integer durationFrom(org.atlasapi.media.entity.Item item, Version version) {
        if (version.getDuration() != null && version.getDuration() > 0) {
            return version.getDuration();
        }
        Iterable<Broadcast> broadcasts = item.flattenBroadcasts();
        if (Iterables.isEmpty(broadcasts)) {
            return null;
        }
        return Ordering.natural().max(Iterables.transform(broadcasts, new Function<Broadcast, Integer>() {

            @Override
            public Integer apply(Broadcast input) {
                Integer duration = input.getBroadcastDuration();
                if (duration == null) {
                    return 0;
                }
                return duration;
            }
        }));
    }

    private void addTo(org.atlasapi.media.entity.simple.Item simpleItem, Version version, Encoding encoding, Item item, Set<Annotation> annotations) {
        DateTime now = new DateTime(DateTimeZones.UTC);
        for (Location location : encoding.getAvailableAt()) {
            if (!annotations.contains(Annotation.AVAILABLE_LOCATIONS) || location.getPolicy() == null || available(location.getPolicy(), now)) {
                addTo(simpleItem, version, encoding, location, item);
            }
        }
    }

    private boolean available(Policy policy, DateTime now) {
        return policy.getAvailabilityStart() == null
                || policy.getAvailabilityEnd() == null
                || policy.getAvailabilityStart().isBefore(now) && policy.getAvailabilityEnd().isAfter(now);
    }

    private void addTo(org.atlasapi.media.entity.simple.Item simpleItem, Version version, Encoding encoding, Location location, Item item) {

        org.atlasapi.media.entity.simple.Location simpleLocation = new org.atlasapi.media.entity.simple.Location();

        copyProperties(version, simpleLocation, item);
        copyProperties(encoding, simpleLocation);
        copyProperties(location, simpleLocation);

        simpleItem.addLocation(simpleLocation);
    }

    private SeriesSummary seriesSummaryFromResolved(ParentRef seriesRef, Set<Annotation> annotations) {
        SeriesSummary baseSummary = new SeriesSummary();
        baseSummary.setUri(seriesRef.getUri());

        return annotations.contains(Annotation.SERIES_SUMMARY) ? containerSummaryResolver.summarizeSeries(seriesRef).or(baseSummary) : baseSummary;
    }

    private BrandSummary summaryFromResolved(ParentRef container, Set<Annotation> annotations) {
        BrandSummary baseSummary = new BrandSummary();
        baseSummary.setUri(container.getUri());

        return annotations.contains(Annotation.BRAND_SUMMARY) ? containerSummaryResolver.summarizeTopLevelContainer(container).or(baseSummary) : baseSummary;
    }

    private void copyProperties(Encoding encoding, org.atlasapi.media.entity.simple.Location simpleLocation) {

        simpleLocation.setAdvertisingDuration(encoding.getAdvertisingDuration());
        simpleLocation.setAudioBitRate(encoding.getAudioBitRate());
        simpleLocation.setAudioChannels(encoding.getAudioChannels());
        simpleLocation.setBitRate(encoding.getBitRate());
        simpleLocation.setContainsAdvertising(encoding.getContainsAdvertising());
        if (encoding.getDataContainerFormat() != null) {
            simpleLocation.setDataContainerFormat(encoding.getDataContainerFormat().toString());
        }
        simpleLocation.setDataSize(encoding.getDataSize());
        simpleLocation.setDistributor(encoding.getDistributor());
        simpleLocation.setHasDOG(encoding.getHasDOG());
        simpleLocation.setSource(encoding.getSource());
        simpleLocation.setVideoAspectRatio(encoding.getVideoAspectRatio());
        simpleLocation.setVideoBitRate(encoding.getVideoBitRate());

        if (encoding.getVideoCoding() != null) {
            simpleLocation.setVideoCoding(encoding.getVideoCoding().toString());
        }
        if (encoding.getAudioCoding() != null) {
            simpleLocation.setAudioCoding(encoding.getAudioCoding().toString());
        }

        simpleLocation.setVideoFrameRate(encoding.getVideoFrameRate());
        simpleLocation.setVideoHorizontalSize(encoding.getVideoHorizontalSize());
        simpleLocation.setVideoProgressiveScan(encoding.getVideoProgressiveScan());
        simpleLocation.setVideoVerticalSize(encoding.getVideoVerticalSize());
    }

    private void copyProperties(Location location, org.atlasapi.media.entity.simple.Location simpleLocation) {
        Policy policy = location.getPolicy();
        if (policy != null) {
            if (policy.getActualAvailabilityStart() != null) {
                simpleLocation.setActualAvailabilityStart(policy.getActualAvailabilityStart().toDate());
            }
            if (policy.getAvailabilityStart() != null) {
                simpleLocation.setAvailabilityStart(policy.getAvailabilityStart().toDate());
            }
            if (policy.getAvailabilityEnd() != null) {
                simpleLocation.setAvailabilityEnd(policy.getAvailabilityEnd().toDate());
            }
            if (policy.getDrmPlayableFrom() != null) {
                simpleLocation.setDrmPlayableFrom(policy.getDrmPlayableFrom().toDate());
            }
            if (policy.getAvailableCountries() != null) {
                simpleLocation.setAvailableCountries(Countries.toCodes(policy.getAvailableCountries()));
            }
            if (policy.getRevenueContract() != null) {
                simpleLocation.setRevenueContract(policy.getRevenueContract().key());
            }
            if (policy.getPrice() != null) {
                simpleLocation.setPrice(policy.getPrice().getAmount());
                simpleLocation.setCurrency(policy.getPrice().getCurrency().getCurrencyCode());
            }
            if (policy.getPlatform() != null) {
                simpleLocation.setPlatform(policy.getPlatform().key());
            }
            if (policy.getNetwork() != null) {
                simpleLocation.setNetwork(policy.getNetwork().key());
            }
        }

        simpleLocation.setTransportIsLive(location.getTransportIsLive());
        if (location.getTransportType() != null) {
            simpleLocation.setTransportType(location.getTransportType().toString());
        }
        if (location.getTransportSubType() != null) {
            simpleLocation.setTransportSubType(location.getTransportSubType().toString());
        }
        simpleLocation.setUri(location.getUri());
        simpleLocation.setEmbedCode(location.getEmbedCode());
        simpleLocation.setEmbedId(location.getEmbedId());

    }
}
