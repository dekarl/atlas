package org.atlasapi.remotesite.channel4.pmlsd;

import static org.atlasapi.media.entity.Identified.TO_URI;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Clip;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

class C4ContentLinker {
    
    private static final Series PLACEHOLDER = new Series();
    private static final CharSequence PMLSC_URI_PREFIX = "http://pmlsc.channel4.com/pmlsd/";
    private static final CharSequence TAG_URI_PREFIX = "tag:pmlsc.channel4.com,2009:/programmes/";

    public SetMultimap<Series, Episode> link4odToEpg(SetMultimap<Series, Episode> epiosodeGuide, List<Episode> fourOd) {

        Map<String, Episode> odIndex = Maps.newHashMap(Maps.uniqueIndex(fourOd, Identified.TO_URI));

        for (Episode episode : epiosodeGuide.values()) {
            Episode odEpisode = odIndex.remove(episode.getCanonicalUri());
            if (odEpisode != null) {
                episode.setVersions(odEpisode.getVersions());
                episode.addAliasUrls(odEpisode.getAliasUrls());
            }
        }
        
        for (Episode episode : odIndex.values()) {
            epiosodeGuide.put(PLACEHOLDER, episode);
        }

        return epiosodeGuide;
    }

    public SetMultimap<Series, Episode> populateBroadcasts(SetMultimap<Series, Episode> episodeGuideContent, List<Episode> epgContent) {

        Multimap<String, Episode> indexedEpg = Multimaps.index(epgContent, TO_TAG_URI);

        for (Episode episode : episodeGuideContent.values()) {
            for (String alias : episode.getAliasUrls()) {
                for (Episode broadcastEpisode : indexedEpg.get(alias)) {
                    Version version = episodeVersion(episode);
                    Broadcast broadcast = getOnlyBroadcast(broadcastEpisode);
                    addOrUpdateBroadcastInVersion(version, broadcast);
                }
            }
        }

        return episodeGuideContent;
    }

    private void addOrUpdateBroadcastInVersion(Version version, Broadcast broadcast) {
        boolean broadcastExists = false;
        for (Broadcast existingBroadcast : version.getBroadcasts()) {
            if (existingBroadcast.equals(broadcast)) {
                existingBroadcast.setAliases(broadcast.getAliases());
                existingBroadcast.setLastUpdated(broadcast.getLastUpdated());
                broadcastExists = true;
            }
        }
        if (!broadcastExists) {
            version.addBroadcast(broadcast);
        }
    }

    private Version episodeVersion(Episode episode) {
        Version version;
        if (episode.getVersions().isEmpty()) {
            version = new Version();
            episode.addVersion(version);
        } else {
            version = Iterables.getOnlyElement(episode.getVersions());
        }
        return version;
    }

    private Broadcast getOnlyBroadcast(Episode broadcastEpisode) {
        Version version = Iterables.getOnlyElement(broadcastEpisode.getVersions(), null);
        if (version == null) {
            return null;
        }
        return Iterables.getOnlyElement(version.getBroadcasts(), null);
    }

    public SetMultimap<Series, Episode> linkClipsToContent(SetMultimap<Series, Episode> content, List<Clip> clips, Brand brand) {

        Map<String, Episode> lookup = toEpisodeLookup(content.values());

        for (Clip clip : clips) {
            Episode episode = findEpisode(lookup, clip);
            if (episode != null) {
                // This is a temporary container of the series/episode number until we have
                // episode IDs in the clip API.
                // 
                // The clipOf field should be unset, since it's not actually a reference to
                // another item
                clip.setClipOf(null);
                episode.addClip(clip);
            } else {
                brand.addClip(clip);
            }
        }

        return content;
    }

    public static final Pattern SERIES_AND_EPISODE_NUMBER_IN_ANY_URI = Pattern.compile("series-(\\d+)/episode-(\\d+)");
    
    private Episode findEpisode(Map<String, Episode> lookup, Clip clip) {
        return lookup.get(clip.getClipOf());
    }

    private Map<String, Episode> toEpisodeLookup(Iterable<Episode> contents) {
        Map<String, Episode> lookup = Maps.newHashMap();
        for (Episode episode : contents) {
            if (episode.getSeriesNumber() != null && episode.getEpisodeNumber() != null) {
                lookup.put(concatSeriesAndEpNum(episode), episode);
            }
        }
        return lookup;
    }

    private String concatSeriesAndEpNum(Episode episode) {
        return concatSeriesAndEpNum(episode.getSeriesNumber(), episode.getEpisodeNumber());
    }

    private String concatSeriesAndEpNum(int seriesNumber, int episodeNumber) {
        return seriesNumber + "-" + episodeNumber;

    }
    
    private static Function<Episode, String> TO_TAG_URI = new Function<Episode, String>() {

        @Override
        public String apply(Episode episode) {
            return episode.getCanonicalUri().replace(PMLSC_URI_PREFIX, TAG_URI_PREFIX);
        }
        
    };
}
