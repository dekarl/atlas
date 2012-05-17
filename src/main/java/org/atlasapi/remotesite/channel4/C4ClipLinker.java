package org.atlasapi.remotesite.channel4;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Clip;
import org.atlasapi.media.entity.Episode;

import com.google.common.collect.Maps;

public class C4ClipLinker {
    public List<SeriesAndEpisodes> linkClipsToContent(Brand brand, List<SeriesAndEpisodes> content,
        List<Clip> clips) {

    Map<String, Episode> lookup = Maps.newHashMap();
    for (SeriesAndEpisodes seriesAndEpisodes : content) {
        lookup.putAll(toEpisodeLookup(seriesAndEpisodes.getEpisodes()));
    };
    
    for (Clip clip : clips) {
        Episode episode = findEpisode(lookup, clip);
        
        if (episode != null) {
            episode.addClip(clip);
        } else {
            brand.addClip(clip);
        }
    }
    
    return content;
}

private Episode findEpisode(Map<String, Episode> lookup, Clip clip) {
    Matcher matcher = C4AtomApi.SERIES_AND_EPISODE_NUMBER_IN_ANY_URI.matcher(clip.getCanonicalUri());
    if (matcher.find()) {
        Integer series = Integer.valueOf(matcher.group(1));
        Integer episodeNumber = Integer.valueOf(matcher.group(2));  
        return lookup.get(concatSeriesAndEpNum(series, episodeNumber));
    }
    return null;
}

private static Map<String, Episode> toEpisodeLookup(Iterable<Episode> contents) {
    Map<String, Episode> lookup = Maps.newHashMap();
    for (Episode episode : contents) {
        if (episode.getSeriesNumber() != null && episode.getEpisodeNumber() != null) {
            lookup.put(concatSeriesAndEpNum(episode), episode);
        }
    }
    return lookup;
}

private static String concatSeriesAndEpNum(Episode episode) {
    return concatSeriesAndEpNum(episode.getSeriesNumber() , episode.getEpisodeNumber());
}

private static String concatSeriesAndEpNum(int seriesNumber, int episodeNumber) {
    return seriesNumber + "-" + episodeNumber;

}
}
