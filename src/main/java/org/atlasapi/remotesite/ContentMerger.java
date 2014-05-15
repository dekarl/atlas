package org.atlasapi.remotesite;

import java.util.Map;

import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ContentMerger {
    
    public enum MergeStrategy {
        REPLACE,
        MERGE
    }

    private final MergeStrategy versionMergeStrategy;
    
    public ContentMerger(MergeStrategy versionMergeStrategy) {
        this.versionMergeStrategy = versionMergeStrategy;
    }
    
    public Item merge(Item current, Item extracted) {

        current = mergeVersions(current, extracted);
        current = mergeContents(current, extracted);
        
        if ( current.getContainer() == null 
                || extracted.getContainer() == null
                || !current.getContainer().getUri().equals(extracted.getContainer().getUri())) {            
            current.setParentRef(extracted.getContainer());
        }
        
        if (current instanceof Episode && extracted instanceof Episode) {
            Episode currentEp = (Episode) current;
            Episode extractedEp = (Episode) extracted;
            currentEp.setEpisodeNumber(extractedEp.getEpisodeNumber());
            
            if ( currentEp.getSeriesRef() == null 
                    || extractedEp.getSeriesRef() == null
                    || !currentEp.getSeriesRef().getUri().equals(extractedEp.getSeriesRef().getUri())) {
                currentEp.setSeriesRef(extractedEp.getSeriesRef());                
            }
        }
        
        if (current instanceof Film && extracted instanceof Film) {
            Film currentFilm = (Film) current;
            Film extractedFilm = (Film) extracted;
            currentFilm.setYear(extractedFilm.getYear());
        }
        
        return current;
    }

    public Container merge(Container current, Container extracted) {
        current = mergeContents(current, extracted);
        if (current instanceof Series && extracted instanceof Series) {
            ((Series) current).withSeriesNumber(((Series) extracted).getSeriesNumber());
            ((Series) current).setParentRef(((Series) extracted).getParent());
        }
        return current;
    }

    private <C extends Content> C mergeContents(C current, C extracted) {
        current.setActivelyPublished(extracted.isActivelyPublished());
        current.setAliasUrls(extracted.getAliasUrls());
        current.setAliases(extracted.getAliases());
        current.setTitle(extracted.getTitle());
        current.setDescription(extracted.getDescription());
        current.setShortDescription(extracted.getShortDescription());
        current.setMediumDescription(extracted.getMediumDescription());
        current.setLongDescription(current.getLongDescription());
        current.setImage(extracted.getImage());
        current.setYear(extracted.getYear());
        current.setGenres(extracted.getGenres());
        current.setPeople(extracted.people());
        current.setLanguages(extracted.getLanguages());
        current.setCertificates(extracted.getCertificates());
        current.setMediaType(extracted.getMediaType());
        current.setSpecialization(extracted.getSpecialization());
        current.setLastUpdated(extracted.getLastUpdated());
        current.setClips(extracted.getClips());
        current.setEquivalentTo(extracted.getEquivalentTo());
        return current;
    }

    private Item mergeVersions(Item current, Item extracted) {
        // need to merge broadcasts on versions with same uri
        if (versionMergeStrategy.equals(MergeStrategy.REPLACE)) {
            current.setVersions(extracted.getVersions());
            return current;
        }
        
        Map<String, Version> mergedVersions = Maps.newHashMap();
        for (Version version : current.getVersions()) {
            mergedVersions.put(version.getCanonicalUri(), version);
        }
        for (Version version : extracted.getVersions()) {
            if (mergedVersions.containsKey(version.getCanonicalUri())) {
                Version mergedVersion = mergedVersions.get(version.getCanonicalUri());
                mergedVersion.setBroadcasts(Sets.union(version.getBroadcasts(), mergedVersion.getBroadcasts()));
                mergedVersion.setManifestedAs(version.getManifestedAs());
                mergedVersions.put(version.getCanonicalUri(), mergedVersion);
            } else {
                mergedVersions.put(version.getCanonicalUri(), version);
            }
        }
        current.setVersions(Sets.newHashSet(mergedVersions.values()));
        return current;
    }
    
    public static Container asContainer(Identified identified) {
        return castTo(identified, Container.class);
    }

    public static Item asItem(Identified identified) {
        return castTo(identified, Item.class);
    }

    private static <T> T castTo(Identified identified, Class<T> cls) {
        try {
            return cls.cast(identified);
        } catch (ClassCastException e) {
            throw new ClassCastException(String.format("%s: expected %s got %s", 
                identified.getCanonicalUri(), 
                cls.getSimpleName(), 
                identified.getClass().getSimpleName()));
        }
    }
}
