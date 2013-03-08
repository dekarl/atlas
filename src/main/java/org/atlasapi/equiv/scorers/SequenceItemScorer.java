package org.atlasapi.equiv.scorers;

import java.util.Set;

import org.atlasapi.equiv.results.description.ResultDescription;
import org.atlasapi.equiv.results.scores.DefaultScoredCandidates;
import org.atlasapi.equiv.results.scores.Score;
import org.atlasapi.equiv.results.scores.ScoredCandidates;
import org.atlasapi.equiv.results.scores.DefaultScoredCandidates.Builder;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;

public class SequenceItemScorer implements EquivalenceScorer<Item> {

    @Override
    public ScoredCandidates<Item> score(Item subject, Set<? extends Item> candidates, ResultDescription desc) {
        Builder<Item> equivalents = DefaultScoredCandidates.fromSource("Sequence");
        
        if (subject instanceof Episode) {
            Episode episode = (Episode) subject;
            desc.appendText("Subject: S: %s, E: %s. %s candidates",
                episode.getSeriesNumber(),
                episode.getEpisodeNumber(),
                Iterables.size(candidates)
            );
            for (Item candidate : candidates) {
                Score score = score(episode, candidate, desc);
                equivalents.addEquivalent(candidate, score);
            }
        } else {
            desc.appendText("Subject: not epsiode");
            for (Item suggestion : candidates) {
                equivalents.addEquivalent(suggestion, Score.NULL_SCORE);
            }
        }

        return equivalents.build();
    }

    private Score score(Episode subject, Item candidate, ResultDescription desc) {

        if (!(candidate instanceof Episode)) {
            desc.appendText("%s not episode", candidate);
            return Score.NULL_SCORE;
        }
        
        Episode candidateEpisode = (Episode) candidate;
        
        Score score;
        if (nullableSeriesNumbersEqual(subject, candidateEpisode)
            && nonNullEpisodeNumbersEqual(subject, candidateEpisode)) {
            score = Score.ONE;
        } else {
            score = Score.NULL_SCORE;
        }
        
        describeScore(desc, candidateEpisode, score);
        return score;
    }

    private void describeScore(ResultDescription desc, Episode candidate, Score score) {
        desc.appendText("%s: S: %s, E: %s scored %s",
            candidate,
            candidate.getSeriesNumber(),
            candidate.getEpisodeNumber(),
            score
        );
    }

    private boolean nonNullEpisodeNumbersEqual(Episode episode, Episode candidate) {
        return episode.getEpisodeNumber() != null
            && episode.getEpisodeNumber().equals(candidate.getEpisodeNumber());
    }
    
    private boolean nullableSeriesNumbersEqual(Episode episode, Episode candidate) {
        return Objects.equal(episode.getSeriesNumber(), candidate.getSeriesNumber());
    }

    @Override
    public String toString() {
        return "Sequence Item Scorer";
    }
}
