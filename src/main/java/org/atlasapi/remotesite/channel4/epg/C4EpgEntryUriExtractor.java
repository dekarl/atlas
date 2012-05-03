package org.atlasapi.remotesite.channel4.epg;

import static org.atlasapi.remotesite.channel4.C4AtomApi.PROGRAMMES_BASE;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.remotesite.channel4.epg.model.C4EpgEntry;

import com.google.common.base.Optional;

public class C4EpgEntryUriExtractor {
    
    private final Pattern uriPattern = Pattern.compile(
        "http://(.+).channel4.com/([^/]+)/([^./]+)(.atom|/episode-guide/series-(\\d+)(.atom|/episode-(\\d+).atom))"
    );
    
    private final int episodeNumberGroup = 7;
    private final int seriesNumberGroup = 5;
    private final int brandNameGroup = 3;
    
    private final String seriesUriInfix = "/episode-guide/series-";
    private final String episodeUriInfix = "/episode-";

    public Optional<String> uriForBrand(C4EpgEntry entry){
        if (!entry.hasRelatedLink()) {
            return Optional.absent();
        }
        String linkUri = entry.getRelatedLink();
        Matcher matcher = uriPattern.matcher(linkUri);
        if (!matcher.matches()) {
            return Optional.absent();
        } else {
            return Optional.of(PROGRAMMES_BASE + matcher.group(3));
        }
    }
    
    
    public Optional<String> uriForSeries(C4EpgEntry entry){
        if (!entry.hasRelatedLink()) {
            return Optional.absent();
        }
        String linkUri = entry.getRelatedLink();
        Matcher matcher = uriPattern.matcher(linkUri);
        if (matcher.matches() && matcher.group(seriesNumberGroup) != null) {
            return Optional.of(PROGRAMMES_BASE + matcher.group(brandNameGroup) + seriesUriInfix + matcher.group(seriesNumberGroup));
        } else if (matcher.matches() && entry.seriesNumber() != null) {
            return Optional.of(PROGRAMMES_BASE + matcher.group(brandNameGroup) + seriesUriInfix + entry.seriesNumber());
        }
        return Optional.absent();
    }
    
    
    public String uriForItemId(C4EpgEntry entry){
        return PROGRAMMES_BASE + entry.programmeId();
    }
    
    
    public Optional<String> uriForItemHierarchy(C4EpgEntry entry, Optional<Brand> brand){
        if (brand.isPresent() && entry.seriesNumber() != null && entry.episodeNumber() != null) {
            return Optional.of(brand.get().getCanonicalUri() + seriesUriInfix + entry.seriesNumber() + episodeUriInfix + entry.episodeNumber());
        }
        return extractHeirarchyUriFrom(entry);
    }

    private Optional<String> extractHeirarchyUriFrom(C4EpgEntry entry) {
        if (!entry.hasRelatedLink()) {
            return Optional.absent();
        }
        String linkUri = entry.getRelatedLink();
        Matcher matcher = uriPattern.matcher(linkUri);
        if (matcher.matches() && matcher.group(seriesNumberGroup) != null && matcher.group(episodeNumberGroup) != null) {
            return Optional.of(
                PROGRAMMES_BASE + matcher.group(brandNameGroup) + 
                seriesUriInfix + matcher.group(seriesNumberGroup) + 
                episodeUriInfix + matcher.group(episodeNumberGroup)
            );
        } else if (matcher.matches() && matcher.group(seriesNumberGroup) != null && entry.episodeNumber() != null) {
            return Optional.of(
                PROGRAMMES_BASE + matcher.group(brandNameGroup) + 
                seriesUriInfix + matcher.group(seriesNumberGroup) + 
                episodeUriInfix + entry.episodeNumber()
            );
        } else if (matcher.matches() && entry.seriesNumber() != null && entry.episodeNumber() != null) {
            return Optional.of(
                PROGRAMMES_BASE + matcher.group(brandNameGroup) + 
                seriesUriInfix + entry.seriesNumber() + 
                episodeUriInfix + entry.episodeNumber()
            );
        }
        return Optional.absent();
    }
    
    public Optional<String> uriForItemSynthesized(C4EpgEntry entry, Optional<Brand> brand){
        if (brand.isPresent()) {
            return Optional.of(synthBrandUri(brand.get()) + slotId(entry));
        }
        return Optional.of(synthBrandUri(entry) + slotId(entry));
    }

    private String synthBrandUri(C4EpgEntry epgEntry) {
        return "http://www.channel4.com/programmes/synthesized/" + brandName(epgEntry.title());
    }
    
    private static String brandName(String title) {
        return title.replaceAll("[^ a-zA-Z0-9]", "").replaceAll("\\s+", "-").toLowerCase();
    }

    private String synthBrandUri(Brand brand) {
        return brand.getCanonicalUri().replace(
            "http://www.channel4.com/programmes/", 
            "http://www.channel4.com/programmes/synthesized/");
    }

    private String slotId(C4EpgEntry epgEntry) {
        return epgEntry.id().substring(epgEntry.id().indexOf("/"));
    }
    
}
