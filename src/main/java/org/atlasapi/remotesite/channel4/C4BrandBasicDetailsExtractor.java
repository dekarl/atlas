package org.atlasapi.remotesite.channel4;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.query.content.PerPublisherCurieExpander;
import org.atlasapi.remotesite.ContentExtractor;
import org.jdom.Element;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.metabroadcast.common.time.Clock;
import com.sun.syndication.feed.atom.Category;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.atom.Link;

/**
 * Extracts basic properties of a Brand from a C4 Atom Feed representing a Brand.
 *
 * @author Fred van den Driessche (fred@metabroadcast.com)
 */
public class C4BrandBasicDetailsExtractor implements ContentExtractor<Feed, Brand> {

	private static final String PRESENTATION_BRAND = "relation.presentationBrand";
	
	private final C4AtomApi c4AtomApi;
    private final Clock clock;
    
    private final C4LinkBrandNameExtractor linkExtractor = new C4LinkBrandNameExtractor();

	public C4BrandBasicDetailsExtractor(ChannelResolver channelResolver, Clock clock) {
        this.c4AtomApi = new C4AtomApi(channelResolver);
        this.clock = clock;
	}
	
    @Override
	public Brand extract(Feed source) {
        Preconditions.checkArgument(C4AtomApi.isABrandFeed(source), "Not a brand feed");
		
		String brandUri = brandUriFrom(source);
		
		Preconditions.checkArgument(brandUri != null && C4AtomApi.isACanonicalBrandUri(brandUri), "URI of feed is not a canonical Brand URI, got: " + brandUri);

		Brand brand = new Brand(brandUri, PerPublisherCurieExpander.CurieAlgorithm.C4.compact(brandUri), Publisher.C4);
		brand.setLastUpdated(clock.now());

		brand.addAlias(brandUri + "/4od");
		brand.addAlias(canonicalIdTag(source));

		brand.setTitle(source.getTitle());
		brand.setDescription(source.getSubtitle().getValue());
		
		brand.setMediaType(MediaType.VIDEO);
		brand.setSpecialization(Specialization.TV);
		
		Set<String> genres = Sets.newHashSet();
		for (Object cat : source.getCategories()) {
            String category = canonicalise((Category) cat);
            if (category != null) {
                genres.add(category);
            }
        }
		brand.setGenres(new C4CategoryGenreMap().mapRecognised(genres));
		
		String presentationBrand = getPresentationBrand(source);
		if(presentationBrand != null) {
		    brand.setPresentationChannel(c4AtomApi.getChannelMap().get(presentationBrand));
		}
		
		C4AtomApi.addImages(brand, source.getLogo());
		
		return brand;
	}
	
    private String canonicalIdTag(Feed source) {
        return C4AtomApi.canonicalizeBrandFeedId(source);
    }

    private final Pattern CATEGORY_PATTERN = Pattern.compile("https?://[^.]+\\.channel4\\.com/[^/]+/(tags/[^.]+)\\.atom.*");
    
	private String canonicalise(Category category) {
	    Matcher matcher = CATEGORY_PATTERN.matcher(category.getTerm());
	    if (matcher.matches()) {
	        return C4AtomApi.PROGRAMMES_BASE + matcher.group(1);
	    }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String getPresentationBrand(Feed source) {
	    Iterable<Element> markup = (Iterable<Element>) source.getForeignMarkup();
        for (Element element : markup) {
            if (PRESENTATION_BRAND.equals(element.getName())) {
                return element.getValue();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String brandUriFrom(Feed source) {
		for (Object link : Iterables.concat(source.getAlternateLinks(), source.getOtherLinks())) {
            Optional<String> uri = linkExtractor.canonicalBrandUriFrom(((Link)link).getHref());
            if (uri.isPresent()) {
                return uri.get();
            }
        }
		return null;
	}
}
