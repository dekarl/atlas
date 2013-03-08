package org.atlasapi.query.v2;

import static com.google.common.collect.Iterables.transform;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.application.ApplicationConfiguration;
import org.atlasapi.application.query.ApplicationConfigurationFetcher;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.output.Annotation;
import org.atlasapi.output.AtlasErrorSummary;
import org.atlasapi.output.AtlasModelWriter;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.query.v2.ChannelFilterer.ChannelFilter;
import org.atlasapi.query.v2.ChannelFilterer.ChannelFilter.ChannelFilterBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.base.MoreOrderings;
import com.metabroadcast.common.http.HttpStatusCode;
import com.metabroadcast.common.ids.NumberToShortStringCodec;
import com.metabroadcast.common.query.Selection;
import com.metabroadcast.common.query.Selection.SelectionBuilder;

@Controller
public class ChannelController extends BaseController<Iterable<Channel>> {
    
    private static final ImmutableSet<Annotation> validAnnotations = ImmutableSet.<Annotation>builder()
        .add(Annotation.CHANNEL_GROUPS)
        .add(Annotation.HISTORY)
        .add(Annotation.PARENT)
        .add(Annotation.VARIATIONS)
        .build();

    private static final AtlasErrorSummary NOT_FOUND = new AtlasErrorSummary(new NullPointerException())
        .withMessage("Channel not found")
        .withStatusCode(HttpStatusCode.NOT_FOUND);
    
    private static final AtlasErrorSummary FORBIDDEN = new AtlasErrorSummary(new NullPointerException())
        .withStatusCode(HttpStatusCode.FORBIDDEN);
    
    private static final AtlasErrorSummary BAD_ANNOTATION = new AtlasErrorSummary(new NullPointerException())
        .withMessage("Invalid annotation specified. Valid annotations are: " + Joiner.on(',').join(Iterables.transform(validAnnotations, Annotation.TO_KEY)))
        .withStatusCode(HttpStatusCode.BAD_REQUEST);
    
    private static final SelectionBuilder SELECTION_BUILDER = Selection.builder().withMaxLimit(100).withDefaultLimit(10);
    private static final Splitter CSV_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();
    private static final String TITLE = "title";
    private static final Object TITLE_REVERSE = "title.reverse";

    private final ChannelFilterer filterer = new ChannelFilterer();
    private final NumberToShortStringCodec codec;
    private final QueryParameterAnnotationsExtractor annotationExtractor;
    private final ChannelResolver channelResolver;
    
    public ChannelController(ApplicationConfigurationFetcher configFetcher, AdapterLog log, AtlasModelWriter<Iterable<Channel>> outputter, ChannelResolver channelResolver, NumberToShortStringCodec codec) {
        super(configFetcher, log, outputter);
        this.channelResolver = channelResolver;
        this.codec = codec;
        this.annotationExtractor = new QueryParameterAnnotationsExtractor();
    }
    
    @RequestMapping(value={"/3.0/channels.*", "/channels.*"})
    public void listChannels(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "platforms", required = false) String platformKey, 
            @RequestParam(value = "regions", required = false) String regionKeys, 
            @RequestParam(value = "broadcaster", required = false) String broadcasterKey,
            @RequestParam(value = "media_type", required = false) String mediaTypeKey, 
            @RequestParam(value = "available_from", required = false) String availableFromKey,
            @RequestParam(value = "order_by", required = false) String orderBy) throws IOException {
        try {
            final ApplicationConfiguration appConfig = appConfig(request); 
            
            Selection selection = SELECTION_BUILDER.build(request);

            List<Channel> channels = filterer.filter(channelResolver.all(), constructFilter(platformKey, regionKeys, broadcasterKey, mediaTypeKey, availableFromKey));
            
            Optional<Ordering<Channel>> ordering = ordering(orderBy);
            if (ordering.isPresent()) {
                channels = ordering.get().immutableSortedCopy(channels);
            }
            
            channels = selection.applyTo(Iterables.filter(
                channels, 
                new Predicate<Channel>() {
                    @Override
                    public boolean apply(Channel input) {
                        return appConfig.isEnabled(input.source());
                    }
                }));
            
            Optional<Set<Annotation>> annotations = annotationExtractor.extract(request);
            if (annotations.isPresent() && !validAnnotations(annotations.get())) {
                errorViewFor(request, response, BAD_ANNOTATION);
            } else { 
                modelAndViewFor(request, response, channels, appConfig);
            }
        } catch (Exception e) {
            errorViewFor(request, response, AtlasErrorSummary.forException(e));
        }
    }
    
    @RequestMapping(value={"/3.0/channels/{id}.*", "/channels/{id}.*"})
    public void listChannel(HttpServletRequest request, HttpServletResponse response,
            @PathVariable("id") String id) throws IOException {
        try {
            Maybe<Channel> possibleChannel = channelResolver.fromId(codec.decode(id).longValue());
            if (possibleChannel.isNothing()) {
                errorViewFor(request, response, NOT_FOUND);
            } else {
                ApplicationConfiguration appConfig = appConfig(request);
                if (!appConfig.isEnabled(possibleChannel.requireValue().source())) {
                    outputter.writeError(request, response, FORBIDDEN.withMessage("Channel " + id + " not available"));
                }

                Optional<Set<Annotation>> annotations = annotationExtractor.extract(request);
                if (annotations.isPresent() && !validAnnotations(annotations.get())) {
                    errorViewFor(request, response, BAD_ANNOTATION);
                } else {
                    modelAndViewFor(request, response, ImmutableList.of(possibleChannel.requireValue()), appConfig);
                }
            }
        } catch (IllegalArgumentException e) {
            response.sendError(HttpStatusCode.BAD_REQUEST.code(), e.getMessage());
        }
    }
    
    private boolean validAnnotations(Set<Annotation> annotations) {
        return validAnnotations.containsAll(annotations);
    }
    
    private Optional<Ordering<Channel>> ordering(String orderBy) {
        if (!Strings.isNullOrEmpty(orderBy)) {
            if (orderBy.equals(TITLE)) {
                return Optional.of(MoreOrderings.transformingOrdering(TO_TITLE));
            } else if (orderBy.equals(TITLE_REVERSE)) {
                return Optional.of(MoreOrderings.transformingOrdering(TO_TITLE, Ordering.<String>natural().reverse()));
            }
        }
        
        return Optional.absent();
    }
    
    private static final Function<Channel, String> TO_TITLE = new Function<Channel, String>() {
        @Override
        public String apply(Channel input) {
            return input.title();
        }
    };
    
    private ChannelFilter constructFilter(String platformId, String regionIds, String broadcasterKey, String mediaTypeKey, String availableFromKey) {
        ChannelFilterBuilder filter = ChannelFilter.builder();
        
        Set<Long> channelGroups = getChannelGroups(platformId, regionIds);
        if (!channelGroups.isEmpty()) {
            filter.withChannelGroups(channelGroups);
        }
        
        if (!Strings.isNullOrEmpty(broadcasterKey)) {
            filter.withBroadcaster(Publisher.fromKey(broadcasterKey).requireValue());
        }
        
        if (!Strings.isNullOrEmpty(mediaTypeKey)) {
            filter.withMediaType(MediaType.valueOf(mediaTypeKey.toUpperCase()));
        }
        
        if (!Strings.isNullOrEmpty(availableFromKey)) {
            filter.withAvailableFrom(Publisher.fromKey(availableFromKey).requireValue());
        }
        
        return filter.build();
    }

    private Set<Long> getChannelGroups(String platformId, String regionIds) {
        Builder<Long> channelGroups = ImmutableSet.builder();
        if (platformId != null) {
            channelGroups.addAll(transform(CSV_SPLITTER.split(platformId), toDecodedId));
        }
        if (regionIds != null) {
            channelGroups.addAll(transform(CSV_SPLITTER.split(regionIds), toDecodedId));
        }
        return channelGroups.build();
    }
    
    private final Function<String, Long> toDecodedId = new Function<String, Long>() {

        @Override
        public Long apply(String input) {
            return codec.decode(input).longValue();
        }
    };
}
