package org.atlasapi.remotesite.channel4;

import java.util.Map;

import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.query.content.PerPublisherCurieExpander;
import org.jdom.Attribute;
import org.jdom.Element;

import com.google.common.base.Strings;
import com.metabroadcast.common.time.Clock;
import com.sun.syndication.feed.atom.Content;
import com.sun.syndication.feed.atom.Entry;

public abstract class BaseC4EpisodeExtractor {

    protected static final String DC_EPISODE_NUMBER = "dc:relation.EpisodeNumber";
    protected static final String DC_SERIES_NUMBER = "dc:relation.SeriesNumber";
    protected static final String DC_PROGRAMME_ID = "dc:relation.programmeId";
    protected final Clock clock;
    
    public BaseC4EpisodeExtractor(Clock clock) {
        this.clock = clock;
    }

    protected Episode createBasicEpisode(Entry source, Map<String, String> lookup) {
        String episodeUri = extractEpisodeUri(source, lookup);
        
        if (episodeUri == null) {
            return null;
        }
        
        Episode episode = new Episode(episodeUri, PerPublisherCurieExpander.CurieAlgorithm.C4.compact(episodeUri), Publisher.C4);
        
        episode.setLastUpdated(clock.now());
        
        episode.setEpisodeNumber(C4AtomApi.readAsNumber(lookup, DC_EPISODE_NUMBER));
        episode.setSeriesNumber(C4AtomApi.readAsNumber(lookup, DC_SERIES_NUMBER));
        
        episode.setTitle(source.getTitle());
        Content summary = source.getSummary();
        if (summary != null) {
            episode.setDescription(Strings.emptyToNull(summary.getValue()));
        }
        addImages(source, episode);
        
        episode.setIsLongForm(true);
        return episode;
    }

    protected String extractEpisodeUri(Entry source, Map<String, String> lookup) {
        String progId = lookup.get(DC_PROGRAMME_ID);
        if (progId == null) {
            return null;
        }
        return C4AtomApi.PROGRAMMES_BASE + progId;
    }

    protected void addImages(Entry source, Item item) {
        Element mediaGroup = getMedia(source);
        
        if (mediaGroup != null) {
            Element thumbnail = mediaGroup.getChild("thumbnail", C4AtomApi.NS_MEDIA_RSS);
            if (thumbnail != null) {
                Attribute thumbnailUri = thumbnail.getAttribute("url");
                C4AtomApi.addImages(item, thumbnailUri.getValue());
            }
        }
    }

    protected Element getMedia(Entry source) {
        // TODO: This needs writing?
        return null;
    }

}
