package org.atlasapi.query.v2;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.application.ApplicationConfiguration;
import org.atlasapi.application.query.ApplicationConfigurationFetcher;
import org.atlasapi.media.channel.ChannelGroup;
import org.atlasapi.media.channel.ChannelGroupResolver;
import org.atlasapi.media.channel.ChannelGroupType;
import org.atlasapi.media.channel.Platform;
import org.atlasapi.output.Annotation;
import org.atlasapi.output.AtlasErrorSummary;
import org.atlasapi.output.AtlasModelWriter;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.query.v2.ChannelGroupFilterer.ChannelGroupFilter;
import org.atlasapi.query.v2.ChannelGroupFilterer.ChannelGroupFilter.ChannelGroupFilterBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.http.HttpStatusCode;
import com.metabroadcast.common.ids.NumberToShortStringCodec;
import com.metabroadcast.common.query.Selection;
import com.metabroadcast.common.query.Selection.SelectionBuilder;

@Controller
public class ChannelGroupController extends BaseController<Iterable<ChannelGroup>> {

    private static final ImmutableSet<Annotation> validAnnotations = ImmutableSet.<Annotation>builder()
        .add(Annotation.CHANNELS)
        .add(Annotation.HISTORY)
        .build();
    
    private static final AtlasErrorSummary NOT_FOUND = new AtlasErrorSummary(new NullPointerException())
        .withErrorCode("Channel Group not found")
        .withStatusCode(HttpStatusCode.NOT_FOUND);
    private static final AtlasErrorSummary FORBIDDEN = new AtlasErrorSummary(new NullPointerException())
        .withStatusCode(HttpStatusCode.FORBIDDEN);
    
    private static final AtlasErrorSummary BAD_ANNOTATION = new AtlasErrorSummary(new NullPointerException())
        .withMessage("Invalid annotation specified. Valid annotations are: " + Joiner.on(',').join(Iterables.transform(validAnnotations, Annotation.TO_KEY)))
        .withStatusCode(HttpStatusCode.BAD_REQUEST);
    
    private static final String TYPE_KEY = "type";
    private static final String PLATFORM_ID_KEY = "platform_id";
    private static final SelectionBuilder SELECTION_BUILDER = Selection.builder().withMaxLimit(50).withDefaultLimit(10);
    private final ChannelGroupResolver channelGroupResolver;
    private final ChannelGroupFilterer filterer = new ChannelGroupFilterer();
    private final NumberToShortStringCodec idCodec;
    private final QueryParameterAnnotationsExtractor annotationExtractor;
   

    public ChannelGroupController(ApplicationConfigurationFetcher configFetcher, AdapterLog log, AtlasModelWriter<Iterable<ChannelGroup>> outputter, ChannelGroupResolver channelGroupResolver, NumberToShortStringCodec idCodec) {
        super(configFetcher, log, outputter);
        this.channelGroupResolver = channelGroupResolver;
        this.idCodec = idCodec;
        this.annotationExtractor = new QueryParameterAnnotationsExtractor();
    }

    @RequestMapping(value={"/3.0/channel_groups.*", "/channel_groups.*"})
    public void listChannels(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = TYPE_KEY, required = false) String type,
            @RequestParam(value = PLATFORM_ID_KEY, required = false) String platformId) throws IOException {
        try {
            final ApplicationConfiguration appConfig = appConfig(request);
            
            List<ChannelGroup> channelGroups = ImmutableList.copyOf(channelGroupResolver.channelGroups());

            Selection selection = SELECTION_BUILDER.build(request);        
            channelGroups = selection.applyTo(Iterables.filter(
                filterer.filter(channelGroups, constructFilter(platformId, type)), 
                    new Predicate<ChannelGroup>() {
                        @Override
                        public boolean apply(ChannelGroup input) {
                            return appConfig.isEnabled(input.getPublisher());
                        }
                    }));


            Optional<Set<Annotation>> annotations = annotationExtractor.extract(request);
            if (annotations.isPresent() && !validAnnotations(annotations.get())) {
                errorViewFor(request, response, BAD_ANNOTATION);
            } else {
                modelAndViewFor(request, response, channelGroups, appConfig);
            }
        } catch (Exception e) {
            errorViewFor(request, response, AtlasErrorSummary.forException(e));
        }
    }

    @RequestMapping(value={"/3.0/channel_groups/{id}.*", "/channel_groups/{id}.*"})
    public void listChannel(HttpServletRequest request, HttpServletResponse response, 
            @PathVariable("id") String id) throws IOException {
        try {
            Optional<ChannelGroup> possibleChannelGroup = channelGroupResolver.channelGroupFor(idCodec.decode(id).longValue());
            if (!possibleChannelGroup.isPresent()) {
                errorViewFor(request, response, NOT_FOUND);
            } else {
                ApplicationConfiguration appConfig = appConfig(request);
                
                if (!appConfig.isEnabled(possibleChannelGroup.get().getPublisher())) {
                    outputter.writeError(request, response, FORBIDDEN.withMessage("ChannelGroup " + id + " not available"));
                }
                
                Optional<Set<Annotation>> annotations = annotationExtractor.extract(request);
                if (annotations.isPresent() && !validAnnotations(annotations.get())) {
                    errorViewFor(request, response, BAD_ANNOTATION);
                } else {
                    modelAndViewFor(request, response, ImmutableList.of(possibleChannelGroup.get()), appConfig);
                }
            }
            
        } catch (Exception e) {
            errorViewFor(request, response, AtlasErrorSummary.forException(e));
        }
    }
    
    private boolean validAnnotations(Set<Annotation> annotations) {
        return validAnnotations.containsAll(annotations);
    }
    
    private ChannelGroupFilter constructFilter(String platformId, String type) {
        ChannelGroupFilterBuilder filter = ChannelGroupFilter.builder();
        
        if (!Strings.isNullOrEmpty(platformId)) {
            // resolve platform if present
            Optional<ChannelGroup> possiblePlatform = channelGroupResolver.channelGroupFor(idCodec.decode(platformId).longValue());
            if (!possiblePlatform.isPresent()) {
                throw new IllegalArgumentException("could not resolve channel group with id " + platformId);
            }
            if (!(possiblePlatform.get() instanceof Platform)) {
                throw new IllegalArgumentException("channel group with id " + platformId + " not a platform");
            }
            filter.withPlatform((Platform)possiblePlatform.get());
        }

        if (!Strings.isNullOrEmpty(type)) {
            // resolve channelGroup type
            if (type.equals("platform")) {
                filter.withType(ChannelGroupType.PLATFORM);
            } else if (type.equals("region")) {
                filter.withType(ChannelGroupType.REGION);
            } else {
                throw new IllegalArgumentException("type provided was not valid, should be either platform or region");
            }
        }
        
        return filter.build();
    }
}
