package org.atlasapi.query.v2;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import java.io.IOException;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.application.query.ApplicationConfigurationFetcher;
import org.atlasapi.content.criteria.ContentQuery;
import org.atlasapi.media.entity.Content;
import org.atlasapi.output.AtlasErrorSummary;
import org.atlasapi.output.AtlasModelWriter;
import org.atlasapi.output.QueryResult;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.persistence.logging.AdapterLog;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.http.HttpStatusCode;
import com.metabroadcast.common.query.Selection;
import org.atlasapi.media.entity.ChildRef;
import org.atlasapi.media.entity.ContentGroup;
import org.atlasapi.persistence.content.ContentGroupResolver;
import org.atlasapi.persistence.content.ResolvedContent;

@Controller
public class ContentGroupController extends BaseController<Iterable<ContentGroup>> {

    private static final AtlasErrorSummary NOT_FOUND = new AtlasErrorSummary(new NullPointerException()).withErrorCode("CONTENT_GROUP_NOT_FOUND").withStatusCode(HttpStatusCode.NOT_FOUND);
    private static final AtlasErrorSummary FORBIDDEN = new AtlasErrorSummary(new NullPointerException()).withErrorCode("CONTENT_GROUP_UNAVAILABLE").withStatusCode(HttpStatusCode.FORBIDDEN);
    
    private static final Function<ChildRef, String> CHILD_REF_TO_URI_FN = new ChildRefToUri();
    //
    private final ContentGroupResolver contentGroupResolver;
    private final QueryController queryController;
    private final KnownTypeQueryExecutor queryExecutor;

    public ContentGroupController(ContentGroupResolver contentGroupResolver, KnownTypeQueryExecutor queryExecutor, ApplicationConfigurationFetcher configFetcher, AdapterLog log, AtlasModelWriter<? super Iterable<ContentGroup>> outputter, QueryController queryController) {
        super(configFetcher, log, outputter);
        this.contentGroupResolver = contentGroupResolver;
        this.queryExecutor = queryExecutor;
        this.queryController = queryController;
    }

    @RequestMapping(value = {"3.0/content_groups.*", "content_groups.*"})
    public void contentGroup(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ContentQuery query = builder.build(req);
        modelAndViewFor(req, resp, query.getSelection().apply(Iterables.filter(contentGroupResolver.findAll(), publisherFilter(query))), query.getConfiguration());
    }

    @RequestMapping(value = {"3.0/content_groups/{id}.*", "content_groups/{id}.*"})
    public void contentGroup(HttpServletRequest req, HttpServletResponse resp, @PathVariable("id") String id) throws IOException {
        long contentGroupId;
        try {
            contentGroupId = idCodec.decode(id).longValue();
        } catch (IllegalArgumentException e) {
            outputter.writeError(req, resp, NOT_FOUND.withMessage("Content Group " + id + " not found"));
            return;
        }
        
        ContentQuery query = builder.build(req);
        ResolvedContent resolvedContent = contentGroupResolver.findByIds(ImmutableList.of(contentGroupId));
        if (resolvedContent.isEmpty()) {
            outputter.writeError(req, resp, NOT_FOUND.withMessage("Content Group " + idCodec.decode(id).longValue() + " not found"));
            return;
        }
        
        ContentGroup contentGroup = (ContentGroup)resolvedContent.getFirstValue().requireValue();
        if (!query.allowsSource(contentGroup.getPublisher())) {
            outputter.writeError(req, resp, FORBIDDEN.withErrorCode("Content Group not available"));
            return;
        }
        modelAndViewFor(req, resp, ImmutableSet.of(contentGroup), query.getConfiguration());
    }

    @RequestMapping(value = {"3.0/content_groups/{id}/content.*", "content_groups/{id}/content.*"})
    public void contentGroupContents(HttpServletRequest req, HttpServletResponse resp, @PathVariable("id") String id) throws IOException {
        long contentGroupId;
        try {
            contentGroupId = idCodec.decode(id).longValue();
        } catch (IllegalArgumentException e) {
            outputter.writeError(req, resp, NOT_FOUND.withMessage("Content Group " + id + " not found"));
            return;
        }
        
        ContentQuery query = builder.build(req);
        ResolvedContent resolvedContent = contentGroupResolver.findByIds(ImmutableList.of(contentGroupId));
        if (resolvedContent.isEmpty()) {
            outputter.writeError(req, resp, NOT_FOUND.withMessage("Content Group " + id + " not found"));
            return;
        } 
        
        ContentGroup contentGroup = (ContentGroup) resolvedContent.getFirstValue().requireValue();
        if (!query.allowsSource(contentGroup.getPublisher())) {
            outputter.writeError(req, resp, FORBIDDEN.withErrorCode("Content Group not available"));
            return;
        }
        
        try {
            Selection selection = query.getSelection();
            QueryResult<Content, ContentGroup> result = QueryResult.of(
                    Iterables.filter(
                    Iterables.concat(
                    queryExecutor.executeUriQuery(Iterables.transform(contentGroup.getContents(), CHILD_REF_TO_URI_FN), query).values()),
                    Content.class),
                    contentGroup);
            queryController.modelAndViewFor(req, resp, result.withSelection(selection), query.getConfiguration());
        } catch (Exception e) {
            errorViewFor(req, resp, AtlasErrorSummary.forException(e));
        }
    }

    private static class ChildRefToUri implements Function<ChildRef, String> {

        @Override
        public String apply(ChildRef input) {
            return input.getUri();
        }
    }
    
    private Predicate<ContentGroup> publisherFilter(final ContentQuery query)  {

        return new Predicate<ContentGroup>() {

            @Override
            public boolean apply(ContentGroup input) {
                return query.allowsSource(input.getPublisher());
            }
        };
    }
}
