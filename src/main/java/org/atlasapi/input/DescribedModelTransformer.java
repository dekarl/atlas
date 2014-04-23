package org.atlasapi.input;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.atlasapi.media.entity.Described;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.RelatedLink;
import org.atlasapi.media.entity.RelatedLink.Builder;
import org.atlasapi.media.entity.RelatedLink.LinkType;
import org.atlasapi.media.entity.Review;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.media.entity.simple.Description;
import org.atlasapi.media.entity.simple.PublisherDetails;
import org.joda.time.DateTime;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.time.Clock;

public abstract class DescribedModelTransformer<F extends Description,T extends Described> extends IdentifiedModelTransformer<F, T> {

    public DescribedModelTransformer(Clock clock) {
        super(clock);
    }
    
    @Override
    protected final T createIdentifiedOutput(F simple, DateTime now) {
        return setDescriptionFields(createDescribedOutput(simple, now), simple);
    }

    private T setDescriptionFields(T result, F inputContent) {
        Publisher publisher = getPublisher(inputContent.getPublisher());
        result.setPublisher(publisher);
        result.setTitle(inputContent.getTitle());
        result.setDescription(inputContent.getDescription());
        result.setImage(inputContent.getImage());
        result.setThumbnail(inputContent.getThumbnail());
        result.setRelatedLinks(relatedLinks(inputContent.getRelatedLinks()));
        if (inputContent.getSpecialization() != null) {
            result.setSpecialization(Specialization.fromKey(inputContent.getSpecialization()).valueOrNull());
        }
        if (inputContent.getMediaType() != null) {
            result.setMediaType(MediaType.valueOf(inputContent.getMediaType().toUpperCase()));
        }
        if (inputContent.getReviews() != null) {
            result.setReviews(reviews(result.getPublisher(), inputContent.getReviews()));
        }
        return result;
    }

    private Iterable<RelatedLink> relatedLinks(
            List<org.atlasapi.media.entity.simple.RelatedLink> relatedLinks) {
        return Lists.transform(relatedLinks,
            new Function<org.atlasapi.media.entity.simple.RelatedLink, RelatedLink>() {
                @Override
                public RelatedLink apply(org.atlasapi.media.entity.simple.RelatedLink input) {
                    LinkType type = LinkType.valueOf(input.getType().toUpperCase());
                    Builder link = RelatedLink.relatedLink(type,input.getUrl())
                       .withSourceId(input.getSourceId())
                       .withShortName(input.getShortName())
                       .withTitle(input.getTitle())
                       .withDescription(input.getDescription())
                       .withImage(input.getImage())
                       .withThumbnail(input.getThumbnail());
                    return link.build();
                }
            }
        );
    }
    
    private Iterable<Review> reviews(final Publisher contentPublisher, Set<org.atlasapi.media.entity.simple.Review> simpleReviews) {
        return Iterables.transform(simpleReviews, new Function<org.atlasapi.media.entity.simple.Review, Review>() {

            @Override
            public Review apply(org.atlasapi.media.entity.simple.Review simpleReview) {
                if (simpleReview.getPublisherDetails() != null && 
                        !getPublisher(simpleReview.getPublisherDetails()).equals(contentPublisher)) {
                    throw new IllegalArgumentException("Review publisher must match content publisher");
                }
                return new Review(Locale.forLanguageTag(simpleReview.getLanguage()), simpleReview.getReview());
            }
            
                
        });
    }

    protected Publisher getPublisher(PublisherDetails pubDets) {
        if (pubDets == null || pubDets.getKey() == null) {
            throw new IllegalArgumentException("missing publisher");
        }
        Maybe<Publisher> possiblePublisher = Publisher.fromKey(pubDets.getKey());
        if (possiblePublisher.isNothing()) {
            throw new IllegalArgumentException("unknown publisher " + pubDets.getKey());
        }
        return possiblePublisher.requireValue();
    }
    
    protected abstract T createDescribedOutput(F simple, DateTime now);
    
}
