package org.atlasapi.remotesite.channel4;

import java.util.List;
import java.util.Map;

import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Identified;

import com.google.common.collect.Maps;

class C4OnDemandEpisodesLinker {
    
    public List<SeriesAndEpisodes> link4odToEpg(List<SeriesAndEpisodes> epiosodeGuide, List<Episode> fourOd) {

        Map<String, Episode> odIndex = Maps.uniqueIndex(fourOd, Identified.TO_URI);

        for (SeriesAndEpisodes seriesAndEpisodes : epiosodeGuide) {
            for (Episode episode : seriesAndEpisodes.getEpisodes()) {
                Episode odEpisode = odIndex.get(episode.getCanonicalUri());
                if (odEpisode != null) {
                    episode.setVersions(odEpisode.getVersions());
                }
            }
        }
        
        return epiosodeGuide;
    }

}
