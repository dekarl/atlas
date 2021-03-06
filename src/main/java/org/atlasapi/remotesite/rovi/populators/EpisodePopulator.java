package org.atlasapi.remotesite.rovi.populators;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.remotesite.rovi.RoviCanonicalUriGenerator.canonicalUriForProgram;
import static org.atlasapi.remotesite.rovi.RoviCanonicalUriGenerator.canonicalUriForSeason;

import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.remotesite.rovi.model.RoviEpisodeSequenceLine;
import org.atlasapi.remotesite.rovi.model.RoviProgramDescriptionLine;
import org.atlasapi.remotesite.rovi.model.RoviProgramLine;

import com.google.common.base.Optional;
import com.google.common.cache.LoadingCache;


public class EpisodePopulator extends ItemPopulator<Episode> {
    
    private final Optional<RoviEpisodeSequenceLine> episodeSequence;
    private final LoadingCache<String, Optional<Integer>> seasonNumberCache;

    public EpisodePopulator(Optional<RoviProgramLine> program,
            Iterable<RoviProgramDescriptionLine> descriptions, ContentResolver contentResolver,
            Optional<RoviEpisodeSequenceLine> episodeSequence,
            LoadingCache<String, Optional<Integer>> seasonNumberCache) {
        super(program, descriptions, contentResolver);
        this.episodeSequence = checkNotNull(episodeSequence);
        this.seasonNumberCache = checkNotNull(seasonNumberCache);
    }

    @Override
    protected void addItemSpecificData(Episode episode) {
        if (optionalProgram.isPresent()) {
            populateFromProgram(episode, optionalProgram.get());
        }
        
        if (episodeSequence.isPresent()) {
            populateFromEpisodeSequence(episode, episodeSequence.get());
        }
    }

    private void populateFromProgram(Episode episode, RoviProgramLine program) {
        if (program.getEpisodeTitle().isPresent()) {
            episode.setTitle(program.getEpisodeTitle().get());
        }
        
        setSeasonNumber(episode, program);
        setBrandRef(episode, program);
        setSeriesRef(episode, program);
    }

    private void populateFromEpisodeSequence(Episode episode, RoviEpisodeSequenceLine episodeSequence) {
        if (episodeSequence.getEpisodeSeasonSequence().isPresent()) {
            episode.setEpisodeNumber(episodeSequence.getEpisodeSeasonSequence().get());
        } 
        
        if (episodeSequence.getEpisodeTitle().isPresent()) {
            episode.setTitle(episodeSequence.getEpisodeTitle().get());
        }        
    }
    
    private void setSeasonNumber(Episode episode, RoviProgramLine program)  {
        if (!program.getSeasonId().isPresent()) {
            episode.setSeriesNumber(null);
            return;
        }
        
        Optional<Integer> seasonNumber = seasonNumberCache.getUnchecked(program.getSeasonId().get());
        if (seasonNumber.isPresent()) {
            episode.setSeriesNumber(seasonNumber.get());
        }
    }
    
    
    private void setBrandRef(Episode episode, RoviProgramLine program) {
        if (!program.getSeriesId().isPresent()) {
            episode.setParentRef(null);
            return;
        }

        String seriesCanonicalUri = canonicalUriForProgram(program.getSeriesId().get());
        episode.setParentRef(new ParentRef(seriesCanonicalUri));
    }
    
    private void setSeriesRef(Episode episode, RoviProgramLine program) {
        if (!program.getSeasonId().isPresent()) {
            episode.setSeriesRef(null);
            return;
        }
        
        String seasonCanonicalUri = canonicalUriForSeason(program.getSeasonId().get());
        episode.setSeriesRef(new ParentRef(seasonCanonicalUri));
    }
}
