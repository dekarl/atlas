package org.atlasapi.remotesite.channel4;

import static org.atlasapi.media.entity.Identified.TO_URI;

import java.util.List;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Version;

import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class C4BrandEpisodeLinker {
    public List<SeriesAndEpisodes> populateBroadcasts(List<SeriesAndEpisodes> episodeGuideContent,
            List<Episode> epgContent, Brand brand) {

        Multimap<String, Episode> indexedEpg = Multimaps.index(epgContent, TO_URI);

        for (SeriesAndEpisodes seriesAndEpisodes : episodeGuideContent) {
            for (Episode episode : seriesAndEpisodes.getEpisodes()) {
                for (String alias : episode.getAliases()) {
                    for (Episode broadcastEpisode : indexedEpg.get(alias)) {
                        Version version = episodeVersion(episode);
                        Broadcast broadcast = getOnlyBroadcast(broadcastEpisode);
                        addOrUpdateBroadcastInVersion(version, broadcast);
                    }
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

}
