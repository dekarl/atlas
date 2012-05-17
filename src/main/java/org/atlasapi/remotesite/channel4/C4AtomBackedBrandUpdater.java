package org.atlasapi.remotesite.channel4;

import static org.atlasapi.media.entity.Identified.TO_URI;

import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Clip;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Described;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.remotesite.FetchException;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sun.syndication.feed.atom.Feed;

public class C4AtomBackedBrandUpdater implements C4BrandUpdater {

	private static final Pattern BRAND_PAGE_PATTERN = Pattern.compile("http://www.channel4.com/programmes/([^/\\s]+)");

	private final Log log = LogFactory.getLog(getClass());
	
	private final C4AtomApiClient feedClient;
	private final C4AtomContentResolver resolver;
	private final ContentWriter writer;
	private final C4BrandExtractor extractor;

	private boolean canUpdateDescriptions = true;
	
	public C4AtomBackedBrandUpdater(C4AtomApiClient feedClient, ContentResolver contentResolver, ContentWriter contentWriter, ChannelResolver channelResolver) {
		this.feedClient = feedClient;
		this.resolver = new C4AtomContentResolver(contentResolver);
		this.writer = contentWriter;
		this.extractor = new C4BrandExtractor(feedClient, channelResolver);
	}
	
	@Override
	public boolean canFetch(String uri) {
		return BRAND_PAGE_PATTERN.matcher(uri).matches();
	}

	public Brand createOrUpdateBrand(String uri) {
	    Preconditions.checkArgument(canFetch(uri), "Cannot fetch C4 uri: %s as it is not in the expected format: %s",uri, BRAND_PAGE_PATTERN.toString());

	    try {
			log.info("Fetching C4 brand " + uri);
			Optional<Feed> source = feedClient.brandFeed(uri);
			
			if (source.isPresent()) {
			    BrandSeriesAndEpisodes brandHierarchy = extractor.extract(source.get());
			    
			    writer.createOrUpdate(resolveAndUpdate(brandHierarchy.getBrand()));
			    for (SeriesAndEpisodes seriesAndEpisodes : brandHierarchy.getSeriesAndEpisodes()) {
			        writer.createOrUpdate(seriesAndEpisodes.getSeries());
			        for (Episode episode : seriesAndEpisodes.getEpisodes()) {
			            writer.createOrUpdate(episode);
			        }
			    }
			    
			    return brandHierarchy.getBrand();
			}
			throw new FetchException("Failed to fetch " + uri);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

    private Brand resolveAndUpdate(Brand brand) {
        Optional<Brand> existingBrand = resolver.brandFor(brand.getCanonicalUri());
        if (!existingBrand.isPresent()) {
            return brand;
        }
        return updateContent(existingBrand.get(), brand);
    }

    private <T extends Content> T updateContent(T existing, T fetched) {
        existing = updateDescribed(existing, fetched);
        
        Set<Clip> mergedClips = Sets.newHashSet();
        ImmutableMap<String, Clip> fetchedClips = Maps.uniqueIndex(fetched.getClips(), TO_URI);
        for (Clip existingClip : existing.getClips()) {
            Clip fetchedClip = fetchedClips.get(existingClip.getCanonicalUri());
            if (fetchedClip != null) {
                mergedClips.add(updateClip(existingClip, fetchedClip));
            }
        }
        for (Clip fetchedClip : fetched.getClips()) {
            mergedClips.add(fetchedClip);
        }
        
        existing.setClips(fetched.getClips());
        if (!Objects.equal(mergedClips, existing.getClips())) {
            copyLastUpdated(fetched, existing);
        }

        return existing;
    }

    private Clip updateClip(Clip existingClip, Clip fetchedClip) {
        existingClip = updateDescribed(existingClip, fetchedClip);
        
        Version existingVersion = Iterables.getOnlyElement(existingClip.getVersions());
        Version fetchedVersion = Iterables.getOnlyElement(fetchedClip.getVersions());
        existingClip.setVersions(Sets.newHashSet(updateVersion(existingVersion, fetchedVersion)));
        return existingClip;
    }

    private Version updateVersion(Version existingVersion, Version fetchedVersion) {
        return existingVersion;
    }

    private <T extends Described> T updateDescribed(T existing, T fetched) {
        if (canUpdateDescriptions) {
            if (!Objects.equal(existing.getTitle(), fetched.getTitle())) {
                existing.setTitle(fetched.getTitle());
                copyLastUpdated(fetched, existing);
            }
            if (!Objects.equal(existing.getDescription(), fetched.getDescription())) {
                existing.setDescription(fetched.getDescription());
                copyLastUpdated(fetched, existing);
            }
            if (!Objects.equal(existing.getImage(), fetched.getImage())) {
                existing.setImage(fetched.getImage());
                copyLastUpdated(fetched, existing);
            }
            if (!Objects.equal(existing.getThumbnail(), fetched.getThumbnail())) {
                existing.setThumbnail(fetched.getThumbnail());
                copyLastUpdated(fetched, existing);
            }
            if (!Objects.equal(existing.getGenres(), fetched.getGenres())) {
                existing.setGenres(fetched.getGenres());
                copyLastUpdated(fetched, existing);
            }
        }
        return existing;
    }

    private void copyLastUpdated(Identified from, Identified to) {
        to.setLastUpdated(from.getLastUpdated());
    }

}
